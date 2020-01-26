package app.models;

import app.interfaces.MessageInterface;

public class Message implements MessageInterface {

    /**
     * Default serial for serialization
     */
    private static final long serialVersionUID = 46822L;

    /**
     * Message name
     */
    private String name;

    /**
     * Content of message
     */
    private String content;

    /**
     * Topic of message
     */
    private Topic topic;

    public Message(Topic topic, String name, String content) {
        this.topic = topic;
        this.name = name;
        this.content = content;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContent() {
        return this.content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Topic getTopic() {
        return this.topic;
    }

    public void setTopic(Topic topic) {
        this.topic = topic;
    }
}