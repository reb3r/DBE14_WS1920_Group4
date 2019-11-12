package app.multicast;

import java.io.*;
import java.net.*;
import java.util.Base64;

import app.Settings;
import app.models.Topic;

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
                // If it is not the end, it "must" be an serialized object
                // Todo: Dont throw exceptions when is is not an serialized object
                deserialize(received);
            }
            socket.leaveGroup(group);
        } catch (Exception e) {
            System.out.println("hh" + e);
        } finally {
            socket.close();
        }
    }

    private void deserialize(String received) {
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(Base64.getDecoder().decode(received));
            ObjectInputStream ois = new ObjectInputStream(bais);
            Object object = ois.readObject();
            ois.close();
            bais.close();

            if (object instanceof Topic) {
                Topic topic = (Topic) object;
                System.out.println("Received Topic: " + topic.getName());
            }

        } catch (IOException e) {
            System.out.println("IOException while deserzialize: " + e.getMessage());
        } catch (ClassNotFoundException e) {
            System.out.println("ClassNotFoundException while deserzialize: " + e.getMessage());
        }
    }
}