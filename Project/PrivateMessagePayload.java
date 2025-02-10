package Project;

import java.io.Serializable;

public class PrivateMessagePayload extends Payload implements Serializable {
    private long targetClientId;

    public long getTargetClientId() {
        return targetClientId;
    }

    public void setTargetClientId(long targetClientId) {
        this.targetClientId = targetClientId;
    }
}