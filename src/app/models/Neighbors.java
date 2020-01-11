package app.models;

import java.net.InetAddress;

public class Neighbors {
    /**
     * IP adress of left neighbor
     */
    private InetAddress ipAdressLeftNeighbor;

    /**
     * IP adress of right neighbor
     */
    private InetAddress ipAdressRightNeighbor;

    public Neighbors(InetAddress ipAdressLeftNeighbor, InetAddress ipAdressRightNeighbor) {
        this.ipAdressLeftNeighbor = ipAdressLeftNeighbor;
        this.ipAdressRightNeighbor = ipAdressRightNeighbor;
    }

    public InetAddress getIPAdressLeftNeighbor() {
        return this.ipAdressLeftNeighbor;
    }

    public void setIPAdressLeftNeighbor(InetAddress ipAdressLeftNeighbor) {
        this.ipAdressLeftNeighbor = ipAdressLeftNeighbor;
    }

    public InetAddress getIPAdressRightNeighbor() {
        return this.ipAdressRightNeighbor;
    }

    public void setIPAdressRightNeighbor(InetAddress ipAdressRightNeighbor) {
        this.ipAdressRightNeighbor = ipAdressRightNeighbor;
    }
}