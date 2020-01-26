package app.models;

public class UnsubscriptionMessage extends Message {
    /**
     * Serial for serialization
     */
    private static final long serialVersionUID = 8582720754587195421L;

    private RightNeighbor rightNeighbor;

    public UnsubscriptionMessage(Topic topic, RightNeighbor rightNeighbor, String name, String content) {
        super(topic, name, content);

        this.rightNeighbor = rightNeighbor;
    }
    
    public RightNeighbor getRightNeighbor() {
        return this.rightNeighbor;
    }
}