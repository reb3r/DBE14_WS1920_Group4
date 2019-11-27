package app;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import app.models.Request;
import app.models.Topic;
import app.models.VectorClock;
import app.multicast.MulticastPublisher;
import app.multicast.MulticastReceiver;

public class App {

    public static VectorClock vectorClock = new VectorClock();

    public static List<Request> requests = new LinkedList<>();

    public static List<Topic> topics = new LinkedList<>();

    public static void main(String[] args) throws Exception {
        UUID uuid = UUID.randomUUID();
        System.out.println("Started with UUID " + uuid.toString());
        App.vectorClock.set(uuid.toString(), 0);
        // Start two receiver threads and save references to List
        // int receiverCount = 1;
        // List<MulticastReceiver> receivers = new LinkedList<MulticastReceiver>();
        // for (int i = 0; i < receiverCount; i++) {
        MulticastReceiver multicastReceiver = new MulticastReceiver(uuid.toString());
        multicastReceiver.start();
        // receivers.add(multicastReceiver); // Save references to receiver threads for
        // better times...
        // }

        // Multicast one payload message and one termination message
        MulticastPublisher multicastPublisher = new MulticastPublisher(uuid.toString());
        // multicastPublisher.multicast("Hello distributed world!");

        // Topic newTopic = new Topic("First Topic");
        // multicastPublisher.announceTopic(newTopic);

        // Message message = new Message(newTopic, "First Message", "Content of first
        // message", new VectorClock());
        // multicastPublisher.sendMessage(message);

        // Terminate all joined receiver
        // Topic endTopic = new Topic("end");
        // multicastPublisher.announceTopic(endTopic);

        // CLI Loop
        while (true) {
            System.out.print("Command: ");
            String line = getLine();
            if ("quit".equals(line)) {
                Topic topic = new Topic("end");
                multicastPublisher.announceTopic(topic);
                break;
            } else if ("help".equals(line)) {
                System.out.println("'topic add' to add a new topic");
                System.out.println("'topic print' to print all topics");
                System.out.println("'help' to show this help");
                System.out.println("'quit' to end programm");
            } else if ("topic add".equals(line)) {
                System.out.print("Topics' name: ");
                Topic topic = new Topic(getLine());
                multicastPublisher.announceTopic(topic);
            } else if ("topic print".equals(line)) {
                System.out.println("Received Topics' name: ");
                Iterator<Topic> it = App.topics.iterator();
                while (it.hasNext()) {
                    System.out.println(it.next().getName());
                }
            } else {
                System.out.println("Did not understand that command. Use 'help' for further assistance.");
            }
        }
    }

    private static String getLine() throws IOException {
        BufferedReader buffer = new BufferedReader(new InputStreamReader(System.in));
        return buffer.readLine();
    }
}