package app.models;

import java.util.LinkedList;
import java.util.List;

/**
 * Simple datastructure to hold the actual queue implemented as lists and the
 * highest delivered sequence number
 */
public class HoldbackQueueItem {

    /*
     * Holds the highest sequence number for received AND delivered requests
     */
    private int sequenceId;

    /*
     * Holds all request which are received but not delivered. delivery only should
     * be done if there are no "sequence holes" like in (2,3,5)
     */
    private List<Request> receivedRequests;

    public HoldbackQueueItem() {
        // call the more elaborated constructor to actually set attributes
        this(0, new LinkedList<>());
    }

    public HoldbackQueueItem(int sequenceId, List<Request> receivedRequests) {
        this.sequenceId = sequenceId;
        this.receivedRequests = receivedRequests;
    }

    public int getSequenceId() {
        return sequenceId;
    }

    public void setSequenceId(int sequenceId) {
        this.sequenceId = sequenceId;
    }

    public List<Request> getReceiveRequests() {
        return receivedRequests;
    }
}