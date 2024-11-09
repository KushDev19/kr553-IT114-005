package Project;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import Project.TextFX.Color;

/**
 * Demoing bi-directional communication between client and server in a
 * multi-client scenario
 */
public enum Client {
    INSTANCE;

    private Socket server = null;
    private ObjectOutputStream out = null;
    private ObjectInputStream in = null;
    final Pattern ipAddressPattern = Pattern
            .compile("/connect\\s+(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}:\\d{3,5})");
    final Pattern localhostPattern = Pattern.compile("/connect\\s+(localhost:\\d{3,5})");
    private volatile boolean isRunning = true; // volatile for thread-safe visibility
    private ConcurrentHashMap<Long, ClientData> knownClients = new ConcurrentHashMap<>();
    private ClientData myData;

    // constants (used to reduce potential types when using them in code)
    private final String COMMAND_CHARACTER = "/";
    private final String CREATE_ROOM = "createroom";
    private final String JOIN_ROOM = "joinroom";
    private final String DISCONNECT = "disconnect";
    private final String LOGOFF = "logoff";
    private final String LOGOUT = "logout";
    private final String SINGLE_SPACE = " ";
    private final String ROLL = "roll";
    private final String FLIP = "flip";


    // needs to be private now that the enum logic is handling this
    private Client() {
        System.out.println("Client Created");
        myData = new ClientData();
    }

    public boolean isConnected() {
        if (server == null) {
            return false;
        }
        // https://stackoverflow.com/a/10241044
        // Note: these check the client's end of the socket connect; therefore they
        // don't really help determine if the server had a problem
        // and is just for lesson's sake
        return server.isConnected() && !server.isClosed() && !server.isInputShutdown() && !server.isOutputShutdown();
    }

    /**
     * Takes an IP address and a port to attempt a socket connection to a server.
     * 
     * @param address
     * @param port
     * @return true if connection was successful
     */

      //kr553 10/20/2024
    private boolean connect(String address, int port) {
        try {
            server = new Socket(address, port);
            // channel to send to server
            out = new ObjectOutputStream(server.getOutputStream());
            // channel to listen to server
            in = new ObjectInputStream(server.getInputStream());
            System.out.println("Client connected");
            // Use CompletableFuture to run listenToServer() in a separate thread
            CompletableFuture.runAsync(this::listenToServer);
        } catch (UnknownHostException e) {
            System.err.println("Unknown host: " + address);
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("I/O error when connecting to " + address + ":" + port);
            e.printStackTrace();
        }
        return isConnected();
    }

    /**
     * <p>
     * Check if the string contains the <i>connect</i> command
     * followed by an IP address and port or localhost and port.
     * </p>
     * <p>
     * Example format: 123.123.123.123:3000
     * </p>
     * <p>
     * Example format: localhost:3000
     * </p>
     * https://www.w3schools.com/java/java_regex.asp
     * 
     * @param text
     * @return true if the text is a valid connection command
     */

     //kr553 10/20/2024
    private boolean isConnection(String text) {
        Matcher ipMatcher = ipAddressPattern.matcher(text);
        Matcher localhostMatcher = localhostPattern.matcher(text);
        return ipMatcher.matches() || localhostMatcher.matches();
    }

