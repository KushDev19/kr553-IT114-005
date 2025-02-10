package Project;

import java.io.Serializable;
import java.util.List;

public class Payload implements Serializable {
    private PayloadType payloadType; // The type of the payload
    private long clientId;           // ID of the sender client
    private long targetClientId;     // ID of the target client (e.g., for mute/unmute)
    private String message;          // Message content
    private String senderName;       // Name of the sender
    private long timestamp;          // Unix timestamp in milliseconds
    private List<String> mutedUsers; // List of muted users

    // Getter and Setter for payloadType
    public PayloadType getPayloadType() {
        return payloadType;
    }

    public void setPayloadType(PayloadType payloadType) {
        this.payloadType = payloadType;
    }

    // Getter and Setter for clientId
    public long getClientId() {
        return clientId;
    }

    public void setClientId(long clientId) {
        this.clientId = clientId;
    }

    // Getter and Setter for targetClientId
    public long getTargetClientId() {
        return targetClientId;
    }

    public void setTargetClientId(long targetClientId) {
        this.targetClientId = targetClientId;
    }

    // Getter and Setter for message
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    // Getter and Setter for senderName
    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    // Getter and Setter for timestamp
    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    // Getter and Setter for mutedUsers
    public List<String> getMutedUsers() {
        return mutedUsers;
    }

    public void setMutedUsers(List<String> mutedUsers) {
        this.mutedUsers = mutedUsers;
    }

    @Override
    public String toString() {
        return String.format(
            "Payload [Type: %s, Client ID: %s, Sender: %s, Target: %s, Timestamp: %s, Message: %s, Muted Users: %s]",
            getPayloadType(), getClientId(), getSenderName(), getTargetClientId(), getTimestamp(), getMessage(), getMutedUsers()
        );
    }
}
