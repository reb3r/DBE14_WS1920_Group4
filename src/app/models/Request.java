package app.models;

import java.io.Serializable;

public class Request implements Serializable {

    /**
     * Default serial for serialization
     */
    private static final long serialVersionUID = 325212L;

    /**
     * Sender specific sequence id
     */
    private int sequenceId;

    /**
     * Payload
     */
    private Object payload;

    public Request(Object payload, int sequenceId) {
        this.payload = payload;
        this.sequenceId = sequenceId;
    }

    public Object getPayload() {
        return this.payload;
    }

    public void setPayload(Object payload) {
        this.payload = payload;
    }

    public int getSequenceId() {
        return this.sequenceId;
    }

    public void setSequenceId(int sequenceId) {
        this.sequenceId = sequenceId;
    }

}