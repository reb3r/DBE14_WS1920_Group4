package app;

import java.util.LinkedList;
import java.util.List;

import app.models.Topic;
import app.multicast.MulticastPublisher;
import app.multicast.MulticastReceiver;

public class App {
    public static void main(String[] args) throws Exception {
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
        // multicastPublisher.multicast("Hello distributed world!");

        Topic newTopic = new Topic("ttt");
        multicastPublisher.announceTopic(newTopic);

        // Terminate all joined receiver
        multicastPublisher.multicast("end"); // Receiver terminates on "end"-Nessage
    }
}