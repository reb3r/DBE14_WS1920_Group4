package app.models;

public class SubscriptionMessage extends Message
{
    /**
     * Serial for serialization
     */
    private static final long serialVersionUID = 646439521525061286L;

    public SubscriptionMessage(Topic topic, String name, String content) {
        super(topic, name, content);
    }
}