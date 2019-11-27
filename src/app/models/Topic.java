package app.models;

import java.io.Serializable;

public class Topic implements Serializable {

    /**
     * Default serial for serialization
     */
    private static final long serialVersionUID = 32525L;

    /**
     * Topics name
     */
    private String name;

    public Topic(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

}