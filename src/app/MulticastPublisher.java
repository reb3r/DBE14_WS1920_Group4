package app;

import java.io.*;
import java.net.*;

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
}