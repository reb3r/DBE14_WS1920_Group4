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
    private RightNeighbor rightNeighbor;

     /**
     * IP adress of left neighbor
     */
    private LeftNeighbor leftNeighbor;

    public TopicNeighbor(Topic topic, RightNeighbor rightNeighbor, LeftNeighbor leftNeighbor) {
        this.topic = topic;
        this.rightNeighbor = rightNeighbor;
        this.leftNeighbor = leftNeighbor;
    }

    public Topic getTopic() {
        return this.topic;
    }

    public void setTopic(Topic topic) {
        this.topic = topic;
    }

    public RightNeighbor getRightNeighbor() {
        return this.rightNeighbor;
    }

    public void setRightNeighbor(RightNeighbor rightNeighbor) {
        this.rightNeighbor = rightNeighbor;
    }

    public LeftNeighbor getLeftNeighbor() {
        return this.leftNeighbor;
    }

    public void setLeftNeighbor(LeftNeighbor leftNeighbor) {
        this.leftNeighbor = leftNeighbor;
    }
}