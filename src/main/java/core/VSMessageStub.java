package core;

/**
 * An object of this class represents a message stub. A message stub allows
 * to run the init method on a VSMessage object. The init method should be
 * hidden by the protocol programming API.
 *
 * @author Paul C. Buetow
 */
public class VSMessageStub {
    /** The message */
    private VSMessage message;

    /**
     * The constructor of the message stub. Creates a new message stub object.
     *
     * @param message the message
     */
    public VSMessageStub(VSMessage message) {
        this.message = message;
    }

    /* (non-Javadoc)
     * @see core.VSMessage#init(VSInternalProcess, java.util.String, boolean)
     */
    public void init(VSInternalProcess process, String protocolClassname,
                     boolean isServerMessage) {
        message.init(process, protocolClassname, isServerMessage);
    }
}
