package app.models;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import app.multicast.MulticastPublisher;
import app.multicast.MulticastReceiver;

public class Topic implements Serializable {

    /**
     * Default serial for serialization
     */
    private static final long serialVersionUID = 1L;

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

    public static void main(String[] args) throws Exception {
        System.out.println("Hello World");

        // Start two receiver threads and save references to List
        int receiverCount = 2;
        List<MulticastReceiver> receivers = new LinkedList<MulticastReceiver>();
        for (int i = 0; i < receiverCount; i++) {
            MulticastReceiver multicastReceiver = new MulticastReceiver();
            multicastReceiver.start();
            receivers.add(multicastReceiver); // Save references to receiver threads for better times...
        }

        // Multicast one payload message and one termination message
        MulticastPublisher multicastPublisher = new MulticastPublisher();
        multicastPublisher.multicast("Hello distributed world!");

        Topic newTopic = new Topic("ttt");
        multicastPublisher.announceTopic(newTopic);

        // Terminate all joined receiver
        multicastPublisher.multicast("end"); // Receiver terminates on "end"-Nessage
    }

}