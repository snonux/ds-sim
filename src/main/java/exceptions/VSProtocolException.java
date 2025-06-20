package exceptions;

/**
 * Exception thrown when there is an error in protocol execution or initialization.
 * This includes protocol misconfiguration, invalid state transitions, or communication errors.
 *
 * @author Paul C. Buetow
 */
public class VSProtocolException extends VSSimulatorException {
    /** The serial version uid */
    private static final long serialVersionUID = 1L;
    
    private final String protocolName;
    
    /**
     * Constructs a new protocol exception with the specified detail message.
     *
     * @param message the detail message
     */
    public VSProtocolException(String message) {
        super(message);
        this.protocolName = null;
    }
    
    /**
     * Constructs a new protocol exception for a specific protocol.
     *
     * @param protocolName the name of the protocol that encountered the error
     * @param message the detail message
     */
    public VSProtocolException(String protocolName, String message) {
        super(String.format("Protocol '%s': %s", protocolName, message));
        this.protocolName = protocolName;
    }
    
    /**
     * Constructs a new protocol exception with the specified detail message and cause.
     *
     * @param protocolName the name of the protocol that encountered the error
     * @param message the detail message
     * @param cause the cause
     */
    public VSProtocolException(String protocolName, String message, Throwable cause) {
        super(String.format("Protocol '%s': %s", protocolName, message), cause);
        this.protocolName = protocolName;
    }
    
    /**
     * Gets the protocol name associated with this exception.
     *
     * @return the protocol name, or null if not specific to a protocol
     */
    public String getProtocolName() {
        return protocolName;
    }
}