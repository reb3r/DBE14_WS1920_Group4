package app.models;

import java.io.Serializable;

public class Request implements Serializable {

    /**
     * Default serial for serialization
     */
    private static final long serialVersionUID = 325212L;

    /**
     * Payload
     */
    private Object payload;

    private VectorClock vectorClock;

    public Request(Object payload, VectorClock vectorClock) {
        this.payload = payload;
        this.vectorClock = vectorClock;
    }

    public Object getPayload() {
        return this.payload;
    }

    public void setPayload(Object payload) {
        this.payload = payload;
    }

    public VectorClock getVectorClock() {
        return this.vectorClock;
    }

    public void setVectorClock(VectorClock vectorClock) {
        this.vectorClock = vectorClock;
    }

}