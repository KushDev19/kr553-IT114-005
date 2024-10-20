package Module4.Part3HW;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * A server-side representation of a single client
 */
public class ServerThread extends Thread {
    private Socket client; // communication directly to "my" client
    private boolean isRunning = false; // control variable to stop this thread
    private ObjectOutputStream out; // exposed here for send()
    private Server server; // ref to our server so we can call methods on it
    private long clientId;
    private Consumer<ServerThread> onInitializationComplete; // callback to inform when this object is ready
    private String clientName;
    private static int clientCount = 0;

    /**
     * A wrapper method so we don't need to keep typing out the long/complex sysout line inside
     * 
     * @param message
     */
    private void info(String message) {
        System.out.println(String.format("Thread[%s]: %s", getClientId(), message));
    }

    /**
     * Wraps the Socket connection and takes a Server reference and a callback
     * 
     * @param myClient
     * @param server
     * @param onInitializationComplete method to inform listener that this object is ready
     */
    protected ServerThread(Socket myClient, Server server, Consumer<ServerThread> onInitializationComplete) {
        Objects.requireNonNull(myClient, "Client socket cannot be null");
        Objects.requireNonNull(server, "Server cannot be null");
        Objects.requireNonNull(onInitializationComplete, "callback cannot be null");
        info("ServerThread created");
        // get communication channels to single client
        this.client = myClient;
        this.server = server;
        this.clientId = this.threadId();
        this.onInitializationComplete = onInitializationComplete;
        this.clientName = "Client" + (++clientCount);
        info("Assigned client name: " + clientName);
    }

    public String getClientName() {
        return clientName;
    }

    public long getClientId() {
        return this.clientId;
    }

    /**
     * One of the two ways to get this to exit the listen loop
     */
    protected void disconnect() {
        info("Thread being disconnected by server");
        isRunning = false;
        this.interrupt(); // breaks out of blocking read in the run() method
        cleanup(); // good practice to ensure data is written out immediately
    }

    /**
     * Sends the message over the socket
     * 
     * @param message
     * @return true if no errors were encountered
     */
    protected boolean send(String message) {
        try {
            out.writeObject(message);
            out.flush();
            return true;
        } catch (IOException e) {
            info("Error sending message to client (most likely disconnected)");
            // e.printStackTrace();
            cleanup();
            return false;
        }
    }

    @Override
public void run() {
    try (ObjectOutputStream out = new ObjectOutputStream(client.getOutputStream());
         ObjectInputStream in = new ObjectInputStream(client.getInputStream());) {
        this.out = out;
        isRunning = true;
        onInitializationComplete.accept(this); // Notify server that initialization is complete
        String fromClient;

        // Keep reading messages from the client
        while (isRunning) {
            try {
                fromClient = (String) in.readObject(); // blocking method
                if (fromClient != null) {
                    info("Received from my client: " + fromClient);
                    server.relay(fromClient, this);  // Relay the message to other clients
                } else {
                    throw new IOException("Connection interrupted"); // Specific exception for a clean break
                }
            } catch (ClassCastException | ClassNotFoundException cce) {
                System.err.println("Error reading object as specified type: " + cce.getMessage());
            } catch (IOException e) {
                if (Thread.currentThread().isInterrupted()) {
                    info("Thread interrupted during read (likely from the disconnect() method)");
                    break;
                }
                info("IO exception while reading from client");
                break;
            }
        }
    } catch (Exception e) {
        info("General Exception: " + e.getMessage());
    } finally {
        isRunning = false;
        info("Exited thread loop. Cleaning up connection");
        cleanup();
        server.removeClient(this);  // Clean up and remove client from the list
    }
}


    private boolean processCommand(String message) {
        if (message.startsWith("/start")) {
            server.startGame();
            return true;
        } else if (message.startsWith("/stop")) {
            server.stopGame();
            return true;
        } else if (message.startsWith("/guess")) {
            String[] tokens = message.split(" ");
            if (tokens.length == 2) {
                try {
                    int guess = Integer.parseInt(tokens[1]);
                    server.processGuess(guess, this);
                } catch (NumberFormatException e) {
                    send("Invalid number format.");
                }
            } else {
                send("Usage: /guess <number>");
            }
            return true;
        } else if (message.startsWith("/toss") || message.startsWith("/flip") || message.startsWith("/coin")) {
            String result = server.tossCoin();
            server.broadcast(clientName + " tossed a coin and got " + result + ".");
            return true;
        }
        return false;
    }

    private void cleanup() {
        info("ServerThread cleanup() start");
        try {
            client.close();
        } catch (IOException e) {
            info("Client already closed");
        }
        info("ServerThread cleanup() end");
    }
}
