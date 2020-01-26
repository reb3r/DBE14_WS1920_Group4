package app.models;

public class HearbeatFailedMessage extends Message {    
    /**
     * Serial for serialization
     */
    private static final long serialVersionUID = 2791846308833784870L;

    private RightNeighbor missingRightNeighbor;

    public HearbeatFailedMessage(Topic topic, RightNeighbor missingRightNeighbor, String name, String content) {
        super(topic, name, content);
        this.missingRightNeighbor = missingRightNeighbor;        
    }
    
    public RightNeighbor getMissingRightNeighbor() {
        return this.missingRightNeighbor;
    }
}