    /**
     * Controller for handling various text commands.
     * <p>
     * Add more here as needed
     * </p>
     * 
     * @param text
     * @return true if the text was a command or triggered a command
     */
    private boolean processClientCommand(String text) {
        if (isConnection(text)) {
            // Handle /connect command
            return handleConnectCommand(text);
        } else if ("/quit".equalsIgnoreCase(text)) {
            // Existing quit handling code...
            sendDisconnect();
            return true;
        } else if (text.startsWith("/name")) {
            // Existing name handling code...
            return handleNameCommand(text);
        } else if (text.equalsIgnoreCase("/users")) {
            // Existing users handling code...
            // Implement if needed
            System.out.println("User list feature not implemented yet.");
            return true;
        } else {
            if (text.startsWith(COMMAND_CHARACTER)) {
                boolean wasCommand = false;
                String fullCommand = text.substring(1); // Remove leading '/'
                String[] commandParts = fullCommand.split(SINGLE_SPACE, 2); // Limit to 2 parts
                final String command = commandParts[0];
                final String commandValue = commandParts.length >= 2 ? commandParts[1] : "";
                switch (command.toLowerCase()) {
                    case CREATE_ROOM:
                        sendCreateRoom(commandValue);
                        wasCommand = true;
                        break;
                    case JOIN_ROOM:
                        sendJoinRoom(commandValue);
                        wasCommand = true;
                        break;
                    case DISCONNECT:
                    case LOGOFF:
                    case LOGOUT:
                        sendDisconnect();
                        wasCommand = true;
                        break;
                    case ROLL:
                        processRollCommand(commandValue);
                        wasCommand = true;
                        break;
                    case FLIP:
                        processFlipCommand();
                        wasCommand = true;
                        break;
                    default:
                        System.out.println(TextFX.colorize("Unknown command: " + command, Color.RED));
                        wasCommand = true;
                        break;
                }
                return wasCommand;
            }
        }
        return false;
    }

    /**
     * Handles the /connect command to establish a connection to the server.
     * 
     * @param text The entire command text (e.g., "/connect localhost:3000")
     * @return true if the command was processed
     */
    private boolean handleConnectCommand(String text) {
        // Extract the address and port
        String[] parts = text.split("\\s+");
        if (parts.length != 2) {
            System.out.println(TextFX.colorize("Invalid /connect command format. Use: /connect host:port", Color.RED));
            return true;
        }
        String hostPort = parts[1];
        String[] hostPortParts = hostPort.split(":");
        if (hostPortParts.length != 2) {
            System.out.println(TextFX.colorize("Invalid /connect command format. Use: /connect host:port", Color.RED));
            return true;
        }
        String host = hostPortParts[0];
        int port;
        try {
            port = Integer.parseInt(hostPortParts[1]);
        } catch (NumberFormatException e) {
            System.out.println(TextFX.colorize("Invalid port number.", Color.RED));
            return true;
        }

        boolean connected = connect(host, port);
        if (connected) {
            System.out.println(TextFX.colorize("Successfully connected to " + host + ":" + port, Color.GREEN));
        } else {
            System.out.println(TextFX.colorize("Failed to connect to " + host + ":" + port, Color.RED));
        }
        return true;
    }

    /**
     * Handles the /name command to set the client's name.
     * 
     * @param text The entire command text (e.g., "/name Alice")
     * @return true if the command was processed
     */
    private boolean handleNameCommand(String text) {
        String[] parts = text.split("\\s+", 2);
        if (parts.length != 2) {
            System.out.println(TextFX.colorize("Invalid /name command format. Use: /name yourName", Color.RED));
            return true;
        }
        String name = parts[1].trim();
        if (name.isEmpty()) {
            System.out.println(TextFX.colorize("Name cannot be empty.", Color.RED));
            return true;
        }
        myData.setClientName(name);
        sendClientName();
        System.out.println(TextFX.colorize("Name set to: " + name, Color.GREEN));
        return true;
    }

    private void processRollCommand(String commandValue) {
        commandValue = commandValue.trim();
        RollPayload rollPayload = new RollPayload();
        rollPayload.setSenderName(myData.getClientName());
        rollPayload.setClientId(myData.getClientId());

        if (commandValue.matches("\\d+")) { // Format 1: /roll #
            int rollRange = Integer.parseInt(commandValue);
            rollPayload.setRollRange(rollRange);
        } else if (commandValue.matches("\\d+d\\d+")) { // Format 2: /roll #d#
            String[] parts = commandValue.split("d");
            int numberOfDice = Integer.parseInt(parts[0]);
            int sidesPerDie = Integer.parseInt(parts[1]);
            rollPayload.setNumberOfDice(numberOfDice);
            rollPayload.setSidesPerDie(sidesPerDie);
        } else {
            System.out.println(TextFX.colorize("Invalid /roll command format.", Color.RED));
            return;
        }

        send(rollPayload);
    }

    private void processFlipCommand() {
        Payload flipPayload = new Payload();
        flipPayload.setPayloadType(PayloadType.FLIP);
        flipPayload.setSenderName(myData.getClientName());
        flipPayload.setClientId(myData.getClientId());
        send(flipPayload);
    }

