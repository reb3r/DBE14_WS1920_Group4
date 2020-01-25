package app.models;

import app.interfaces.MessageInterface;

public class RingCompleteMessage implements MessageInterface {    
    /**
     * Default serial for serialization
     */
    private static final long serialVersionUID = -2743281066281915411L;
    
    private Topic topic;

    public RingCompleteMessage(Topic topic) {
        this.topic = topic;  
    }
    
    public Topic getTopic() {
        return this.topic;
    }
}