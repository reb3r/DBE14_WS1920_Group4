package app.models;

import java.io.Serializable;

public class Message implements Serializable {

    /**
     * Default serial for serialization
     */
    private static final long serialVersionUID = 1L;

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

    /**
     * Vectorclock
     */
    private VectorClock vectorClock;

    public Message(Topic topic, String name, String content, VectorClock vectorClock) {
        this.topic = topic;
        this.name = name;
        this.content = content;
        this.vectorClock = vectorClock;
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

    public VectorClock getVectorClock() {
        return this.vectorClock;
    }

    public int compareTo(Message message) {
        VectorClock ownVectorClock = this.vectorClock;
        VectorClock foreignVectorClock = message.getVectorClock();

        // TODO: comparision
        return -1;
    }

}