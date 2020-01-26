package app.models;

import java.util.Comparator;

class SortRequestsBySeq implements Comparator<Request> {
    // Used for sorting in ascending order of
    // seuquence id
    public int compare(Request a, Request b) {
        return a.getSequenceId() - b.getSequenceId();
    }
}