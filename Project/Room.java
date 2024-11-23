package Project;

import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class Room implements AutoCloseable {
    private String name; // Unique name of the Room
    private volatile boolean isRunning = false;
    private ConcurrentHashMap<Long, ServerThread> clientsInRoom = new ConcurrentHashMap<>();

    public static final String LOBBY = "lobby";

    private void info(String message) {
        System.out.println(String.format("Room[%s]: %s", name, message));
    }

    public Room(String name) {
        this.name = name;
        isRunning = true;
        System.out.println(String.format("Room[%s] created", this.name));
    }

    public String getName() {
        return this.name;
    }

    // kr553 11/9/2024
    public void processRollCommand(ServerThread client, RollPayload payload) {
        String resultMessage = "";
        Random rand = new Random();

        if (payload.getNumberOfDice() > 0 && payload.getSidesPerDie() > 0) {
            // Format: /roll #d#
            int total = 0;
            StringBuilder rolls = new StringBuilder();
            for (int i = 0; i < payload.getNumberOfDice(); i++) {
                int roll = rand.nextInt(payload.getSidesPerDie()) + 1; // Random number between 1 and sidesPerDie
                total += roll;
                rolls.append(roll);
                if (i < payload.getNumberOfDice() - 1) {
                    rolls.append(", ");
                }
            }
            resultMessage = String.format("%s rolled %dd%d and got %d (%s)",
                    client.getClientName(),
                    payload.getNumberOfDice(),
                    payload.getSidesPerDie(),
                    total,
                    rolls.toString());
        } else if (payload.getRollRange() > 0) {
            // Format: /roll #
            int roll = rand.nextInt(payload.getRollRange()) + 1; // Random number between 1 and rollRange
            resultMessage = String.format("%s rolled %d and got %d",
                    client.getClientName(),
                    payload.getRollRange(),
                    roll);
        } else {
            // Invalid roll parameters
            resultMessage = String.format("%s attempted an invalid roll command.", client.getClientName());
        }

        // Broadcast the result to all clients in the room
        sendMessage(client, resultMessage); // 'sender' is null because it's a server-generated message
    }

    public void processFlipCommand(ServerThread client) {
        Random rand = new Random();
        String result = rand.nextBoolean() ? "heads" : "tails";
        String resultMessage = String.format("%s flipped a coin and got %s", client.getClientName(), result);
    
        // Broadcast the result to all clients in the room
        sendMessage(client, resultMessage); // Pass 'client' instead of 'null'
    }

    protected synchronized void addClient(ServerThread client) {
        if (!isRunning) { // Block action if Room isn't running
            return;
        }
        if (clientsInRoom.containsKey(client.getClientId())) {
            info("Attempting to add a client that already exists in the room");
            return;
        }
        clientsInRoom.put(client.getClientId(), client);
        client.setCurrentRoom(this);

        // Notify clients of someone joining
        sendRoomStatus(client.getClientId(), client.getClientName(), true);
        // Sync room state to joiner
        syncRoomList(client);

        info(String.format("%s[%s] joined the Room[%s]", client.getClientName(), client.getClientId(), getName()));
    }

    // kr553 10/21/2024
    protected synchronized void removedClient(ServerThread client) {
        if (!isRunning) { // Block action if Room isn't running
            return;
        }
        // Notify remaining clients of someone leaving
        sendRoomStatus(client.getClientId(), client.getClientName(), false);
        clientsInRoom.remove(client.getClientId());

        info(String.format("%s[%s] left the room", client.getClientName(), client.getClientId(), getName()));

        autoCleanup();
    }

    /**
     * Takes a ServerThread and removes them from the Server.
     * Adding the synchronized keyword ensures that only one thread can execute
     * these methods at a time, preventing concurrent modification issues and ensuring thread safety.
     * 
     * @param client The client to disconnect.
     */
    // kr553 10/21/2024
    protected synchronized void disconnect(ServerThread client) {
        if (!isRunning) { // Block action if Room isn't running
            return;
        }
        long id = client.getClientId();
        sendDisconnect(client);
        client.disconnect();
        clientsInRoom.remove(client.getClientId());

        // Improved logging with user data
        info(String.format("%s[%s] disconnected", client.getClientName(), id));
    }

    protected synchronized void disconnectAll() {
        info("Disconnect All triggered");
        if (!isRunning) {
            return;
        }
        clientsInRoom.values().removeIf(client -> {
            disconnect(client);
            return true;
        });
        info("Disconnect All finished");
    }

    /**
     * Sends a private message between two users in the room.
     */
    protected synchronized void sendPrivateMessage(ServerThread sender, long targetClientId, String message) {
        if (!isRunning) {
            return;
        }

        ServerThread targetClient = clientsInRoom.get(targetClientId);

        if (targetClient != null) {
            // Apply text formatting
            String formattedMessage = TextFX.formatText(message);

            long senderId = sender.getClientId();

            // Send the message to sender and receiver
            boolean failedToSendSender = !sender.sendPrivateMessage(senderId, formattedMessage);
            boolean failedToSendReceiver = !targetClient.sendPrivateMessage(senderId, formattedMessage);

            if (failedToSendSender) {
                info(String.format("Removing disconnected client[%s] from list", sender.getClientId()));
                disconnect(sender);
            }
            if (failedToSendReceiver) {
                info(String.format("Removing disconnected client[%s] from list", targetClient.getClientId()));
                disconnect(targetClient);
            }

            // Log the private message (optional)
            info(String.format("Private message from %s to %s: %s", sender.getClientName(), targetClient.getClientName(), message));
        } else {
            // Target client not found in the room
            // Optionally, send an error message back to the sender
            sender.sendMessage(String.format("User with ID '%d' not found in the room.", targetClientId));
        }
    }

    /**
     * Attempts to close the room to free up resources if it's empty.
     */
    private void autoCleanup() {
        if (!Room.LOBBY.equalsIgnoreCase(name) && clientsInRoom.isEmpty()) {
            close();
        }
    }

    @Override
    public void close() {
        // Attempt to gracefully close and migrate clients
        if (!clientsInRoom.isEmpty()) {
            sendMessage(null, "Room is shutting down, migrating to lobby");
            info(String.format("Migrating %d clients", clientsInRoom.size()));
            clientsInRoom.values().forEach(client -> Server.INSTANCE.joinRoom(Room.LOBBY, client));
            clientsInRoom.clear();
        }
        Server.INSTANCE.removeRoom(this);
        isRunning = false;
        info(String.format("Room[%s] closed", name));
    }

    // Send/sync data to client(s)

    /**
     * Sends to all clients details of a disconnected client.
     */
    // kr553 10/21/2024
    protected synchronized void sendDisconnect(ServerThread client) {
        info(String.format("Sending disconnect status to %d recipients", clientsInRoom.size()));
        clientsInRoom.values().removeIf(clientInRoom -> {
            boolean failedToSend = !clientInRoom.sendDisconnect(client.getClientId(), client.getClientName());
            if (failedToSend) {
                info(String.format("Removing disconnected client[%s] from list", clientInRoom.getClientId()));
                disconnect(clientInRoom);
            }
            return failedToSend;
        });
    }

    /**
     * Syncs info of existing users in room with the client.
     */
    protected synchronized void syncRoomList(ServerThread client) {
        clientsInRoom.values().forEach(clientInRoom -> {
            if (clientInRoom.getClientId() != client.getClientId()) {
                client.sendClientSync(clientInRoom.getClientId(), clientInRoom.getClientName());
            }
        });
    }

    /**
     * Syncs room status of one client to all connected clients.
     */
    protected synchronized void sendRoomStatus(long clientId, String clientName, boolean isConnect) {
        info(String.format("Sending room status to %d recipients", clientsInRoom.size()));
        clientsInRoom.values().removeIf(client -> {
            boolean failedToSend = !client.sendRoomAction(clientId, clientName, getName(), isConnect);
            if (failedToSend) {
                info(String.format("Removing disconnected client[%s] from list", client.getClientId()));
                disconnect(client);
            }
            return failedToSend;
        });
    }

    /**
     * Sends a message from the sender to all clients in the room.
     * If the sender is null, it's considered a server message.
     */
    // kr553 10/21/2024
    protected synchronized void sendMessage(ServerThread sender, String message) {
        if (!isRunning) {
            return;
        }
    
        String formattedMessage = TextFX.formatText(message);
        long senderId = sender == null ? ServerThread.DEFAULT_CLIENT_ID : sender.getClientId();
    
        for (ServerThread client : clientsInRoom.values()) {
            if (client.isMuted(senderId)) {
                continue; // Skip sending the message to this client
            }
    
            boolean messageSent = client.sendMessage(senderId, formattedMessage);
    
            if (!messageSent) {
                info(String.format("Removing disconnected client[%s] from list", client.getClientId()));
                disconnect(client);
            }
        }
    }
    
    

    // End send data to client(s)

    // Receive data from ServerThread

    protected void handleCreateRoom(ServerThread sender, String roomName) {
        if (Server.INSTANCE.createRoom(roomName)) {
            Server.INSTANCE.joinRoom(roomName, sender);
            sender.sendMessage("Room '" + roomName + "' created successfully and you joined.");
        } else {
            sender.sendMessage("Room '" + roomName + "' already exists.");
        }
    }

    protected void handleJoinRoom(ServerThread sender, String room) {
        if (!Server.INSTANCE.joinRoom(room, sender)) {
            sender.sendMessage(String.format("Room '%s' doesn't exist.", room));
        }
    }

    protected void clientDisconnect(ServerThread sender) {
        disconnect(sender);
    }

    // End receive data from ServerThread
}
