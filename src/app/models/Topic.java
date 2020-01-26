package app.models;

import java.io.Serializable;
import java.util.UUID;

public class Topic implements Serializable {

    /**
     * Default serial for serialization
     */
    private static final long serialVersionUID = 32525L;

    /**
     * Unique topic ID
     */
    private UUID uuid;

     /**
     * Tells if topic is still in use or should be deleted
     */
    private String state = "ACTIVE";

    /**
     * Topics name
     */
    private String name;

    /**
     * Leader of the topic
     */
	private Leader leader;

    public Topic(String name) {
        this.name = name;
        this.uuid = UUID.randomUUID();
    }

    public UUID getUUID() {
        return this.uuid;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getState() {
        return this.state;
    }

    public void setName(UUID uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Leader getLeader() {
        return this.leader;
    }

    public void setLeader(Leader leader) {
        this.leader = leader;
    }

    /**
     * It may be checked if this super short implementation is sufficient for this
     * project
     */
    public boolean equals(Object obj) {
        if (obj instanceof Topic) {
            if (((Topic) obj).getUUID().equals(this.getUUID())) {
                return true;
            }
        }
        return false;
    }

}