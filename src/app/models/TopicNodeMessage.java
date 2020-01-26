package app.models;

import java.util.UUID;
import app.interfaces.MessageInterface;

public class TopicNodeMessage implements MessageInterface {
    /**
     * Default serial for serialization
     */
    private static final long serialVersionUID = -6287459226984339714L;

    private final UUID uuid;
    private boolean isLeader = false;
    private Topic topic;

    public TopicNodeMessage(Topic topic, UUID uuid, boolean isLeader) {
        this.topic = topic;
        this.uuid = uuid;
        this.isLeader = isLeader;     
    }
    
    public Topic getTopic() {
        return this.topic;
    }

    public UUID getUUID() {
        return this.uuid;
    }

    public boolean getIsLeader() {
        return this.isLeader;
    }

    public void setIsLeader(boolean isLeader) {
        this.isLeader = isLeader;
    }
}