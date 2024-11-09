package Project;

import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class Room implements AutoCloseable{
    private String name;// unique name of the Room
    private volatile boolean isRunning = false;
    private ConcurrentHashMap<Long, ServerThread> clientsInRoom = new ConcurrentHashMap<Long, ServerThread>();

    public final static String LOBBY = "lobby";

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
        sendMessage(null, resultMessage);
    }

    public void processFlipCommand(ServerThread client) {
        Random rand = new Random();
        String result = rand.nextBoolean() ? "heads" : "tails";
        String resultMessage = String.format("%s flipped a coin and got %s", client.getClientName(), result);

        // Broadcast the result to all clients in the room
        sendMessage(null, resultMessage);
    }

    protected synchronized void addClient(ServerThread client) {
        if (!isRunning) { // block action if Room isn't running
            return;
        }
        if (clientsInRoom.containsKey(client.getClientId())) {
            info("Attempting to add a client that already exists in the room");
            return;
        }
        clientsInRoom.put(client.getClientId(), client);
        client.setCurrentRoom(this);

        // notify clients of someone joining
        sendRoomStatus(client.getClientId(), client.getClientName(), true);
        // sync room state to joiner
        syncRoomList(client);

        info(String.format("%s[%s] joined the Room[%s]", client.getClientName(), client.getClientId(), getName()));

    }

    //kr553 10/21/2024
    protected synchronized void removedClient(ServerThread client) {
        if (!isRunning) { // block action if Room isn't running
            return;
        }
        // notify remaining clients of someone leaving
        // happen before removal so leaving client gets the data
        sendRoomStatus(client.getClientId(), client.getClientName(), false);
        clientsInRoom.remove(client.getClientId());

        info(String.format("%s[%s] left the room", client.getClientName(), client.getClientId(), getName()));

        autoCleanup();

    }

    /**
     * Takes a ServerThread and removes them from the Server
     * Adding the synchronized keyword ensures that only one thread can execute
     * these methods at a time,
     * preventing concurrent modification issues and ensuring thread safety
     * 
     * @param client
     */
    //kr553 10/21/2024
    protected synchronized void disconnect(ServerThread client) {
        if (!isRunning) { // block action if Room isn't running
            return;
        }
        long id = client.getClientId();
        sendDisconnect(client);
        client.disconnect();
        // removedClient(client); // <-- use this just for normal room leaving
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
     * Attempts to close the room to free up resources if it's empty
     */
    private void autoCleanup() {
        if (!Room.LOBBY.equalsIgnoreCase(name) && clientsInRoom.isEmpty()) {
            close();
        }
    }

    public void close() {
        // attempt to gracefully close and migrate clients
        if (!clientsInRoom.isEmpty()) {
            sendMessage(null, "Room is shutting down, migrating to lobby");
            info(String.format("migrating %s clients", name, clientsInRoom.size()));
            clientsInRoom.values().removeIf(client -> {
                Server.INSTANCE.joinRoom(Room.LOBBY, client);
                return true;
            });
        }
        Server.INSTANCE.removeRoom(this);
        isRunning = false;
        clientsInRoom.clear();
        info(String.format("closed", name));
    }

    // send/sync data to client(s)

    /**
     * Sends to all clients details of a disconnect client
     * @param client
     */
    //kr553 10/21/2024
    protected synchronized void sendDisconnect(ServerThread client) {
        info(String.format("sending disconnect status to %s recipients", getName(), clientsInRoom.size()));
        clientsInRoom.values().removeIf(clientInRoom -> {
            boolean failedToSend = !clientInRoom.sendDisconnect(client.getClientId(), client.getClientName());
            if (failedToSend) {
                info(String.format("Removing disconnected client[%s] from list", client.getClientId()));
                disconnect(client);
            }
            return failedToSend;
        });
    }

    /**
     * Syncs info of existing users in room with the client
     * 
     * @param client
     */
    protected synchronized void syncRoomList(ServerThread client) {

        clientsInRoom.values().forEach(clientInRoom -> {
            if (clientInRoom.getClientId() != client.getClientId()) {
                client.sendClientSync(clientInRoom.getClientId(), clientInRoom.getClientName());
            }
        });
    }

    /**
     * Syncs room status of one client to all connected clients
     * 
     * @param clientId
     * @param clientName
     * @param isConnect
     */
    protected synchronized void sendRoomStatus(long clientId, String clientName, boolean isConnect) {
        info(String.format("sending room status to %s recipients", getName(), clientsInRoom.size()));
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
     * Sends a basic String message from the sender to all connectedClients
     * Internally calls processCommand and evaluates as necessary.
     * Note: Clients that fail to receive a message get removed from
     * connectedClients.
     * Adding the synchronized keyword ensures that only one thread can execute
     * these methods at a time,
     * preventing concurrent modification issues and ensuring thread safety
     * 
     * @param message
     * @param sender  ServerThread (client) sending the message or null if it's a
     *                server-generated message
     */
    //kr553 10/21/2024
    protected synchronized void sendMessage(ServerThread sender, String message) {
        if (!isRunning) {
            return;
        }
    
        // Apply text formatting
        String formattedMessage = TextFX.formatText(message);
    
        long senderId = sender == null ? ServerThread.DEFAULT_CLIENT_ID : sender.getClientId();
    
        info(String.format("sending message to %s recipients: %s", clientsInRoom.size(), formattedMessage));
    
        clientsInRoom.values().removeIf(client -> {
            boolean failedToSend = !client.sendMessage(senderId, formattedMessage);
            if (failedToSend) {
                info(String.format("Removing disconnected client[%s] from list", client.getClientId()));
                disconnect(client);
            }
            return failedToSend;
        });
    }
    
    
    // end send data to client(s)

    // receive data from ServerThread
    protected void handleCreateRoom(ServerThread sender, String room) {
        if (Server.INSTANCE.createRoom(room)) {
            Server.INSTANCE.joinRoom(room, sender);
        } else {
            sender.sendMessage(String.format("Room %s already exists", room));
        }
    }

    protected void handleJoinRoom(ServerThread sender, String room) {
        if (!Server.INSTANCE.joinRoom(room, sender)) {
            sender.sendMessage(String.format("Room %s doesn't exist", room));
        }
    }

    protected void clientDisconnect(ServerThread sender) {
        disconnect(sender);
    }

    // end receive data from ServerThread
}
