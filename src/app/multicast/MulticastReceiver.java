package app.multicast;

import java.io.*;
import java.net.*;

import app.App;
import app.Settings;
import app.models.HoldbackQueue;
import app.models.Message;
import app.models.RightNeighbor;
import app.models.Request;
import app.models.RetransmissionRequest;
import app.models.SubscriptionMessage;
import app.models.Topic;
import app.models.TopicNeighbor;

public class MulticastReceiver extends Thread {
    protected MulticastSocket socket = null;
    protected byte[] buf = new byte[16384]; // maybe a little bit high, but memory is cheap :-)

    protected HoldbackQueue holdbackQueue;

    public MulticastReceiver() {
        this.holdbackQueue = new HoldbackQueue();
    }

    public void run() {
        try {
            Settings settings = Settings.getInstance();
            socket = new MulticastSocket(settings.getPort());
            InetAddress group = InetAddress.getByName(settings.getMulticastAddress());
            socket.joinGroup(group);
            threadloop: while (true) {
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
                        App.topics.add(topic);
                    }

                    // Process message
                    if (object instanceof Message) {
                        Message message = (Message) object;
                        if (object instanceof SubscriptionMessage) {
                            Topic topic = message.getTopic();
                            InetAddress localHostAdress = InetAddress.getLocalHost();

                            if (topic.getLeader().getIPAdress() == localHostAdress) {
                                System.out.println("Leader received SubscriptionMessage from sender: " + message.getContent()
                                        + " to topic " + topic.getName());

                                InetAddress subscriberAdress = InetAddress.getByName(message.getContent());       
                                RightNeighbor rightNeighbor = App.topicNeighbours.get(topic.getUUID());
                                
                                App.topicNeighbours.replace(topic.getUUID(), new RightNeighbor(subscriberAdress));
                                MulticastPublisher multicastPublisher = MulticastPublisher.getInstance();

                                if (rightNeighbor != null) {
                                    multicastPublisher.sendTopicNeighbor(subscriberAdress.toString(), new TopicNeighbor(topic, rightNeighbor));                                    
                                }
                                else {
                                    multicastPublisher.sendTopicNeighbor(subscriberAdress.toString(), new TopicNeighbor(topic, new RightNeighbor(localHostAdress)));
                                }
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