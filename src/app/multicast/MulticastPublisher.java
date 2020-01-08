package app.multicast;

import java.io.*;
import java.net.*;
import java.util.LinkedList;
import java.util.List;

import app.Settings;
import app.models.Topic;
import app.models.Message;
import app.models.Request;

public class MulticastPublisher {
    private DatagramSocket socket;
    private InetAddress group;

    // Sequence number of the last sent request
    private int sequenceId;
    // List of all sent requests. Needed for later retransmissions...
    private List<Request> sentRequests;

    public MulticastPublisher(String uuid) {
        // on the startup, the list is initialized empty and the sequence id is 0
        sentRequests = new LinkedList<>();
        sequenceId = 0;
    }

    /**
     * sends data over the wire with multicasts
     * 
     * @param buf bytearray of to be sent data
     * @throws IOException
     */
    public void multicast(byte[] buf) throws IOException {
        Settings settings = Settings.getInstance();
        socket = new DatagramSocket();
        group = InetAddress.getByName(settings.getMulticastAddress());
        DatagramPacket packet = new DatagramPacket(buf, buf.length, group, settings.getPort());
        socket.send(packet);
        socket.close();
    }

    /**
     * Serializes an Object, packs it into an Request, give the request a sequence
     * number and save it to a linked list
     * 
     * @param object
     * @throws IOException
     */
    public void multicastObject(Object object) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(baos);

        // Increment sequenceId by one
        sequenceId = sequenceId + 1;
        Request request = new Request(object, sequenceId);
        System.out.println(1);

        out.writeObject(request);
        this.multicast(baos.toByteArray());
        out.close();
        baos.close();
        System.out.println(2);
        // Add request to sentRequests List
        sentRequests.add(request);
    }

    /**
     * Send Topic object as an base64 encoded serialized string over multicast
     */
    public void announceTopic(Topic topic) throws IOException {
        this.multicastObject(topic);
    }

    /**
     * Send Message object as an base64 encoded serialized string over multicast
     */
    public void sendMessage(Message message) throws IOException {
        this.multicastObject(message);
    }

    // TODO: Receive retransmission request and retransmit message
}
