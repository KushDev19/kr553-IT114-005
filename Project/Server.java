package Project;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public enum Server {
    INSTANCE;

    private int port = 3000;
    private final ConcurrentHashMap<String, Room> rooms = new ConcurrentHashMap<>();
    private boolean isRunning = true;
    private long nextClientId = 1;
    private final Set<ServerThread> clients = ConcurrentHashMap.newKeySet(); // Thread-safe set for connected clients

    private Server() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("JVM is shutting down. Perform cleanup tasks.");
            shutdown();
        }));
    }

    private void start(int port) {
        this.port = port;
        System.out.println("Listening on port " + this.port);

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            createRoom(Room.LOBBY); // Create the default lobby room
            while (isRunning) {
                System.out.println("Waiting for next client");
                Socket incomingClient = serverSocket.accept();
                System.out.println("Client connected");

                // Create and initialize a new client thread
                ServerThread sClient = new ServerThread(incomingClient, this::onClientInitialized);
                sClient.start();
            }
        } catch (IOException e) {
            System.err.println("Error accepting connection");
            e.printStackTrace();
        } finally {
            shutdown();
            System.out.println("Closing server socket");
        }
    }

    /**
     * Gracefully shutdown all clients and rooms.
     */
    private void shutdown() {
        try {
            clients.forEach(ServerThread::disconnect); // Disconnect all clients
            clients.clear(); // Clear the clients set
            rooms.values().forEach(Room::close); // Close all rooms
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Callback invoked when a new client is fully initialized.
     * 
     * @param sClient the initialized client
     */
    private void onClientInitialized(ServerThread sClient) {
        sClient.sendClientId(nextClientId);
        nextClientId++;
        if (nextClientId < 0) {
            nextClientId = 1;
        }
        clients.add(sClient); // Add the client to the tracked set
        System.out.println(String.format("Server: *%s[%s] initialized*", sClient.getClientName(), sClient.getClientId()));
        joinRoom(Room.LOBBY, sClient); // Add the client to the lobby room
    }

    /**
     * Get all currently connected clients.
     * 
     * @return a set of all connected clients
     */
    public Set<ServerThread> getAllClients() {
        return new HashSet<>(clients); // Return a thread-safe copy
    }

    /**
     * Get a client by their unique ID.
     * 
     * @param clientId the client ID
     * @return the matching client, or null if not found
     */
    protected ServerThread getClientById(long clientId) {
        return clients.stream()
                .filter(client -> client.getClientId() == clientId)
                .findFirst()
                .orElse(null);
    }

    /**
     * Create a new room if it doesn't already exist.
     * 
     * @param name the name of the room
     * @return true if the room was created, false otherwise
     */
    protected boolean createRoom(String name) {
        final String nameCheck = name.toLowerCase();
        if (rooms.containsKey(nameCheck)) {
            return false; // Room already exists
        }
        Room room = new Room(name);
        rooms.put(nameCheck, room);
        System.out.println(String.format("Created new Room %s", name));
        return true;
    }

    /**
     * Attempt to move a client to a specific room.
     * 
     * @param name   the target room name
     * @param client the client to move
     * @return true if the move was successful, false otherwise
     */
    protected boolean joinRoom(String name, ServerThread client) {
        final String nameCheck = name.toLowerCase();
        if (!rooms.containsKey(nameCheck)) {
            return false; // Room does not exist
        }

        Room currentRoom = client.getCurrentRoom();
        if (currentRoom != null) {
            currentRoom.removedClient(client); // Remove the client from their current room
        }

        Room nextRoom = rooms.get(nameCheck);
        nextRoom.addClient(client); // Add the client to the target room
        return true;
    }

    /**
     * Remove a room from the server's list of rooms.
     * 
     * @param room the room to remove
     */
    protected void removeRoom(Room room) {
        rooms.remove(room.getName().toLowerCase());
        System.out.println(String.format("Server removed room %s", room.getName()));
    }

    /**
     * Broadcast a message to all connected clients.
     * 
     * @param message the message to broadcast
     */
    protected void broadcast(String message) {
        for (ServerThread client : clients) {
            client.sendMessage(message);
        }
    }

    public static void main(String[] args) {
        System.out.println("Server Starting");
        Server server = Server.INSTANCE;
        int port = 3000;
        try {
            port = Integer.parseInt(args[0]);
        } catch (Exception e) {
            // Default to the predefined port in case of an error
        }
        server.start(port);
        System.out.println("Server Stopped");
    }
}
