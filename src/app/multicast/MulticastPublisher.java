package app.multicast;

import java.io.*;
import java.net.*;
import java.util.*;

import app.Settings;
import app.models.Topic;

public class MulticastPublisher {
    private DatagramSocket socket;
    private InetAddress group;
    private byte[] buf;

    public void multicast(String multicastMessage) throws IOException {
        Settings settings = Settings.getInstance();
        socket = new DatagramSocket();
        group = InetAddress.getByName(settings.getMulticastAddress());
        buf = multicastMessage.getBytes();

        DatagramPacket packet = new DatagramPacket(buf, buf.length, group, settings.getPort());
        socket.send(packet);
        socket.close();
    }

    /**
     * Send Topic object as an base64 encoded serialized string over multicast
     */
    public void announceTopic(Topic topic) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(baos);
        out.writeObject(topic);
        this.multicast(Base64.getEncoder().encodeToString(baos.toByteArray()));
        out.close();
        baos.close();
    }
}