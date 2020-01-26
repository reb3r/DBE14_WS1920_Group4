package app;

import java.net.InetAddress;
import java.net.Socket;
import java.util.UUID;
import app.models.TopicNodeMessage;
import app.models.Leader;
import app.models.Topic;
import app.multicast.MulticastPublisher;

public class TopicNode {
    private final UUID uuid;
    private UUID leader_uuid = null;

    private Topic topic;

    public TopicNode(Topic topic) {  
        this.topic = topic;
        this.uuid = UUID.randomUUID();
    }

    public void runLCR (TopicNodeMessage msg, MulticastPublisher multicastPublisher) {
        try {
            Topic topic = msg.getTopic();
            String rightNeighborInetAdress = App.rightTopicNeighbours.get(topic.getUUID()).getIPAdress().getHostAddress();

            if (msg.getIsLeader()){
                leader_uuid = msg.getUUID();
                if (msg.getUUID().equals(this.uuid) == false){
                    multicastPublisher.sendMessageUnicast(rightNeighborInetAdress, new TopicNodeMessage(topic, leader_uuid, true));
                }
                
                // Winner tells other nodes that its the new leader
                Socket s = new Socket("www.google.com", 80);
                InetAddress localHostAdress = s.getLocalAddress();
                s.close();
                
                Leader leader = new Leader(localHostAdress);
                topic.setLeader(leader);                
                multicastPublisher.announceTopic(topic);

                return;
            }

            int compareResult = msg.getUUID().compareTo(this.uuid);
            if (compareResult == 1) {
                //id from message is higher, forward message
                multicastPublisher.sendMessageUnicast(rightNeighborInetAdress, msg);
            }
            else if (compareResult == 0) {
                //received own election message. Anounce self as leader.
                leader_uuid = this.uuid;
                multicastPublisher.sendMessageUnicast(rightNeighborInetAdress, new TopicNodeMessage(topic, this.uuid, true));
            }            
        } catch (Exception e) {            
            System.out.println("Unknown Exception while processing node message data: " + e.getMessage());
        }
    }

    public UUID getUUID() {
        return this.uuid;
    }

    public Topic getTopic() {
        return this.topic;
    }
}