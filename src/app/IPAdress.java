package app;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class IPAdress {

    /**
     * This method is implemented to workaround the problem, that the local ip adress can not be determined correctly in some cases. See:
     * https://stackoverflow.com/questions/2381316/java-inetaddress-getlocalhost-returns-127-0-0-1-how-to-get-real-ip
     */
    public static InetAddress getLocalIPAddress() {
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();

            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface ni = networkInterfaces.nextElement();
                Enumeration<InetAddress> nias = ni.getInetAddresses();
                while(nias.hasMoreElements()) {
                    InetAddress address= nias.nextElement();
                    if (!address.isLinkLocalAddress() 
                     && !address.isLoopbackAddress()
                     && address instanceof Inet4Address) {
                        return address;
                    }
                }
            }
        } catch (SocketException e) {
            Log.debug("Unable to IP of local host" + e.getMessage());
        }

        return null;
    }
}