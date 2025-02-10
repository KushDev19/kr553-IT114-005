package Project;

public class RollPayload extends Payload {
    private int rollRange; // For format: /roll #
    private int numberOfDice; // For format: /roll #d#
    private int sidesPerDie; // For format: /roll #d#

    public RollPayload() {
        setPayloadType(PayloadType.ROLL);
        setTimestamp(System.currentTimeMillis());
    }

    // Getter and Setter for rollRange
    public int getRollRange() {
        return rollRange;
    }

    public void setRollRange(int rollRange) {
        this.rollRange = rollRange;
    }

    // Getter and Setter for numberOfDice
    public int getNumberOfDice() {
        return numberOfDice;
    }

    public void setNumberOfDice(int numberOfDice) {
        this.numberOfDice = numberOfDice;
    }

    // Getter and Setter for sidesPerDie
    public int getSidesPerDie() {
        return sidesPerDie;
    }

    public void setSidesPerDie(int sidesPerDie) {
        this.sidesPerDie = sidesPerDie;
    }

    @Override
    public String toString() {
        String baseString = super.toString();
        if (numberOfDice > 0 && sidesPerDie > 0) {
            return baseString + String.format(
                    " [RollPayload] Format: %dd%d", numberOfDice, sidesPerDie);
        } else if (rollRange > 0) {
            return baseString + String.format(
                    " [RollPayload] Range: %d", rollRange);
        } else {
            return baseString + " [RollPayload] Invalid parameters.";
        }
    }

}