    // send methods to pass data to the ServerThread

    /**
     * Sends the room name we intend to create
     * 
     * @param room
     */
    private void sendCreateRoom(String room) {
        Payload p = new Payload();
        p.setPayloadType(PayloadType.ROOM_CREATE);
        p.setMessage(room);
        send(p);
    }

    /**
     * Sends the room name we intend to join
     * 
     * @param room
     */
    private void sendJoinRoom(String room) {
        Payload p = new Payload();
        p.setPayloadType(PayloadType.ROOM_JOIN);
        p.setMessage(room);
        send(p);
    }

    /**
     * Tells the server-side we want to disconnect
     */
    private void sendDisconnect() {
        Payload p = new Payload();
        p.setPayloadType(PayloadType.DISCONNECT);
        send(p);
    }

    /**
     * Sends desired message over the socket
     * 
     * @param message
     */
    private void sendMessage(String message) {
        Payload p = new Payload();
        p.setPayloadType(PayloadType.MESSAGE);
        p.setMessage(message);
        send(p);
    }

    /**
     * Sends chosen client name after socket handshake
     */
    private void sendClientName() {
        if (myData.getClientName() == null || myData.getClientName().length() == 0) {
            System.out.println(TextFX.colorize("Name must be set first via /name command", Color.RED));
            return;
        }
        ConnectionPayload cp = new ConnectionPayload();
        cp.setClientName(myData.getClientName());
        send(cp);
    }

    /**
     * Generic send that passes any Payload over the socket (to ServerThread)
     * 
     * @param p
     */
    private void send(Payload p) {
        try { 
            out.writeObject(p);
            out.flush();
        } catch (IOException e) {
            System.out.println(TextFX.colorize("Failed to send message to server.", Color.RED));
            e.printStackTrace();
        }
    }
    
    // end send methods

    public void start() throws IOException {
        System.out.println("Client starting");

        // Use CompletableFuture to run listenToInput() in a separate thread
        CompletableFuture<Void> inputFuture = CompletableFuture.runAsync(this::listenToInput);

        // Wait for inputFuture to complete to ensure proper termination
        inputFuture.join();
    }

    /**
     * Listens for messages from the server
     */

     //kr553 10/20/2024
    private void listenToServer() {
        try {
            while (isRunning && isConnected()) {
                Payload fromServer = (Payload) in.readObject(); // blocking read
                if (fromServer != null) {
                    // System.out.println(fromServer);
                    processPayload(fromServer);
                } else {
                    System.out.println(TextFX.colorize("Server disconnected.", Color.RED));
                    break;
                }
            }
        } catch (ClassCastException | ClassNotFoundException cce) {
            System.err.println("Error reading object as specified type: " + cce.getMessage());
            cce.printStackTrace();
        } catch (IOException e) {
            if (isRunning) {
                System.out.println(TextFX.colorize("Connection dropped.", Color.RED));
                e.printStackTrace();
            }
        } finally {
            closeServerConnection();
        }
        System.out.println("listenToServer thread stopped");
    }

    /**
     * Listens for keyboard input from the user
     */

     //kr553 10/20/2024
    private void listenToInput() {
        try (Scanner si = new Scanner(System.in)) {
            System.out.println("Waiting for input"); // moved here to avoid console spam
            while (isRunning) { // Run until isRunning is false
                String line = si.nextLine();
                if (!processClientCommand(line)) {
                    if (isConnected()) {
                        sendMessage(line);
                    } else {
                        System.out.println(
                                TextFX.colorize(
                                        "Not connected to server (hint: type `/connect host:port` without the quotes and replace host/port with the necessary info)",
                                        Color.YELLOW));
                    }
                }
            }
        } catch (Exception e) {
            System.out.println(TextFX.colorize("Error in listenToInput()", Color.RED));
            e.printStackTrace();
        }
        System.out.println("listenToInput thread stopped");
    }

    /**
     * Closes the client connection and associated resources
     */
    private void close() {
        isRunning = false;
        closeServerConnection();
        System.out.println(TextFX.colorize("Client terminated.", Color.YELLOW));
        // System.exit(0); // Terminate the application
    }

