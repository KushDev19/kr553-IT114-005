package Module4.Part3HW;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;
import java.util.Random;

public class Server {
    private int port;
    private boolean isRunning = false;
    private Vector<ServerThread> clients; // could use ArrayList instead
    private boolean gameActive = false;
    private int hiddenNumber;
    private Random random = new Random();

    public Server(int port) {
        this.port = port;
        clients = new Vector<ServerThread>();
    }

    public synchronized void startGame() {
        gameActive = true;
        hiddenNumber = random.nextInt(10) + 1; // Random number between 1 and 10
        broadcast("The number guesser game has started! Use /guess <number> to participate.");
    }

    public synchronized void stopGame() {
        gameActive = false;
        broadcast("The number guesser game has been stopped.");
    }

    public synchronized void processGuess(int guess, ServerThread sender) {
        if (!gameActive) {
            sender.send("The game is not active.");
            return;
        }
        if (guess == hiddenNumber) {
            broadcast(sender.getClientName() + " guessed the hidden number correctly!");
            gameActive = false;
        } else {
            broadcast(sender.getClientName() + " guessed " + guess + " but it was not correct.");
        }
    }

    public synchronized String tossCoin() {
        return random.nextBoolean() ? "heads" : "tails";
    }

    public synchronized void broadcast(String message) {
        synchronized (clients) {
            for (ServerThread client : clients) {
                client.send(message);
            }
        }
    }

    public synchronized void removeClient(ServerThread client) {
        clients.remove(client);
    }

    public synchronized void relay(String message, ServerThread sender) {
        // Send the message to all clients except the sender
        synchronized (clients) {
            for (ServerThread client : clients) {
                if (client != sender) {
                    client.send(message);
                }
            }
        }
    }

    public synchronized boolean isGameActive() {
        return gameActive;
    }

    public static void main(String[] args) {
        Server server = new Server(3000);
        server.startServer();
    }

    public synchronized void startServer() {
        isRunning = true;
        try (ServerSocket serverSocket = new ServerSocket(port);) {
            System.out.println("Server started on port " + port);

            while (isRunning) {
                System.out.println("Waiting for client connection...");
                Socket client = serverSocket.accept();
                System.out.println("Client connected");
                ServerThread thread = new ServerThread(client, this);
                thread.setClientName("User[" + thread.getId() + "]");
                synchronized (clients) {
                    clients.add(thread);
                }
                broadcast(thread.getClientName() + " has joined the server.");
                thread.start();
            }
        } catch (IOException e) {
            System.err.println("IOException in startServer: " + e.getMessage());
            e.printStackTrace();
        } finally {
            isRunning = false;
        }
    }
}