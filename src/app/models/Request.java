package app.models;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class Request implements Serializable {

    /**
     * Default serial for serialization
     */
    private static final long serialVersionUID = 325212L;

    /**
     * Sender of the request
     */
    private InetAddress sender;

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
        try {
            this.sender = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            // do nothing....
        }
    }

    public Request(Object payload, int sequenceId, InetAddress sender) {
        this(payload, sequenceId);
        this.sender = sender;
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

    public InetAddress getSender() {
        return this.sender;
    }

    public void setSender(InetAddress sender) {
        this.sender = sender;
    }

}