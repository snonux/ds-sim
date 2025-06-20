package exceptions;

/**
 * Exception thrown when there is a configuration error in the simulator.
 * This includes invalid preferences, missing required settings, or conflicting configurations.
 *
 * @author Paul C. Buetow
 */
public class VSConfigurationException extends VSSimulatorException {
    /** The serial version uid */
    private static final long serialVersionUID = 1L;
    
    /**
     * Constructs a new configuration exception with the specified detail message.
     *
     * @param message the detail message
     */
    public VSConfigurationException(String message) {
        super(message);
    }
    
    /**
     * Constructs a new configuration exception with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause
     */
    public VSConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}