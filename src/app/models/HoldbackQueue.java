package app.models;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import app.multicast.MulticastPublisher;

public class HoldbackQueue {

    // Key (Sender) - Value (Request)
    // As each request holds an sender specific increment, missing requests can be
    // detected by inspecting the hashmap
    private Map<String, HoldbackQueueItem> holdbackQueue;

    public HoldbackQueue() {
        holdbackQueue = new HashMap<>();
    }

    public void push(String sender, Request request) throws IOException {
        int sequenceId = request.getSequenceId();
        System.out.println("Received seq id: " + sequenceId);

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
                // TODO: 5 times and wait 30 seconds in between
                this.requestRequestRetransmission(first.getSender().getHostAddress(),
                        highestDeliveredSequenceNumber + 1);
            }
        } else {
            // Just ignore request which where already delivered
        }
    }

    public boolean hasMoreDeliverables(String sender) {
        // If no queueitem for sender is there, return 0 as highest seq id.
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
        }

        // If there are more requests in the holdback queue, check if there the next is
        // missing. If it is so, request the retransmission
        if (requests.size() > 0 && requests.get(0).getSequenceId() != item.getSequenceId() + 1) {
            // TODO: 5 times and wait 30 seconds in between
            this.requestRequestRetransmission(sender, item.getSequenceId() + 1);
        }

        return deliverableRequest;
    }

    private int getHighestDeliveredSequenceNumber(String sender) {
        // If no queueitem for sender is there, return 0 as highest seq id.
        if (holdbackQueue.containsKey(sender) == false) {
            return 0;
        }

        HoldbackQueueItem holdbackQueueItem = holdbackQueue.get(sender);
        return holdbackQueueItem.getSequenceId();
    }

    private void requestRequestRetransmission(String sender, int sequenceId) throws IOException {
        RetransmissionRequest retransmissionRequest = new RetransmissionRequest(
                this.getHighestDeliveredSequenceNumber(sender) + 1);

        MulticastPublisher multicastPublisher = MulticastPublisher.getInstance();
        multicastPublisher.unicastRetransmissionRequest(sender, retransmissionRequest);
    }

}