package app.multicast;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;

import app.App;
import app.Settings;
import app.TopicNode;
import app.models.HoldbackQueue;
import app.models.Message;
import app.models.TopicNodeMessage;
import app.models.RightNeighbor;
import app.models.Request;
import app.models.RetransmissionRequest;
import app.models.SubscriptionMessage;
import app.models.Topic;
import app.models.TopicNeighbor;

public class MulticastReceiver extends Thread {
    private MulticastPublisher multicastPublisher;

    protected MulticastSocket socket = null;
    protected byte[] buf = new byte[16384]; // maybe a little bit high, but memory is cheap :-)

    protected HoldbackQueue holdbackQueue;

    private volatile boolean exit = false;

    public MulticastReceiver() {
        this.multicastPublisher = MulticastPublisher.getInstance();
        this.holdbackQueue = new HoldbackQueue();
    }

    // Should mark thread to stop
    public void stopThread() {
        exit = true;
    }

    public void run() {
        try {
            Settings settings = Settings.getInstance();
            socket = new MulticastSocket(settings.getPort());
            InetAddress group = InetAddress.getByName(settings.getMulticastAddress());
            socket.joinGroup(group);
            threadloop: while (exit == false) {
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);
                // Get the sender. Used as key in holdbackQueue. is ip address of the sender
                InetAddress sender = ((InetSocketAddress) packet.getSocketAddress()).getAddress();
                System.out.println("Sender of request: " + sender.getHostAddress());

                // Get actual sent data
                byte[] receivedData = packet.getData();

                // Try to deserialize an Object from sent data
                deserialize(sender, receivedData);

                // While there are requests in the queue which are deliverable, do processing.
                while (this.holdbackQueue.hasMoreDeliverables(sender.getHostAddress())) {
                    Request request = this.holdbackQueue.deliver(sender.getHostAddress());
                    Object object = request.getPayload();

                    // "End" topic
                    if (object instanceof Topic && ((Topic) object).getName().equals("end")) {
                        System.out.println("Received end topic. Ready to die now...");
                        break threadloop;
                    }

                    // Print ordinary topic
                    if (object instanceof Topic) {
                        Topic topic = (Topic) object;
                        System.out.println("Received Topic: " + topic.getName());

                        for (Topic top : App.topics) {
                            if (top.equals(topic)) {
                                App.topics.remove(top);
                                break;
                            }
                        }

                        App.topics.add(topic);
                    }

                    // Process message
                    if (object instanceof Message) {
                        Message message = (Message) object;
                        if (object instanceof SubscriptionMessage) {
                            Topic topic = message.getTopic();
                            InetAddress localHostAdress = InetAddress.getLocalHost();

                            // create new topicNode for leader election purposes
                            TopicNode topicNode = App.topicNodes.put(topic.getUUID(), new TopicNode(topic));

                            if (topicNode == null) {
                                topicNode = App.topicNodes.get(topic.getUUID());
                            }

                            if (topic.getLeader().getIPAdress().equals(localHostAdress)) {
                                System.out.println("Leader received SubscriptionMessage from sender: "
                                        + message.getContent() + " to topic " + topic.getName());

                                InetAddress subscriberAdress = InetAddress.getByName(message.getContent());
                                RightNeighbor rightNeighbor = App.topicNeighbours.get(topic.getUUID());

                                App.topicNeighbours.replace(topic.getUUID(), new RightNeighbor(subscriberAdress));

                                String subscriberAdressString = subscriberAdress.getHostAddress();
                                // Tell new node how to join the ring
                                if (rightNeighbor != null) {
                                    multicastPublisher.sendTopicNeighbor(subscriberAdressString,
                                            new TopicNeighbor(topic, rightNeighbor));
                                } else {
                                    multicastPublisher.sendTopicNeighbor(subscriberAdressString,
                                            new TopicNeighbor(topic, new RightNeighbor(localHostAdress)));
                                }

                                // Start leader election
                                TopicNodeMessage nodeMessage = new TopicNodeMessage(topic, topicNode.getUUID(), false);
                                multicastPublisher.sendMessageUnicast(subscriberAdressString, nodeMessage);
                            }
                        } else {
                            System.out.println("Received Message: " + message.getName() + " with topic "
                                    + message.getTopic().getName());
                            if (App.subscribedTopics.contains(message.getTopic())) {
                                App.messages.add(message); // Add message only if current instance has subscribed to
                                                           // that topic
                            }
                        }
                    }

                    // Process node message
                    if (object instanceof TopicNodeMessage) {
                        // Received message for leader election process
                        TopicNodeMessage nodeMessage = (TopicNodeMessage) object;
                        TopicNode topicNode = App.topicNodes.get(nodeMessage.getTopic().getUUID());

                        topicNode.runLCR(nodeMessage, multicastPublisher);
                    }

                    // Set new neighbor for specific topic
                    if (object instanceof TopicNeighbor) {
                        TopicNeighbor topicNeighbor = (TopicNeighbor) object;

                        App.topicNeighbours.put(topicNeighbor.getTopic().getUUID(), topicNeighbor.getNeighbor());
                    }
                }
            }
            socket.leaveGroup(group);
        } catch (Exception e) {
            System.out.println("Unknown Exception while receiving data: " + e.getMessage());
        } finally {
            socket.close();
        }
    }

    private void deserialize(InetAddress sender, byte[] receivedData) {
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(receivedData);
            ObjectInputStream ois = new ObjectInputStream(bais);
            Object object = ois.readObject();
            ois.close();
            bais.close();
            if (object instanceof Request) {
                Request request = (Request) object;
                request.setSender(sender);
                // push new message to the queue
                this.holdbackQueue.push(sender.getHostAddress(), request);
            } else if (object instanceof RetransmissionRequest) {
                // Get missing sequenceId from RetransmissionRequest
                int sequenceId = ((RetransmissionRequest) object).getSequenceId();

                System.out.println("Received RetransmissionRequest for seqId " + sequenceId);

                // Get instance from publisher
                MulticastPublisher multicastPublisher = MulticastPublisher.getInstance();
                // Arange retransmission with publisher. there SHOULD be more error handling for
                // faulty sequence ids
                multicastPublisher.retransmitRequest(sequenceId);
            }
        } catch (IOException e) {
            System.out.println("IOException while deserzialize: " + e);
        } catch (ClassNotFoundException e) {
            System.out.println("ClassNotFoundException while deserzialize: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.out.println("Received string was not base64 encoded: " + e.getMessage());
        }
    }
}