package protocols.implementations;

import core.VSMessage;
import protocols.VSAbstractProtocol;

/**
 * The class VSPingPongProtocol, an implementation of the ping pong protocol.
 *
 * @author Paul C. Buetow
 */
public class VSPingPongProtocol extends VSAbstractProtocol {
    /** The client counter. */
    private int clientCounter;

    /** The server counter. */
    private int serverCounter;

    /**
     * Instantiates a new ping pong protocol.
     */
    public VSPingPongProtocol() {
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
        clientCounter = 0;
    }

    /* (non-Javadoc)
     * @see protocols.VSAbstractProtocol#onClientStart()
     */
    public void onClientStart() {
        VSMessage message = new VSMessage();
        message.setBoolean("fromClient", true);
        message.setInteger("counter", ++clientCounter);
        super.sendMessage(message);
    }

    /* (non-Javadoc)
     * @see protocols.VSAbstractProtocol#onClientRecv(core.VSMessage)
     */
    public void onClientRecv(VSMessage recvMessage) {
        if (!recvMessage.getBoolean("fromServer"))
            return;

        super.log("message: " + recvMessage.getInteger("counter"));

        VSMessage message = new VSMessage();
        message.setBoolean("fromClient", true);
        message.setInteger("counter", ++clientCounter);
        super.sendMessage(message);
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
        serverCounter = 0;
    }

    /* (non-Javadoc)
     * @see protocols.VSAbstractProtocol#onServerRecv(core.VSMessage)
     */
    public void onServerRecv(VSMessage recvMessage) {
        if (!recvMessage.getBoolean("fromClient"))
            return;

        super.log("message: " + recvMessage.getInteger("counter"));

        VSMessage message = new VSMessage();
        message.setBoolean("fromServer", true);
        message.setInteger("counter", ++serverCounter);
        super.sendMessage(message);
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
        return super.toString() + "; New message afterwards";
    }
}
