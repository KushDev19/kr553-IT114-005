package Project;

public class ConnectionPayload extends Payload {
    private String clientName;
    private boolean isConnect;

    public ConnectionPayload() {
        setPayloadType(PayloadType.CLIENT_CONNECT);
        setTimestamp(System.currentTimeMillis());
    }

    // Getter and Setter for clientName
    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    // Getter and Setter for isConnect
    public boolean isConnect() {
        return isConnect;
    }

    public void setConnect(boolean isConnect) {
        this.isConnect = isConnect;
    }

    @Override
    public String toString() {
        return super.toString() + String.format(
            " [ConnectionPayload] Client Name: %s, Status: %s",
            clientName, isConnect ? "connect" : "disconnect"
        );
    }
}
