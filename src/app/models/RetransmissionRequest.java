package app.models;

import java.io.Serializable;

/**
 * This Request will be sent by Receiver if there is a hole in the holdback
 * queue. This message should be transmitted via unicast datagrams to the sender
 * of an message.
 */
public class RetransmissionRequest implements Serializable {

    /**
     * Default serial for serialization
     */
    private static final long serialVersionUID = 3423412L;

    /**
     * Sender specific sequence id which should be retransmitted
     */
    private int sequenceId;

    public RetransmissionRequest(int sequenceId) {
        this.sequenceId = sequenceId;
    }

    public int getSequenceId() {
        return this.sequenceId;
    }

    public void setSequenceId(int sequenceId) {
        this.sequenceId = sequenceId;
    }

}