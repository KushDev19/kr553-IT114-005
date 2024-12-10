package Project;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.Socket;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

/**
 * A server-side representation of a single client.
 * This class is more about the data and abstracted communication
 */
public class ServerThread extends BaseServerThread {
    public static final long DEFAULT_CLIENT_ID = -1;
    private Room currentRoom;
    private long clientId;
    private String clientName;
    private Consumer<ServerThread> onInitializationComplete; // callback to inform when this object is ready

    // Set to store client IDs that this client has muted
    private Set<Long> mutedClientIds = new HashSet<>();

    /**
     * Wraps the Socket connection and takes a Server reference and a callback
     * 
     * @param myClient
     * @param onInitializationComplete method to inform listener that this object is
     *                                 ready
     */
    protected ServerThread(Socket myClient, Consumer<ServerThread> onInitializationComplete) {
        Objects.requireNonNull(myClient, "Client socket cannot be null");
        Objects.requireNonNull(onInitializationComplete, "callback cannot be null");
        info("ServerThread created");
        // get communication channels to single client
        this.client = myClient;
        this.clientId = ServerThread.DEFAULT_CLIENT_ID; // this is updated later by the server
        this.onInitializationComplete = onInitializationComplete;
    }

    public void setClientName(String name) {
        if (name == null) {
            throw new NullPointerException("Client name can't be null");
        }
        this.clientName = name;
        onInitialized();
    }

    public String getClientName() {
        return clientName;
    }

    public long getClientId() {
        return this.clientId;
    }

    protected Room getCurrentRoom() {
        return this.currentRoom;
    }

    protected void setCurrentRoom(Room room) {
        if (room == null) {
            throw new NullPointerException("Room argument can't be null");
        }
        currentRoom = room;
    }

    @Override
    protected void onInitialized() {
        loadMuteList(); // Load the mute list for this client
        info("Mute list initialized: " + mutedClientIds); // Log muted client IDs
        onInitializationComplete.accept(this); // Notify server that initialization is complete
    }
    

