package app.models;

import java.io.Serializable;
import java.net.InetAddress;

public class LeftNeighbor implements Serializable {
    /**
     * Serial for serialization
     */
    private static final long serialVersionUID = -2670759778432936760L;
    
    /**
     * IP adress of left neighbor
     */
    private InetAddress ipAdress;

    public LeftNeighbor(InetAddress ipAdress) {
        this.ipAdress = ipAdress;
    }

    public InetAddress getIPAdress() {
        return this.ipAdress;
    }

    public void setIPAdress(InetAddress ipAdress) {
        this.ipAdress = ipAdress;
    }
}