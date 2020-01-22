package app.multicast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import app.Log;
import app.Settings;
import app.interfaces.MessageInterface;
import app.models.Topic;
import app.models.TopicNeighbor;
import app.models.Request;
import app.models.RetransmissionRequest;

public class MulticastPublisher {
    // Sequence number of the last sent request
    private int sequenceId;
    // List of all sent requests. Needed for later retransmissions...
    private List<Request> sentRequests;

    private UUID senderUuid;

    // SINGLETON-Pattern!
    // See: https://en.wikipedia.org/wiki/Singleton_pattern
    private static volatile MulticastPublisher instance;

    private MulticastPublisher() {
        // on the startup, the list is initialized empty and the sequence id is 0
        sentRequests = new LinkedList<>();
        sequenceId = 0;
        senderUuid = UUID.randomUUID();
    }

    // SINGLETON-Pattern!
    public static MulticastPublisher getInstance() {
        if (MulticastPublisher.instance == null) {
            synchronized (MulticastPublisher.class) {
                if (MulticastPublisher.instance == null) {
                    MulticastPublisher.instance = new MulticastPublisher();
                }
            }
        }
        return MulticastPublisher.instance;
    }

    /**
     * sends data over the wire with multicasts
     * 
     * @param buf bytearray of to be sent data
     * @throws IOException
     */
    private void multicast(byte[] buf) throws IOException {
        Settings settings = Settings.getInstance();
        DatagramSocket socket = new DatagramSocket();
        InetAddress group = InetAddress.getByName(settings.getMulticastAddress());
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
        request.setSenderUuid(this.senderUuid);

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
    public void sendMessage(MessageInterface message) throws IOException {
        this.multicastObject(message);
    }

    /**
     * Send Message object as an base64 encoded serialized string over unicast
     */
    public void sendMessageUnicast(String address, MessageInterface message) throws IOException {
        this.unicastObject(address, message);
    }

    public void unicast(String address, byte[] buf) throws IOException {
        Settings settings = Settings.getInstance();
        DatagramSocket socket = new DatagramSocket();
        InetAddress receiver = InetAddress.getByName(address); // Resolve DNS if hostname is given...
        DatagramPacket packet = new DatagramPacket(buf, buf.length, receiver, settings.getPort());
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
        request.setSenderUuid(this.senderUuid);

        out.writeObject(request);
        this.unicast(address, baos.toByteArray());
        out.close();
        baos.close();

        // Add request to sentRequests List
        sentRequests.add(request);
    }

    public void sendTopicNeighbor(String address, TopicNeighbor topicNeighbor) throws IOException {
        unicastObject(address, topicNeighbor);
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
                // Initalize streams vor serialization
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream out = new ObjectOutputStream(baos);
                // Serialize request and retransmit via multicast (eventually many receivers
                // missed the original message)
                Log.debug("Resend Request for seqId " + sequenceId);
                out.writeObject(request);
                this.multicast(baos.toByteArray());
                // Close streams
                out.close();
                baos.close();
                return true;
            }
        }
        Log.debug("Could not retransmit Request (not found!) for seqId " + sequenceId);
        return false;
    }
}
