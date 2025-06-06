package events.internal;

import core.VSMessage;
import protocols.VSAbstractProtocol;
import serialize.VSNotSerializable;

/**
 * The class VSMessageReceiveEvent, this event is used if a process receives
 * a message.
 *
 * @author Paul C. Buetow
 */
public class VSMessageReceiveEvent extends VSAbstractInternalEvent
    implements VSNotSerializable {

    /** The message. */
    private VSMessage message;

    /**
     * Instantiates a new message receive event.
     *
     * @param message the message
     */
    public VSMessageReceiveEvent(VSMessage message) {
        this.message = message;
    }

    /* (non-Javadoc)
     * @see events.VSAbstractEvent#onInit()
     */
    public void onInit() {
        setClassname(getClass().toString());
    }

    /* (non-Javadoc)
     * @see events.VSAbstractEvent#onStart()
     */
    public void onStart() {
        boolean onlyRelevantMessages = process.getPrefs().getBoolean("sim.messages.relevant");

        //String eventName = message.getName();
        String protocolClassname = message.getProtocolClassname();

        if (onlyRelevantMessages && !isRelevantMessage())
            return;

        Object protocolObj = null;

        if (process.objectExists(protocolClassname))
            protocolObj = process.getObject(protocolClassname);

        process.updateLamportTime(message.getLamportTime()+1);
        process.updateVectorTime(message.getVectorTime());

        StringBuffer buffer = new StringBuffer();
        buffer.append(prefs.getString("lang.message.recv"));
        buffer.append("; ");
        buffer.append(message);;
        log(buffer.toString());

        if (protocolObj != null)
            ((VSAbstractProtocol) protocolObj).onMessageRecvStart(message);
    }

    /**
     * Checks if the message delivering is relevant.
     *
     * @return true, if relevant
     */
    public boolean isRelevantMessage() {
        String protocolClassname = message.getProtocolClassname();
        Object protocolObj = null;

        if (process.objectExists(protocolClassname))
            protocolObj = process.getObject(protocolClassname);
        else
            return false;

        if (!((VSAbstractProtocol) protocolObj).isRelevantMessage(message))
            return false;

        return true;
    }
}
