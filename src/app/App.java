package app;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import app.models.Message;
import app.models.RightNeighbor;
import app.models.SubscriptionMessage;
import app.models.Topic;
import app.models.UnsubscriptionMessage;
import app.models.HearbeatFailedMessage;
import app.models.Leader;
import app.models.LeftNeighbor;
import app.multicast.MulticastPublisher;
import app.multicast.MulticastReceiver;

public class App {
    private static Timer timer;

    public static List<Topic> topics = new LinkedList<>();
    public static List<Topic> subscribedTopics = new LinkedList<>();

    public static Map<UUID, TopicNode> topicNodes = new HashMap<UUID, TopicNode>(); // Save node information for each
                                                                                    // subscribed topic
    // Save neighbors for each subscribed topic
    public static Map<UUID, RightNeighbor> rightTopicNeighbours = new HashMap<UUID, RightNeighbor>();
    public static Map<UUID, LeftNeighbor> leftTopicNeighbours = new HashMap<UUID, LeftNeighbor>();

    public static List<Message> messages = new LinkedList<>();

    public static void main(String[] args) throws Exception {
        MulticastReceiver multicastReceiver = new MulticastReceiver();
        multicastReceiver.start();

        MulticastPublisher multicastPublisher = MulticastPublisher.getInstance();

        timer = new Timer();

        // CLI Loop
        while (true) {
            System.out.print("Command: ");
            String line = getLine();
            if ("quit".equals(line)) {
                // Mark thread to stop
                multicastReceiver.stopThread();
                // Make sure the thread will go through the next iteration
                Topic topic = new Topic("end");
                multicastPublisher.announceTopic(topic);
                break;
            } else if ("help".equals(line)) {
                System.out.println("'topic add' to add a new topic");
                System.out.println("'topic delete' to delete a new topic (only as leader)");
                System.out.println("'topic modify name' to modify a topics name (only as leader)");
                System.out.println("'topic subscribe' to subscribe to one of the available topics");
                System.out.println("'topic unsubscribe' to unsubscribe from one of the subscribed topics");
                System.out.println("'topic print' to print all topics");
                System.out.println("'message add' to add a new message to a topic");
                System.out.println("'message print' to print messages of an choosen topic");
                System.out.println("'help' to show this help");
                System.out.println("'quit' to end programm");
            } else if ("topic add".equals(line)) {
                System.out.print("Topics' name: ");

                String topicName;

                do {
                    topicName = getLine();

                    if (existsTopicWithName(topicName)) {
                        System.out
                                .println("The name '" + topicName + "' is already in use. Please choose another one.");
                    }
                } while (existsTopicWithName(topicName));

                Socket s = new Socket("www.google.com", 80);
                InetAddress localHostAdress = s.getLocalAddress();
                s.close();
                Leader leader = new Leader(localHostAdress); // Creator of the topic declares himself as the
                                                             // leader

                Topic topic = new Topic(topicName);
                topic.setLeader(leader);

                rightTopicNeighbours.put(topic.getUUID(), null);
                leftTopicNeighbours.put(topic.getUUID(), null);

                subscribedTopics.add(topic); // a topic creator is automatically subscribed to the topic

                multicastPublisher.announceTopic(topic);

                InitHeartbeat(multicastPublisher, topic);
            } else if ("topic delete".equals(line)) {
                if (subscribedTopics.size() < 1) {
                    System.out.println(
                            "You are not subscribed to any topic. You can only delete topics which you are subscribed to.");
                    continue;
                }

                Topic topic;
                System.out.println("Which topic do you want to delete?");
                Socket s = new Socket("www.google.com", 80);
                InetAddress localHostAdress = s.getLocalAddress();
                s.close();
                do {
                    topic = topicCliChooser(true);

                    if (topic != null && topic.getLeader().getIPAdress().equals(localHostAdress) == false) {
                        System.out.println("You are not the leader of the topic '" + topic.getName()
                                + "'. You can only delete a topic as a leader. Please choose another one.");
                    }
                } while (topic != null && topic.getLeader().getIPAdress().equals(localHostAdress) == false);

                if (topic == null) {
                    continue;
                }

                topic.setState("DELETED");

                multicastPublisher.announceTopic(topic);
            } else if ("topic modify name".equals(line)) {
                if (subscribedTopics.size() < 1) {
                    System.out.println(
                            "You are not subscribed to any topic. You can only modify topics which you are subscribed to.");
                    continue;
                }

                Topic topic;
                System.out.println("Which topic do you want to rename?");
                Socket s = new Socket("www.google.com", 80);
                InetAddress localHostAdress = s.getLocalAddress();
                s.close();
                do {
                    topic = topicCliChooser(true);

                    if (topic != null && topic.getLeader().getIPAdress().equals(localHostAdress) == false) {
                        System.out.println("You are not the leader of the topic '" + topic.getName()
                                + "'. You can only modify a topic as a leader. Please choose another one.");
                    }
                } while (topic != null && topic.getLeader().getIPAdress().equals(localHostAdress) == false);

                if (topic == null) {
                    continue;
                }

                System.out.println("Please enter the new topic name.");
                String topicName;

                do {
                    topicName = getLine();

                    if (existsTopicWithName(topicName)) {
                        System.out
                                .println("The name '" + topicName + "' is already in use. Please choose another one.");
                    }
                } while (existsTopicWithName(topicName));

                topic.setName(topicName);

                multicastPublisher.announceTopic(topic);
            } else if ("topic subscribe".equals(line)) {
                if (topics.size() < 1) {
                    System.out.println("No topics available. Please add topic before...");
                    continue;
                }

                Topic topic;
                System.out.println("To which topic do you want to subscribe?");
                do {
                    topic = topicCliChooser(false);

                    if (subscribedTopics.contains(topic)) {
                        System.out.println("You have already subscribed to the topic '" + topic.getName()
                                + "'. Please choose another one.");
                    }
                } while (subscribedTopics.contains(topic));

                if (topic == null) {
                    continue;
                }

                subscribedTopics.add(topic);
                Socket s = new Socket("www.google.com", 80);
                InetAddress localHostAdress = s.getLocalAddress();
                s.close();
                multicastPublisher.sendMessage(
                        new SubscriptionMessage(topic, "Subscribed", localHostAdress.getHostAddress().toString()));
            } else if ("topic unsubscribe".equals(line)) {
                if (subscribedTopics.size() < 1) {
                    System.out.println("You are not subscribed to any topic.");
                    continue;
                }

                System.out.println("From which topic do you want to unsubscribe?");

                Topic topic = topicCliChooser(true);

                if (topic == null) {
                    continue;
                }

                Socket s = new Socket("www.google.com", 80);
                InetAddress localHostAdress = s.getLocalAddress();
                s.close();

                subscribedTopics.remove(topic);
                RightNeighbor rightNeighbor = App.rightTopicNeighbours.get(topic.getUUID());
                LeftNeighbor leftNeighbor = App.leftTopicNeighbours.get(topic.getUUID());
                multicastPublisher.sendMessage(new UnsubscriptionMessage(topic, rightNeighbor, leftNeighbor,
                        "Unsubscribed", localHostAdress.getHostAddress()));
            } else if ("topic print".equals(line)) {
                System.out.println("Received Topics' name: ");
                Iterator<Topic> it = topics.iterator();
                while (it.hasNext()) {
                    System.out.println(it.next().getName());
                }
            } else if ("message add".equals(line)) {
                if (checkTopics() == false) {
                    continue;
                }

                System.out.println("To which topic do you want to add a message?");
                Topic topic = topicCliChooser(true);

                if (topic == null) {
                    continue;
                }

                System.out.println("Add a new message to topic " + topic.getName() + ":");
                System.out.print("Messages' name: ");
                String messageName = getLine();
                System.out.print("Messages' content: ");
                String messageContent = getLine(); // maybe content should be more as one line ;-)
                Message message = new Message(topic, messageName, messageContent);
                multicastPublisher.sendMessage(message);
            } else if ("message print".equals(line)) {
                if (checkTopics() == false) {
                    continue;
                }

                Topic topic = topicCliChooser(true);

                if (topic == null) {
                    continue;
                }

                System.out.println("The " + messages.size() + " messages to topic " + topic.getName() + " are:");
                Iterator<Message> it = messages.iterator();
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

    private static void InitHeartbeat(MulticastPublisher multicastPublisher, Topic topic) {
        // Create seperate heartbeat for each topic
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    RightNeighbor rightNeighbor = rightTopicNeighbours.get(topic.getUUID());
                    if (rightNeighbor != null && rightNeighbor.getIPAdress()
                            .isReachable(Settings.getInstance().getNodeTimeout()) == false) {
                        Socket s = new Socket("www.google.com", 80);
                        InetAddress localHostAdress = s.getLocalAddress();
                        s.close();
                        // neighbor is not reachable after 5 seconds
                        multicastPublisher.sendMessage(new HearbeatFailedMessage(topic, rightNeighbor, "HearbeatFailed",
                                localHostAdress.getHostAddress()));
                    }
                } catch (IOException e) {
                    System.out.println("A problem occured while executing hearbeat for topic: " + topic.getName());
                    e.printStackTrace();
                }
            }
        }, Settings.getInstance().getHearbeatDelay(), Settings.getInstance().getHearbeatPeriod());
    }

    private static boolean existsTopicWithName(String name) {
        if (name != null) {
            for (Topic topic : topics) {
                if (topic.getName().equals(name)) {
                    return true;
                }
            }
        }

        return false;
    }

    private static String getLine() throws IOException {
        BufferedReader buffer = new BufferedReader(new InputStreamReader(System.in));
        return buffer.readLine();
    }

    private static boolean checkTopics() {
        if (topics.size() < 1) {
            System.out.println("No topics available. Please add topic before...");
            return false;
        }
        if (subscribedTopics.size() < 1) {
            System.out.println(
                    "You are not subscribed to any available topic. Please subsrcibe to a topic before adding a message.");
            return false;
        }

        return true;
    }

    private static Topic topicCliChooser(Boolean chooseFromSubscribed) throws IOException {
        List<Topic> lstTopics;

        if (chooseFromSubscribed) {
            lstTopics = subscribedTopics;
        } else {
            lstTopics = topics;
        }

        if (lstTopics.isEmpty()) {
            return null;
        }

        System.out.println("-1: exit selection");

        for (Topic element : lstTopics) {
            int index = lstTopics.indexOf(element);
            System.out.println(index + ": " + element.getName());
        }

        String indexInput = getLine();
        if ("-1".equals(indexInput)) {
            return null;
        }

        try {
            Topic topic = lstTopics.get(Integer.valueOf(indexInput));
            return topic;
        } catch (Exception e) {
            System.out.println("Your selection was no good. Please try again...");
            return topicCliChooser(chooseFromSubscribed);
        }
    }
}
