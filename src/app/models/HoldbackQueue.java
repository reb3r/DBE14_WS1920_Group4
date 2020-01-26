package app.models;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import app.Log;
import app.multicast.MulticastPublisher;

/**
 * This class is the implementation of the holdback queue for many different
 * senders. The classname may be cofusing as the actuall queues per sender are
 * implemented in the HoldbackQueueItem
 */
public class HoldbackQueue {

    // Key (Sender) - Value (Request)
    // As each request holds an sender specific increment, missing requests can be
    // detected by inspecting the hashmap
    private Map<String, HoldbackQueueItem> holdbackQueue;

    public HoldbackQueue() {
        holdbackQueue = new HashMap<>();
    }

    /**
     * Method to fill up the sender specific queues with requests as they are
     * received
     * 
     * @param sender  identifier for the sender (src) of request
     * @param request the request which is received
     * @throws IOException
     */
    public void push(String sender, Request request) throws IOException {
        int sequenceId = request.getSequenceId();
        Log.debug("Received seq id: " + sequenceId + " in holdback queue push for sender " + sender);

        // save to holdback queue
        HoldbackQueueItem holdbackQueueItem = holdbackQueue.get(sender);
        // if holdbackQueueItem is null, initialize a new one
        if (holdbackQueueItem == null) {
            holdbackQueueItem = new HoldbackQueueItem();
            holdbackQueue.put(sender, holdbackQueueItem);
        }

        // only save the request, if it is NOT an already delivered one
        int highestDeliveredSequenceNumber = this.getHighestDeliveredSequenceNumber(sender);
        if (sequenceId > highestDeliveredSequenceNumber) {
            // Add new request to the list
            holdbackQueueItem.getReceiveRequests().add(request);
            // Sort to guarantuee the the lowest id is the first element
            Collections.sort(holdbackQueueItem.getReceiveRequests(), new SortRequestsBySeq());

            // If the highest seq. id + 1 is less than the first element of the ordered
            // list, there must be a hole. So request the message.
            Request first = holdbackQueueItem.getReceiveRequests().get(0);
            if (highestDeliveredSequenceNumber + 1 < first.getSequenceId()) {
                this.requestRequestRetransmission(first.getSender().getHostAddress());
            }
        } else {
            // Just ignore request which where already delivered
            Log.debug("Throw away request from " + sender + " with seq. id " + sequenceId + " (Highest seq. id was "
                    + highestDeliveredSequenceNumber + ")");
        }
    }

    /**
     * Indicates if there are more requests left which can be delivered
     * 
     * @param sender identifier for the sender (src) of request
     * @return true if there could be more requests delivered
     */
    public boolean hasMoreDeliverables(String sender) {
        // If no queueitem for sender is there, return false, as there cannot be
        // deliverables.
        if (holdbackQueue.containsKey(sender) == false) {
            return false;
        }
        HoldbackQueueItem item = this.holdbackQueue.get(sender);
        List<Request> requests = item.getReceiveRequests();

        // If list is zero, there are no deliverables for sure
        if (requests.size() == 0) {
            return false;
        }

        // Only if the NEXT request id is EXACTLY the highest delivered id + 1, there is
        // a way to deliver the request
        if (requests.get(0).getSequenceId() == item.getSequenceId() + 1) {
            return true;
        }

        // In all other cases there is no deliverable request
        return false;
    }

    /**
     * Pops the first element of the queue and returns it if the first message is
     * deliverable
     * 
     * @param sender identifier for the sender (src) of request
     * @return Request which can be delivered otherwise null is returned
     * @throws IOException
     */
    public Request deliver(String sender) throws IOException {
        // Abort method if there are no deliverable requests
        if (this.hasMoreDeliverables(sender) == false) {
            return null;
        }

        HoldbackQueueItem item = this.holdbackQueue.get(sender);
        List<Request> requests = item.getReceiveRequests();

        Request deliverableRequest = null;

        // Only if the NEXT request id is EXACTLY the highest delivered id + 1, there is
        // a way to deliver the request
        if (requests.get(0).getSequenceId() == item.getSequenceId() + 1) {
            deliverableRequest = requests.remove(0);
            // Set new highest delivered request id
            item.setSequenceId(deliverableRequest.getSequenceId());
            Log.debug("Deliver request with seq. id " + deliverableRequest.getSequenceId() + " from sender " + sender);
        }

        // If there are more requests in the holdback queue, check if there the next is
        // missing. If it is so, request the retransmission
        if (requests.size() > 0 && requests.get(0).getSequenceId() != item.getSequenceId() + 1) {
            this.requestRequestRetransmission(requests.get(0).getSender().getHostAddress());
        }

        return deliverableRequest;
    }

    /**
     * Helper method to get the highest delivered sequence number for a specific
     * sender
     * 
     * @param sender identifier for the sender (src) of request
     * @return the highest sequence number
     */
    private int getHighestDeliveredSequenceNumber(String sender) {
        // If no queueitem for sender is there, return 0 as highest seq id.
        if (holdbackQueue.containsKey(sender) == false) {
            return 0;
        }

        HoldbackQueueItem holdbackQueueItem = holdbackQueue.get(sender);
        return holdbackQueueItem.getSequenceId();
    }

    private void requestRequestRetransmission(String sender) throws IOException {
        RetransmissionRequest retransmissionRequest = new RetransmissionRequest(
                this.getHighestDeliveredSequenceNumber(sender) + 1);

        MulticastPublisher multicastPublisher = MulticastPublisher.getInstance();
        multicastPublisher.unicastRetransmissionRequest(sender, retransmissionRequest);
    }

}