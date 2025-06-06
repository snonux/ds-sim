package protocols.implementations;

import java.util.ArrayList;

import core.VSMessage;
import protocols.VSAbstractProtocol;

/**
 * The class VSBroadcastProtocol, an implementation of the broadcast
 * sturm protocol.
 *
 * @author Paul C. Buetow
 */
public class VSBroadcastProtocol extends VSAbstractProtocol {
    /** The sent messages. */
    private ArrayList<Integer> sentMessages;

    /** The broadcast count. */
    private static int broadcastCount;

    /**
     * Instantiates a new broadcast sturm protocol.
     */
    public VSBroadcastProtocol() {
        super(VSAbstractProtocol.HAS_ON_CLIENT_START);
        setClassname(getClass().toString());
        sentMessages = new ArrayList<Integer>();
    }

    /* (non-Javadoc)
     * @see events.VSAbstractProtocol#onClientInit()
     */
    public void onClientInit() {
    }

    /* (non-Javadoc)
     * @see protocols.VSAbstractProtocol#onClientReset()
     */
    public void onClientReset() {
    }

    /* (non-Javadoc)
     * @see protocols.VSAbstractProtocol#onClientStart()
     */
    public void onClientStart() {
        VSMessage message = new VSMessage();
        message.setInteger("Broadcast", broadcastCount++);
        sentMessages.add(message.getIntegerObj("Broadcast"));
        sendMessage(message);
    }

    /* (non-Javadoc)
     * @see protocols.VSAbstractProtocol#onClientRecv(core.VSMessage)
     */
    public void onClientRecv(VSMessage recvMessage) {
        onServerRecv(recvMessage);
    }

    /* (non-Javadoc)
     * @see protocols.VSAbstractProtocol#onClientSchedule()
     */
    public void onClientSchedule() {
    }

    /* (non-Javadoc)
     * @see events.VSAbstractProtocol#onServerInit()
     */
    public void onServerInit() {
    }

    /* (non-Javadoc)
     * @see protocols.VSAbstractProtocol#onServerReset()
     */
    public void onServerReset() {
        sentMessages.clear();
    }

    /* (non-Javadoc)
     * @see protocols.VSAbstractProtocol#onServerSchedule()
     */
    public void onServerSchedule() {
    }

    /* (non-Javadoc)
     * @see protocols.VSAbstractProtocol#onServerRecv(core.VSMessage)
     */
    public void onServerRecv(VSMessage recvMessage) {
        if (!sentMessages.contains(recvMessage.getIntegerObj("Broadcast"))) {
            VSMessage message = new VSMessage();
            message.setInteger("Broadcast",
                               recvMessage.getInteger("Broadcast"));
            sentMessages.add(message.getIntegerObj("Broadcast"));
            sendMessage(message);
        }
    }

    /* (non-Javadoc)
     * @see protocols.VSAbstractProtocol#toString()
     */
    public String toString() {
        return super.toString();
    }
}
