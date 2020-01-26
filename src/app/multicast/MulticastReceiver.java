package app.multicast;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;

import app.App;
import app.IPAdress;
import app.Log;
import app.Settings;
import app.TopicNode;
import app.models.HearbeatFailedMessage;
import app.models.HoldbackQueue;
import app.models.LeftNeighbor;
import app.models.Message;
import app.models.TopicNodeMessage;
import app.models.UnsubscriptionMessage;
import app.models.RightNeighbor;
import app.models.RingCompleteMessage;
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
                Log.debug("Received data from: " + sender.getHostAddress());

                // Get actual sent data
                byte[] receivedData = packet.getData();

                // Try to deserialize an Object from sent data
                String senderUuid = deserialize(sender, receivedData);

                // While there are requests in the queue which are deliverable, do processing.
                while (this.holdbackQueue.hasMoreDeliverables(senderUuid)) {
                    Request request = this.holdbackQueue.deliver(senderUuid);
                    Object object = request.getPayload();

                    // "End" topic
                    if (object instanceof Topic && ((Topic) object).getName().equals("end")) {
                        Log.debug("Received end topic. Ready to die now...");
                        break threadloop;
                    }

                    // Process topic object
                    if (object instanceof Topic) {
                        Topic topic = (Topic) object;
                        System.out.println("Received Topic: " + topic.getName() + " in state " + topic.getState());

                        App.topics.removeIf(t -> t.equals(topic));

                        for (Topic top : App.subscribedTopics) {
                            if (top.equals(topic)) {
                                top.setName(topic.getName());
                            }
                        }

                        if ("ACTIVE".equals(topic.getState())){
                            App.topics.add(topic);
                        } else {
                            App.subscribedTopics.remove(topic);            
                            App.rightTopicNeighbours.remove(topic.getUUID());
                            App.topicNodes.remove(topic.getUUID());
                        }
                    }

                    // Process message
                    if (object instanceof Message) {
                        Message message = (Message) object;
                        if (object instanceof SubscriptionMessage) {
                            Topic topic = message.getTopic();
                            InetAddress localHostAdress = IPAdress.getLocalIPAddress();

                            if (topic.getLeader().getIPAdress().equals(localHostAdress)) {
                                System.out.println("Leader received SubscriptionMessage from sender: "
                                        + message.getContent() + " to topic " + topic.getName());

                                InetAddress subscriberAdress = InetAddress.getByName(message.getContent());
                                String subscriberAdressString = subscriberAdress.getHostAddress();

                                RightNeighbor myFormerRightNeighbor = App.rightTopicNeighbours.replace(topic.getUUID(), new RightNeighbor(subscriberAdress));

                                // Tell new node how to join the ring
                                if (myFormerRightNeighbor != null) {
                                    multicastPublisher.sendTopicNeighborUnicast(subscriberAdressString,
                                            new TopicNeighbor(topic, myFormerRightNeighbor, new LeftNeighbor(localHostAdress)));

                                    multicastPublisher.sendTopicNeighborUnicast(myFormerRightNeighbor.getIPAdress().getHostAddress(),
                                            new TopicNeighbor(topic, new RightNeighbor(null), new LeftNeighbor(subscriberAdress)));
                                } else {
                                    App.leftTopicNeighbours.put(topic.getUUID(), new LeftNeighbor(subscriberAdress));

                                    multicastPublisher.sendTopicNeighborUnicast(subscriberAdressString,
                                            new TopicNeighbor(topic, new RightNeighbor(localHostAdress), new LeftNeighbor(localHostAdress)));
                                }
                            }
                        } else if (object instanceof UnsubscriptionMessage) {
                            UnsubscriptionMessage unsubscriptionMessage = (UnsubscriptionMessage) message;
                            Topic topic = unsubscriptionMessage.getTopic();
                            InetAddress localHostAdress = IPAdress.getLocalIPAddress();

                            if (topic.getLeader().getIPAdress().equals(localHostAdress)) {
                                System.out.println("Leader received UnsubscriptionMessage from sender: "
                                        + unsubscriptionMessage.getContent() + " to topic " + topic.getName());

                                InetAddress unsubscriberAdress = InetAddress.getByName(unsubscriptionMessage.getContent());
                                
                                RightNeighbor myRightNeighbor = App.rightTopicNeighbours.get(topic.getUUID());

                                if (unsubscriberAdress.equals(localHostAdress) && myRightNeighbor == null ) {
                                    // last node leaves topic
                                    App.topics.remove(topic);
                                    continue;
                                }
                                
                                RightNeighbor rightNeighborOfUnsubscriber = unsubscriptionMessage.getRightNeighbor();
                                LeftNeighbor leftNeighborOfUnsubscriber = unsubscriptionMessage.getLeftNeighbor();

                                if (rightNeighborOfUnsubscriber.getIPAdress().equals(leftNeighborOfUnsubscriber.getIPAdress())) {                                    
                                    // When only two nodes are left and one leaves, the last node will receive null as new neighbors.
                                    multicastPublisher.sendTopicNeighborUnicast(rightNeighborOfUnsubscriber.getIPAdress().getHostAddress(),
                                    new TopicNeighbor(topic, null, null));
                                } else {
                                    multicastPublisher.sendTopicNeighborUnicast(rightNeighborOfUnsubscriber.getIPAdress().getHostAddress(),
                                        new TopicNeighbor(topic, new RightNeighbor(null), leftNeighborOfUnsubscriber));

                                    multicastPublisher.sendTopicNeighborUnicast(leftNeighborOfUnsubscriber.getIPAdress().getHostAddress(),
                                        new TopicNeighbor(topic, rightNeighborOfUnsubscriber, new LeftNeighbor(null)));
                                }                             
                            }
                        } else if (object instanceof HearbeatFailedMessage) {
                            HearbeatFailedMessage hearbeatFailedMessage = (HearbeatFailedMessage) object;
                            Topic topic = hearbeatFailedMessage.getTopic();          
                            
                            InetAddress heartbeatSenderAdress = InetAddress.getByName(hearbeatFailedMessage.getContent());                            
                            InetAddress failedNeighborAdress = hearbeatFailedMessage.getMissingRightNeighbor().getIPAdress();
                            
                            LeftNeighbor currentLeftNeighbor = App.leftTopicNeighbours.get(topic.getUUID());

                            if (currentLeftNeighbor.getIPAdress().equals(failedNeighborAdress)) {
                                App.leftTopicNeighbours.replace(topic.getUUID(), currentLeftNeighbor, new LeftNeighbor(heartbeatSenderAdress));

                                multicastPublisher.sendTopicNeighborUnicast(heartbeatSenderAdress.getHostAddress(),
                                        new TopicNeighbor(topic, new RightNeighbor(IPAdress.getLocalIPAddress()), new LeftNeighbor(null)));
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

                    // Set neighbor for specific topic
                    if (object instanceof TopicNeighbor) {
                        TopicNeighbor topicNeighbor = (TopicNeighbor) object;

                        RightNeighbor rightNeighbor = topicNeighbor.getRightNeighbor();
                        LeftNeighbor leftNeighbor = topicNeighbor.getLeftNeighbor();

                        if (rightNeighbor != null) {
                            if (rightNeighbor.getIPAdress() != null) {
                                App.rightTopicNeighbours.put(topicNeighbor.getTopic().getUUID(), topicNeighbor.getRightNeighbor());
                            }
                        } else {                            
                            App.rightTopicNeighbours.put(topicNeighbor.getTopic().getUUID(), null);
                        }

                        if (leftNeighbor != null) {
                            if (leftNeighbor.getIPAdress() != null) {
                                App.leftTopicNeighbours.put(topicNeighbor.getTopic().getUUID(), topicNeighbor.getLeftNeighbor());
                            }
                        } else {                            
                            App.leftTopicNeighbours.put(topicNeighbor.getTopic().getUUID(), null);
                        }

                        // notify all nodes that ring is complete
                        multicastPublisher.sendMessage(new RingCompleteMessage(topicNeighbor.getTopic()));
                    }

                    // Process ring complete message
                    if (object instanceof RingCompleteMessage) {
                        // Received message that ring building is completed
                        RingCompleteMessage ringMessage = (RingCompleteMessage) object;
                        Topic topic = ringMessage.getTopic();                        

                        // Create new topicNode for leader election purposes. If node already exists, put will return existing one.
                        // Put will return null if put action was successful.
                        TopicNode topicNode = App.topicNodes.put(topic.getUUID(), new TopicNode(topic));

                        if (topicNode == null) {
                            topicNode = App.topicNodes.get(topic.getUUID());
                        }                      

                        // Start leader election. Send message to right neighbor.
                        TopicNodeMessage nodeMessage = new TopicNodeMessage(topic, topicNode.getUUID(), false);
                        multicastPublisher.sendMessageUnicast(App.rightTopicNeighbours.get(topic.getUUID()).getIPAdress().getHostAddress(), nodeMessage);
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

    private String deserialize(InetAddress sender, byte[] receivedData) {
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
                this.holdbackQueue.push(request.getSenderUuid().toString(), request);
                return request.getSenderUuid().toString();
            } else if (object instanceof RetransmissionRequest) {
                // Get missing sequenceId from RetransmissionRequest
                int sequenceId = ((RetransmissionRequest) object).getSequenceId();

                Log.debug("Received RetransmissionRequest for seqId " + sequenceId);

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
        return "";
    }
}