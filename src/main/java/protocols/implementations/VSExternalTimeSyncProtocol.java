package protocols.implementations;

import core.VSMessage;
import protocols.VSAbstractProtocol;

/**
 * The class VSExternalTimeSyncProtocol, an implementation of the external
 * time synchronisation protocol.
 *
 * @author Paul C. Buetow
 */
public class VSExternalTimeSyncProtocol extends VSAbstractProtocol {
    /** The request time. */
    private long requestTime;

    /** The server is waiting for response, if true. */
    private boolean waitingForResponse;

    /**
     * Instantiates a new external time sync protocol object.
     */
    public VSExternalTimeSyncProtocol() {
        super(VSAbstractProtocol.HAS_ON_CLIENT_START);
        setClassname(getClass().toString());
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
        requestTime = process.getTime();
        waitingForResponse = true;

        /* Multicast message to all processes */
        VSMessage message = new VSMessage();
        message.setBoolean("isClientRequest", true);
        sendMessage(message);
    }

    /* (non-Javadoc)
     * @see protocols.VSAbstractProtocol#onClientRecv(core.VSMessage)
     */
    public void onClientRecv(VSMessage recvMessage) {
        if (!recvMessage.getBoolean("isServerResponse"))
            return;

        if (waitingForResponse)
            waitingForResponse = false;
        else
            return;

        long recvTime = process.getTime();
        long roundTripTime = recvTime - requestTime;
        long serverTime = recvMessage.getLong("time");
        long newTime = serverTime + (long) (roundTripTime / 2);

        log("Server time: " + serverTime + "; RTT: " + roundTripTime + "; Old time: " + recvTime + "; New time: " + newTime + "; Offset: " + (newTime - recvTime));

        process.setTime(newTime);
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
    }

    /* (non-Javadoc)
     * @see protocols.VSAbstractProtocol#onServerRecv(core.VSMessage)
     */
    public void onServerRecv(VSMessage recvMessage) {
        if (!recvMessage.getBoolean("isClientRequest"))
            return;

        /* Multicast message to all processes */
        VSMessage message = new VSMessage();
        message.setLong("time", process.getTime());
        message.setBoolean("isServerResponse", true);
        sendMessage(message);
    }

    /* (non-Javadoc)
     * @see protocols.VSAbstractProtocol#onServerSchedule()
     */
    public void onServerSchedule() {
    }

    /* (non-Javadoc)
     * @see protocols.VSAbstractProtocol#toString()
     */
    public String toString() {
        return super.toString();
    }
}
