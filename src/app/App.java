package app;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import app.models.Message;
import app.models.Neighbors;
import app.models.SubscriptionMessage;
import app.models.Topic;
import app.models.Leader;
import app.multicast.MulticastPublisher;
import app.multicast.MulticastReceiver;

public class App {

    // public static List<Request> requests = new LinkedList<>();

    public static List<Topic> topics = new LinkedList<>();
    public static List<Topic> subscribedTopics = new LinkedList<>();
    public static Map<UUID, Neighbors> topicNeighbours = new HashMap<UUID, Neighbors>();

    public static List<Message> messages = new LinkedList<>();

    public static void main(String[] args) throws Exception {
        MulticastReceiver multicastReceiver = new MulticastReceiver();
        multicastReceiver.start();

        MulticastPublisher multicastPublisher = MulticastPublisher.getInstance();

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
                System.out.println("'topic subscribe' to subscribe to one of the available topics");
                System.out.println("'topic print' to print all topics");
                System.out.println("'message add' to add a new message to a topic");
                System.out.println("'message print' to print messages of an choosen topic");
                System.out.println("'help' to show this help");
                System.out.println("'quit' to end programm");
            } else if ("topic add".equals(line)) {
                System.out.print("Topics' name: ");
                Topic topic = new Topic(getLine());
                Leader leader = new Leader(InetAddress.getLocalHost()); // Creator of the topic declares himself as the
                                                                        // leader
                topic.setLeader(leader);
                topicNeighbours.put(topic.getUUID(), null);
                multicastPublisher.announceTopic(topic);
            } else if ("topic subscribe".equals(line)) {
                Topic topic;
                System.out.println("To which topic do you want to subscribe?");
                do {
                    topic = topicCliChooser(false);
                    if (App.subscribedTopics.contains(topic)) {
                        System.out.println("You have already subscribed to the topic '" + topic.getName()
                                + "'. Please choose another one.");
                    }
                } while (App.subscribedTopics.contains(topic));
                App.subscribedTopics.add(topic);
                multicastPublisher.sendMessage(
                        new SubscriptionMessage(topic, "Subscribed", InetAddress.getLocalHost().toString()));
            } else if ("topic print".equals(line)) {
                System.out.println("Received Topics' name: ");
                Iterator<Topic> it = App.topics.iterator();
                while (it.hasNext()) {
                    System.out.println(it.next().getName());
                }
            } else if ("message add".equals(line)) {
                if (App.topics.size() < 1) {
                    System.out.println("No topics available. Please add topic before...");
                    continue;
                }
                if (App.subscribedTopics.size() < 1) {
                    System.out.println(
                            "You are not subscribed to any available topic. Please subsrcibe to a topic before adding a message.");
                    continue;
                }
                System.out.println("To which topic do you want to add a message?");
                Topic topic = topicCliChooser(true);
                System.out.println("Add a new message to topic " + topic.getName() + ":");
                System.out.print("Messages' name: ");
                String messageName = getLine();
                System.out.print("Messages' content: ");
                String messageContent = getLine(); // maybe content should be more as one line ;-)
                Message message = new Message(topic, messageName, messageContent);
                multicastPublisher.sendMessage(message);
            } else if ("message print".equals(line)) {
                if (App.topics.size() < 1) {
                    System.out.println("No topics available. Please add topic before...");
                    continue;
                }
                if (App.subscribedTopics.size() < 1) {
                    System.out.println(
                            "You are not subscribed to any available topic. Please subsrcibe to a topic before adding a message.");
                    continue;
                }
                Topic topic = topicCliChooser(true);
                System.out.println("The " + App.messages.size() + " messages to topic " + topic.getName() + " are:");
                Iterator<Message> it = App.messages.iterator();
                while (it.hasNext()) {
                    Message message = it.next();
                    if (message.getTopic().equals(topic)) {
                        System.out.println("Message Name: " + message.getName());
                        System.out.println("Message Content: " + message.getContent());
                    }
                    if (it.hasNext()) {
                        // Print divider between messages if there are more on the list
                        System.out.println("----------------------------------------------");
                    }
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

    private static Topic topicCliChooser(Boolean chooseFromSubscribed) throws IOException {
        List<Topic> lstTopics;

        if (chooseFromSubscribed) {
            lstTopics = App.subscribedTopics;
        } else {
            lstTopics = App.topics;
        }

        for (Topic element : lstTopics) {
            int index = lstTopics.indexOf(element);
            System.out.println(index + ": " + element.getName());
        }

        String indexInput = getLine();
        try {
            Topic topic = lstTopics.get(Integer.valueOf(indexInput));
            return topic;
        } catch (IndexOutOfBoundsException e) {
            System.out.println("Your selection was no good. Please try again...");
            return App.topicCliChooser(chooseFromSubscribed);
        }
    }
}
