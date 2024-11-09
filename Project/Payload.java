package Project;

import java.io.Serializable;

public class Payload implements Serializable {
    private PayloadType payloadType;
    private long clientId;
    private String message;
    private String senderName;
    private long timestamp; // Unix timestamp in milliseconds

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

    @Override
    public String toString() {
        return String.format(
            "Payload [Type: %s, Client ID: %s, Sender: %s, Timestamp: %s, Message: %s]",
            getPayloadType(), getClientId(), getSenderName(), getTimestamp(), getMessage()
        );
    }
}
