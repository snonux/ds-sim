package exceptions;

/**
 * Exception thrown when attempting to copy an event that does not support copying.
 * Events must implement VSCopyableEvent interface to be copyable.
 *
 * @author Paul C. Buetow
 */
public class VSEventNotCopyableException extends VSSimulatorException {
    /** The serial version uid */
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new event not copyable exception with the specified event description.
     *
     * @param eventDescription description of the event that cannot be copied
     */
    public VSEventNotCopyableException(String eventDescription) {
        super("Event cannot be copied: " + eventDescription);
    }
}
