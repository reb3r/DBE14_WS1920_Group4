package app.models;

import java.io.Serializable;

/**
 * This class represents a topic-to-neighbor mappping
 */
public class TopicNeighbor implements Serializable {
    /**
     * Serial for serialization
     */
    private static final long serialVersionUID = -3718386399221704017L;

    /**
     * Topic to witch the neighbor belongs
     */
    private Topic topic;

    /**
     * IP adress of right neighbor
     */
    private RightNeighbor neighbor;

    public TopicNeighbor(Topic topic, RightNeighbor neighbor) {
        this.topic = topic;
        this.neighbor = neighbor;
    }

    public Topic getTopic() {
        return this.topic;
    }

    public void setTopic(Topic topic) {
        this.topic = topic;
    }

    public RightNeighbor getNeighbor() {
        return this.neighbor;
    }

    public void setNeighbor(RightNeighbor neighbor) {
        this.neighbor = neighbor;
    }
}