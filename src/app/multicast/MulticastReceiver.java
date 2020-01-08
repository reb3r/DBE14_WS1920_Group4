package app.multicast;

import java.io.*;
import java.net.*;
import java.util.HashMap;

import app.App;
import app.Settings;
import app.models.HoldbackQueueItem;
import app.models.Message;
import app.models.Request;
import app.models.Topic;

public class MulticastReceiver extends Thread {
    protected MulticastSocket socket = null;
    protected byte[] buf = new byte[4096]; // maybe a little bit high, but memory is cheap :-)

    private String uuid;

    // Key (Sender) - Value (Request)
    // As each request holds an sender specific increment, missing requests can be
    // detected by inspecting the hashmap
    private HashMap<String, HoldbackQueueItem> holdbackQueue;

    public MulticastReceiver(String uuid) {
        this.uuid = uuid;
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
                // Get the sender. Used as key in holdbackQueue.
                String sender = ((InetSocketAddress) packet.getSocketAddress()).getAddress().toString();
                ;// socket.getRemoteSocketAddress().toString();
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
                    System.out.println(
                            "Received Message: " + message.getName() + " with topic " + message.getTopic().getName());
                    App.messages.add(message);
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
            System.out.println(14);
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

                    // TODO: Request new transmit of request
                    return null;
                } else {
                    // old request retransmitted
                    // just ignore....
                }
                return null;
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