package app.models;

import java.io.Serializable;
import java.net.InetAddress;

public class Leader implements Serializable{
    /**
     * Serial for serialization
     */
    private static final long serialVersionUID = 681545692437236558L;

    /**
     * Leaders IP adress
     */
    private InetAddress ipAdress;

    public Leader(InetAddress ipAdress) {
        this.ipAdress = ipAdress;
    }

    public InetAddress getIPAdress() {
        return this.ipAdress;
    }

    public void setIPAdress(InetAddress ipAdress) {
        this.ipAdress = ipAdress;
    }
}