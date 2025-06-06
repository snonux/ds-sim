package protocols.implementations;

import core.VSMessage;
import protocols.VSAbstractProtocol;

/**
 * The class VSDummyProtocol, can be used as a template in order to create
 * own protocols.
 *
 * @author Paul C. Buetow
 */
public class VSDummyProtocol extends VSAbstractProtocol {
    /**
     * Instantiates a new dummy protocol object.
     */
    public VSDummyProtocol() {
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
        log("onClientReset()");
    }

    /* (non-Javadoc)
     * @see protocols.VSAbstractProtocol#onClientStart()
     */
    public void onClientStart() {
        log("onClientStart()");

        VSMessage message = new VSMessage();
        message.setString("Greeting", "Hello World!");
        message.setInteger("A number", 1);
        message.setBoolean("A boolean", true);
        message.setFloat("A float", 1.2f);
        sendMessage(message);
    }

    /* (non-Javadoc)
     * @see protocols.VSAbstractProtocol#onClientRecv(core.VSMessage)
     */
    public void onClientRecv(VSMessage recvMessage) {
        log("onClientRecv("+recvMessage+")");

        /*
        String s = recvMessage.getString("Greeting");
        int n = recvMessage.getInteger("A number");
        boolean b = recvMessage.getBoolean("A boolean");
        float f = recvMessage.getFloat("A float");
        */
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
        log("onClientReset()");
    }

    /* (non-Javadoc)
     * @see protocols.VSAbstractProtocol#onServerRecv(core.VSMessage)
     */
    public void onServerRecv(VSMessage recvMessage) {
        log("onServerRecv("+recvMessage+")");
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
        return super.toString() + "; Dummy Test";
    }
}
