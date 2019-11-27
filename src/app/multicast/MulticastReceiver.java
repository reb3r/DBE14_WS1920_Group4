package app.multicast;

import java.io.*;
import java.net.*;

import app.App;
import app.Settings;
import app.models.Message;
import app.models.Request;
import app.models.Topic;
import app.models.VectorClock;

public class MulticastReceiver extends Thread {
    protected MulticastSocket socket = null;
    protected byte[] buf = new byte[1024];

    private String uuid;

    public MulticastReceiver(String uuid) {
        this.uuid = uuid;
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
                byte[] received = packet.getData();
                Object object = deserialize(received);

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

                // Proint message
                if (object instanceof Message) {
                    Message message = (Message) object;
                    System.out.println(
                            "Received Message: " + message.getName() + " with topic " + message.getTopic().getName());
                }
            }
            socket.leaveGroup(group);
        } catch (Exception e) {
            System.out.println("Unknown Exception while receiving data: " + e.getMessage());
        } finally {
            socket.close();
        }
    }

    private Object deserialize(byte[] received) {
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(received);
            ObjectInputStream ois = new ObjectInputStream(bais);
            Object object = ois.readObject();
            ois.close();
            bais.close();

            if (object instanceof Request) {
                Request request = (Request) object;
                App.requests.add(request);
                VectorClock packedVectorClock = request.getVectorClock();
                System.out.println("My VectorClock is before: " + App.vectorClock.toString());
                System.out.println("Received VectorClock is " + packedVectorClock.toString());

                VectorClock merged = VectorClock.mergeClocks(packedVectorClock, App.vectorClock);
                merged.addOneTo(this.uuid);
                // System.out.println("Merged VectorClock is " + merged.toString());

                App.vectorClock = merged;
                System.out.println("My VectorClock is now: " + App.vectorClock.toString());
                return request.getPayload();
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
}