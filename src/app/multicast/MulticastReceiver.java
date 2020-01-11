package app.multicast;

import java.io.*;
import java.net.*;
import java.util.HashMap;

import app.App;
import app.Settings;
import app.models.HoldbackQueueItem;
import app.models.Message;
import app.models.Request;
import app.models.RetransmissionRequest;
import app.models.SubscriptionMessage;
import app.models.Topic;

public class MulticastReceiver extends Thread {
    protected MulticastSocket socket = null;
    protected byte[] buf = new byte[4096]; // maybe a little bit high, but memory is cheap :-)

    // Key (Sender) - Value (Request)
    // As each request holds an sender specific increment, missing requests can be
    // detected by inspecting the hashmap
    private HashMap<String, HoldbackQueueItem> holdbackQueue;

    public MulticastReceiver() {
        holdbackQueue = new HashMap<>();
    }

    public void run() {
        try {
            Settings settings = Settings.getInstance();
            socket = new MulticastSocket(settings.getPort());
            InetAddress group = InetAddress.getByName(settings.getMulticastAddress());
            socket.joinGroup(group);
            while (true) {
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);
                // Get the sender. Used as key in holdbackQueue. is ip address of the sender
                String sender = ((InetSocketAddress) packet.getSocketAddress()).getAddress().getHostAddress();
                System.out.println(sender);

                // Get actual sent data
                byte[] receivedData = packet.getData();

                // Try to deserialize an Object from sent data
                Object object = deserialize(sender, receivedData);

                if (object == null) {
                    continue;
                }

                // "End" topic
                if (object instanceof Topic && ((Topic) object).getName().equals("end")) {
                    System.out.println("Received end topic. Ready to die now...");
                    break;
                }

                // Print ordinary topic
                if (object instanceof Topic) {
                    Topic topic = (Topic) object;
                    System.out.println("Received Topic: " + topic.getName());
                    App.topics.add(topic);
                }

                // Print message
                if (object instanceof Message) {                    
                    Message message = (Message) object;
                    if (object instanceof SubscriptionMessage){
                        System.out.println(
                            "Received SubscriptionMessage from Sender: " + message.getContent() + " to topic " + message.getTopic().getName());
                    } else {
                        System.out.println(
                                "Received Message: " + message.getName() + " with topic " + message.getTopic().getName());
                        if (App.subscribedTopics.contains(message.getTopic())){
                            App.messages.add(message); // Add message only if current instance has subscribed to that topic
                        }
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

    private Object deserialize(String sender, byte[] receivedData) {
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(receivedData);
            ObjectInputStream ois = new ObjectInputStream(bais);
            Object object = ois.readObject();
            ois.close();
            bais.close();
            if (object instanceof Request) {
                Request request = (Request) object;
                // Get sequence id id request
                int sequenceId = request.getSequenceId();
                System.out.println("Received seq id: " + sequenceId);

                // save to holdback queue
                HoldbackQueueItem holdbackQueueItem = holdbackQueue.get(sender);
                // if holdbackQueueItem is null, initialize a new one
                if (holdbackQueueItem == null) {
                    holdbackQueueItem = new HoldbackQueueItem();
                    holdbackQueue.put(sender, holdbackQueueItem);
                }

                if (sequenceId == this.getHighestDeliveredSequenceNumber(sender) + 1) {
                    // Deliver request
                    // System.out.println("deliver");
                    return request.getPayload();
                } else if (sequenceId > this.getHighestDeliveredSequenceNumber(sender) + 1) {
                    // Save to queue list
                    holdbackQueueItem.getReceiveRequests().add(request);

                    RetransmissionRequest retransmissionRequest = new RetransmissionRequest(
                            this.getHighestDeliveredSequenceNumber(sender) + 1);

                    MulticastPublisher multicastPublisher = MulticastPublisher.getInstance();
                    // Test
                    multicastPublisher.unicastRetransmissionRequest(sender, retransmissionRequest);
                    return null;
                } else {
                    // old request retransmitted
                    // just ignore....
                }
                return null;
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
            return object;
        } catch (IOException e) {
            System.out.println("IOException while deserzialize: " + e);
        } catch (ClassNotFoundException e) {
            System.out.println("ClassNotFoundException while deserzialize: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.out.println("Received string was not base64 encoded: " + e.getMessage());
        }
        // Return null if there was an exception
        return null;
    }

    private int getHighestDeliveredSequenceNumber(String sender) {
        // If no queueitem for sender is there, return 0 as highest seq id.
        if (holdbackQueue.containsKey(sender) == false) {
            return 0;
        }

        HoldbackQueueItem holdbackQueueItem = holdbackQueue.get(sender);
        return holdbackQueueItem.getSequenceId();
    }
}