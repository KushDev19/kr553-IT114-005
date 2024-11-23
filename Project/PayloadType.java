package Project;

public enum PayloadType {
    CLIENT_CONNECT, // client requesting to connect to server (passing of initialization data [name])
    CLIENT_ID,      // server sending client id
    SYNC_CLIENT,    // silent syncing of clients in room
    DISCONNECT,     // distinct disconnect action
    ROOM_CREATE,
    ROOM_JOIN,      // join/leave room based on boolean
    MESSAGE,        // sender and message
    ROLL,           // new type for roll command
    FLIP,          // new type for flip command
    PRIVATE_MESSAGE, // new type for private messages
    MUTE,           
    UNMUTE
}
