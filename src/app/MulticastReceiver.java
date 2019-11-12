package app;

import java.io.*;
import java.net.*;

public class MulticastReceiver extends Thread {
    protected MulticastSocket socket = null;
    protected byte[] buf = new byte[256];

    public void run() {
        try {
            Settings settings = Settings.getInstance();
            socket = new MulticastSocket(settings.getPort());
            InetAddress group = InetAddress.getByName(settings.getMulticastAddress());
            socket.joinGroup(group);
            while (true) {
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);
                String received = new String(packet.getData(), 0, packet.getLength());
                System.out.println("Received over multicast: " + received);
                if ("end".equals(received)) {
                    break;
                }
            }
            socket.leaveGroup(group);
        } catch (Exception e) {
            System.out.println(e);
        } finally {
            socket.close();
        }
    }
}