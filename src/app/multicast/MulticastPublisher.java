package app.multicast;

import java.io.*;
import java.net.*;

import app.App;
import app.Settings;
import app.models.Topic;
import app.models.VectorClock;
import app.models.Message;
import app.models.Request;

public class MulticastPublisher {
    private DatagramSocket socket;
    private InetAddress group;

    private String uuid;

    public MulticastPublisher(String uuid) {
        this.uuid = uuid;
    }

    public void multicast(byte[] buf) throws IOException {
        Settings settings = Settings.getInstance();
        socket = new DatagramSocket();
        group = InetAddress.getByName(settings.getMulticastAddress());
        DatagramPacket packet = new DatagramPacket(buf, buf.length, group, settings.getPort());
        socket.send(packet);
        socket.close();
    }

    public void multicastObject(Object object) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(baos);

        VectorClock vectorClock = App.vectorClock;
        vectorClock.addOneTo(this.uuid);
        System.out.println("Send Object. VectorClok is now: " + vectorClock.toString());
        Request request = new Request(object, (VectorClock) vectorClock.Clone());

        out.writeObject(request);
        this.multicast(baos.toByteArray());
        out.close();
        baos.close();
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
}
