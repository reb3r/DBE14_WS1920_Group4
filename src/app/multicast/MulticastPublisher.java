package app.multicast;

import java.io.*;
import java.net.*;
import java.util.LinkedList;
import java.util.List;

import app.Settings;
import app.models.Topic;
import app.models.Message;
import app.models.Request;
import app.models.RetransmissionRequest;

public class MulticastPublisher {
    private DatagramSocket socket;
    private InetAddress group;

    // Sequence number of the last sent request
    private int sequenceId;
    // List of all sent requests. Needed for later retransmissions...
    private List<Request> sentRequests;

    // SINGLETON-Pattern!
    private static MulticastPublisher instance;

    private MulticastPublisher() {
        // on the startup, the list is initialized empty and the sequence id is 0
        sentRequests = new LinkedList<>();
        sequenceId = 0;
    }

    // SINGLETON-Pattern!
    public static MulticastPublisher getInstance() {
        if (MulticastPublisher.instance == null) {
            MulticastPublisher.instance = new MulticastPublisher();
        }
        return MulticastPublisher.instance;
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

        out.writeObject(request);
        this.multicast(baos.toByteArray());
        out.close();
        baos.close();
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

    public void unicast(String address, byte[] buf) throws IOException {
        Settings settings = Settings.getInstance();
        socket = new DatagramSocket();
        group = InetAddress.getByName(address); // Resolve DNS if hostname is given...
        DatagramPacket packet = new DatagramPacket(buf, buf.length, group, settings.getPort());
        socket.send(packet);
        socket.close();
    }

    /**
     * Should not be used. Only for testing...
     * 
     * @deprecated
     */
    public void unicastObject(String address, Object object) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(baos);

        // Increment sequenceId by one
        sequenceId = sequenceId + 1;
        Request request = new Request(object, sequenceId);

        out.writeObject(request);
        this.unicast(address, baos.toByteArray());
        out.close();
        baos.close();

        // Add request to sentRequests List
        sentRequests.add(request);
    }

    /**
     * Sends an RetransmissionRequest to the address given
     * 
     * @param address address of the original sender
     * @param object  the retransmissionrequest
     * @throws IOException
     */
    public void unicastRetransmissionRequest(String address, RetransmissionRequest object) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(baos);

        out.writeObject(object);
        this.unicast(address, baos.toByteArray());
        out.close();
        baos.close();
    }

    /**
     * Retransmits request (specified by sequenceId) to multicast
     * 
     * @param sequenceId sequenceId of the to be retransmitted request
     * @return true if request is retransmitted, false if request not found
     * @throws IOException
     */
    public boolean retransmitRequest(int sequenceId) throws IOException {
        for (Request request : sentRequests) {
            if (request.getSequenceId() == sequenceId) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream out = new ObjectOutputStream(baos);
                System.out.println("Resend Request for seqId " + sequenceId);
                out.writeObject(request);
                this.multicast(baos.toByteArray());
                out.close();
                baos.close();
                return true;
            }
        }
        System.out.println("Could not retransmit Request (not found!) for seqId " + sequenceId);
        return false;
    }
}
