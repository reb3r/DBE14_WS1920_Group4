package app.models;

import java.io.Serializable;

/**
 * This class represents a topic-to-neighbor mappping
 */
public class NewTopicNeighborMessage implements Serializable {
    /**
     * Serial for serialization
     */
    private static final long serialVersionUID = 4407212071487382396L;

    /**
     * Topic to witch the new neighbor belongs
     */
    private Topic topic;

    /**
     * IP adress of old right neighbor
     */
    private RightNeighbor oldRightNeighbor;

    /**
     * IP adress of new right neighbor
     */
    private RightNeighbor newRightNeighbor;

    public NewTopicNeighborMessage(Topic topic, RightNeighbor oldRightNeighbor, RightNeighbor newRightNeighbor) {
        this.topic = topic;
        this.oldRightNeighbor = oldRightNeighbor;
        this.newRightNeighbor = newRightNeighbor;
    }

    public Topic getTopic() {
        return this.topic;
    }

    public RightNeighbor getOldRightNeighbor() {
        return this.oldRightNeighbor;
    }

    public RightNeighbor getNewRightNeighbor() {
        return this.newRightNeighbor;
    }
}