    /**
     * Closes the server connection and associated resources
     */
    private void closeServerConnection() {
        myData.reset();
        knownClients.clear();
        try {
            if (out != null) {
                System.out.println("Closing output stream");
                out.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            if (in != null) {
                System.out.println("Closing input stream");
                in.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            if (server != null) {
                System.out.println("Closing connection");
                server.close();
                System.out.println("Closed socket");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Client client = Client.INSTANCE;
        try {
            client.start();
        } catch (IOException e) {
            System.out.println(TextFX.colorize("Exception from main()", Color.RED));
            e.printStackTrace();
        }
    }

    /**
     * Handles received message from the ServerThread
     * 
     * @param payload
     */

     //kr553 10/20/2024
    private void processPayload(Payload payload) {
        try {
            switch (payload.getPayloadType()) {
                case PayloadType.CLIENT_ID: // get id assigned
                    ConnectionPayload cp = (ConnectionPayload) payload;
                    processClientData(cp.getClientId(), cp.getClientName());
                    break;
                case PayloadType.SYNC_CLIENT: // silent add
                    cp = (ConnectionPayload) payload;
                    processClientSync(cp.getClientId(), cp.getClientName());
                    break;
                case PayloadType.DISCONNECT: // remove a disconnected client (mostly for the specific message vs leaving
                                             // a room)
                    cp = (ConnectionPayload) payload;
                    processDisconnect(cp.getClientId(), cp.getClientName());
                    // note: we want this to cascade
                case PayloadType.ROOM_JOIN: // add/remove client info from known clients
                    cp = (ConnectionPayload) payload;
                    processRoomAction(cp.getClientId(), cp.getClientName(), cp.getMessage(), cp.isConnect());
                    break;
                case PayloadType.MESSAGE: // displays a received message
                    processMessage(payload.getClientId(), payload.getMessage());
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            System.out.println(TextFX.colorize("Could not process Payload: " + payload, Color.RED));
            e.printStackTrace();
        }
    }

    // payload processors

    private void processDisconnect(long clientId, String clientName) {
        System.out.println(
                TextFX.colorize(String.format("*%s disconnected*",
                        clientId == myData.getClientId() ? "You" : clientName),
                        Color.RED));
        if (clientId == myData.getClientId()) {
            closeServerConnection();
        }
    }

    private void processClientData(long clientId, String clientName) {
        if (myData.getClientId() == ClientData.DEFAULT_CLIENT_ID) {
            myData.setClientId(clientId);
            myData.setClientName(clientName);
            // knownClients.put(cp.getClientId(), myData);// <-- this is handled later
        }
    }

    //kr553 10/20/2024
    private void processMessage(long clientId, String message) {
        String name = knownClients.containsKey(clientId) ? knownClients.get(clientId).getClientName() : "Room";
        String formattedMessage = TextFX.formatText(message);
        System.out.println(TextFX.colorize(String.format("%s: %s", name, formattedMessage), Color.BLUE));
    }
    

    private void processClientSync(long clientId, String clientName) {
        if (!knownClients.containsKey(clientId)) {
            ClientData cd = new ClientData();
            cd.setClientId(clientId);
            cd.setClientName(clientName);
            knownClients.put(clientId, cd);
        }
    }

    private void processRoomAction(long clientId, String clientName, String message, boolean isJoin) {
        if (isJoin && !knownClients.containsKey(clientId)) {
            ClientData cd = new ClientData();
            cd.setClientId(clientId);
            cd.setClientName(clientName);
            knownClients.put(clientId, cd);
            System.out.println(TextFX
                    .colorize(String.format("*%s[%s] joined the Room %s*", clientName, clientId, message),
                            Color.GREEN));
        } else if (!isJoin) {
            ClientData removed = knownClients.remove(clientId);
            if (removed != null) {
                System.out.println(
                        TextFX.colorize(String.format("*%s[%s] left the Room %s*", clientName, clientId, message),
                                Color.YELLOW));
            }
            //clear our list
            if(clientId == myData.getClientId()){
                knownClients.clear();
            }
        }
    }
    // end payload processors

}