    private void loadMuteList() {
        File muteFile = new File(MUTE_FILE_DIR, clientName + ".txt");
        if (muteFile.exists()) {
            synchronized (mutedClientIds) {
                try (BufferedReader reader = new BufferedReader(new FileReader(muteFile))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        long mutedId = Long.parseLong(line.trim());
                        // Prevent self-muting during initialization
                        if (mutedId != clientId) {
                            mutedClientIds.add(mutedId);
                        }
                    }
                } catch (IOException e) {
                    info("Error reading mute file for " + clientName + ": " + e.getMessage());
                }
            }
        }
    }
    

    private void saveMuteList() {
        synchronized (mutedClientIds) {
            File muteFile = new File(MUTE_FILE_DIR, clientName + ".txt");
            muteFile.getParentFile().mkdirs(); // Ensure directory exists
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(muteFile))) {
                for (Long mutedId : mutedClientIds) {
                    writer.write(mutedId.toString());
                    writer.newLine();
                }
            } catch (IOException e) {
                info("Error writing mute file for " + clientName + ": " + e.getMessage());
            }
        }
    }

    @Override
    protected void info(String message) {
        System.out.println(String.format("ServerThread[%s(%s)]: %s", getClientName(), getClientId(), message));
    }

    @Override
    protected void cleanup() {
        currentRoom = null;
        super.cleanup();
    }

    @Override
    protected void disconnect() {
        // sendDisconnect(clientId, clientName);
        super.disconnect();
    }

    // kr553 11/9/2024
    private void processRollPayload(RollPayload payload) {
        if (currentRoom != null) {
            currentRoom.processRollCommand(this, payload);
        } else {
            System.out.println("No room assigned to process roll command.");
        }
    }

    private void processFlipPayload(Payload payload) {
        if (currentRoom != null) {
            currentRoom.processFlipCommand(this);
        } else {
            System.out.println("No room assigned to process flip command.");
        }
    }

    private static final String MUTE_FILE_DIR = "mutes"; // Directory for mute files

    // handle received message from the Client
    // kr553 10/20/2024
    @Override
    protected void processPayload(Payload payload) {
        try {
            switch (payload.getPayloadType()) {
                case CLIENT_CONNECT:
                    ConnectionPayload cp = (ConnectionPayload) payload;
                    setClientName(cp.getClientName());
                    break;
                case PRIVATE_MESSAGE:
                    processPrivateMessagePayload((PrivateMessagePayload) payload);
                    break;
                case MESSAGE:
                    info(String.format("Processing MESSAGE payload: %s", payload.getMessage()));
                    if (currentRoom != null) {
                        currentRoom.sendMessage(this, payload.getMessage());
                    } else {
                        info("No room assigned for MESSAGE payload.");
                    }
                    break;
                case ROOM_CREATE:
                    currentRoom.handleCreateRoom(this, payload.getMessage());
                    break;
                case ROOM_JOIN:
                    currentRoom.handleJoinRoom(this, payload.getMessage());
                    break;
                case DISCONNECT:
                    currentRoom.disconnect(this);
                    break;
                case ROLL:
                    processRollPayload((RollPayload) payload);
                    break;
                case FLIP:
                    processFlipPayload(payload);
                    break;
                case MUTE:
                    handleMute(payload);
                    break;
                case UNMUTE:
                    handleUnmute(payload);
                    break;
                default:
                    System.out.println("Unhandled payload type: " + payload.getPayloadType());
                    break;
            }
        } catch (Exception e) {
            System.out.println("Could not process Payload: " + payload);
            e.printStackTrace();
        }
    }

    private void handleMute(Payload payload) {
        long targetClientId = payload.getTargetClientId();
        if (targetClientId == clientId) {
            sendMessage("You cannot mute yourself.");
            return;
        }
    
        if (mutedClientIds.add(targetClientId)) { // Only add if not already muted
            saveMuteList(); // Save updated mute list
            sendMessage("You have muted " + payload.getMessage());
    
            // Notify the muted client
            ServerThread targetClient = Server.INSTANCE.getClientById(targetClientId);
            if (targetClient != null) {
                targetClient.sendMessage(clientName + " has muted you.");
            }
        }
    }
    

    private void handleUnmute(Payload payload) {
        long targetClientId = payload.getTargetClientId();
        if (mutedClientIds.remove(targetClientId)) { // Only remove if already muted
            saveMuteList(); // Save updated mute list
            sendMessage("You have unmuted " + payload.getMessage());

            // Notify the unmuted client
            ServerThread targetClient = Server.INSTANCE.getClientById(targetClientId);
            if (targetClient != null) {
                targetClient.sendMessage(clientName + " has unmuted you.");
            }
        }
    }

    private ServerThread getClientById(long clientId) {
        return Server.INSTANCE.getClientById(clientId);
    }

    public boolean isMuted(long clientId) {
        return mutedClientIds.contains(clientId);
    }

    // send methods to pass data back to the Client

    private void processPrivateMessagePayload(PrivateMessagePayload payload) {
        if (currentRoom != null) {
            currentRoom.sendPrivateMessage(this, payload.getTargetClientId(), payload.getMessage());
        } else {
            System.out.println("No room assigned to process private message.");
        }
    }

    public boolean sendPrivateMessage(long senderId, String message) {
        PrivateMessagePayload p = new PrivateMessagePayload();
        p.setClientId(senderId);
        p.setMessage(message);
        p.setPayloadType(PayloadType.PRIVATE_MESSAGE);
        return send(p);
    }

    public boolean sendClientSync(long clientId, String clientName) {
        ConnectionPayload cp = new ConnectionPayload();
        cp.setClientId(clientId);
        cp.setClientName(clientName);
        cp.setConnect(true);
        cp.setPayloadType(PayloadType.SYNC_CLIENT);
        return send(cp);
    }

    /**
     * Overload of sendMessage used for server-side generated messages
     * 
     * @param message
     * @return @see {@link #send(Payload)}
     */
    public boolean sendMessage(String message) {
        return sendMessage(ServerThread.DEFAULT_CLIENT_ID, message);
    }

    /**
     * Sends a message with the author/source identifier
     * 
     * @param senderId
     * @param message
     * @return @see {@link #send(Payload)}
     */

    // kr553 10/20/2024
    public boolean sendMessage(long senderId, String message) {
        Payload p = new Payload();
        p.setClientId(senderId);
        p.setMessage(message);
        p.setPayloadType(PayloadType.MESSAGE);
        return send(p);
    }

    /**
     * Tells the client information about a client joining/leaving a room
     * 
     * @param clientId   their unique identifier
     * @param clientName their name
     * @param room       the room
     * @param isJoin     true for join, false for leaving
     * @return success of sending the payload
     */
    public boolean sendRoomAction(long clientId, String clientName, String room, boolean isJoin) {
        ConnectionPayload cp = new ConnectionPayload();
        cp.setPayloadType(PayloadType.ROOM_JOIN);
        cp.setConnect(isJoin); // <-- determine if join or leave
        cp.setMessage(room);
        cp.setClientId(clientId);
        cp.setClientName(clientName);
        return send(cp);
    }

    /**
     * Tells the client information about a disconnect (similar to leaving a room)
     * 
     * @param clientId   their unique identifier
     * @param clientName their name
     * @return success of sending the payload
     */
    // kr553 10/21/2024
    public boolean sendDisconnect(long clientId, String clientName) {
        ConnectionPayload cp = new ConnectionPayload();
        cp.setPayloadType(PayloadType.DISCONNECT);
        cp.setConnect(false);
        cp.setClientId(clientId);
        cp.setClientName(clientName);
        return send(cp);
    }

    /**
     * Sends (and sets) this client their id (typically when they first connect)
     * 
     * @param clientId
     * @return success of sending the payload
     */
    public boolean sendClientId(long clientId) {
        this.clientId = clientId;
        ConnectionPayload cp = new ConnectionPayload();
        cp.setPayloadType(PayloadType.CLIENT_ID);
        cp.setConnect(true);
        cp.setClientId(clientId);
        cp.setClientName(clientName);
        return send(cp);
    }

    // end send methods
}