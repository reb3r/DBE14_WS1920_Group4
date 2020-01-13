package app.models;

import java.io.Serializable;
import java.net.InetAddress;

public class RightNeighbor implements Serializable {
    /**
     * Serial for serialization
     */
    private static final long serialVersionUID = 288908798234831333L;

    /**
     * IP adress of right neighbor
     */
    private InetAddress ipAdress;

    public RightNeighbor(InetAddress ipAdress) {
        this.ipAdress = ipAdress;
    }

    public InetAddress getIPAdress() {
        return this.ipAdress;
    }

    public void setIPAdress(InetAddress ipAdress) {
        this.ipAdress = ipAdress;
    }
}