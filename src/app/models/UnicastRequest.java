package app.models;

import java.net.InetAddress;
import java.util.UUID;

public class UnicastRequest extends Request {

    private static final long serialVersionUID = -22213242L;

    public UnicastRequest(Object payload, int sequenceId) {
        super(payload, sequenceId);
    }

    public UnicastRequest(Object payload, int sequenceId, InetAddress sender, UUID senderUuid) {
        super(payload, sequenceId, sender, senderUuid);
    }

}