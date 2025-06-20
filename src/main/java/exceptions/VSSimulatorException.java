package exceptions;

/**
 * Base exception class for all DS-Sim simulator exceptions.
 * This provides a common base for all custom exceptions in the application.
 *
 * @author Paul C. Buetow
 */
public class VSSimulatorException extends Exception {
    /** The serial version uid */
    private static final long serialVersionUID = 1L;
    
    /**
     * Constructs a new simulator exception with null as its detail message.
     */
    public VSSimulatorException() {
        super();
    }
    
    /**
     * Constructs a new simulator exception with the specified detail message.
     *
     * @param message the detail message
     */
    public VSSimulatorException(String message) {
        super(message);
    }
    
    /**
     * Constructs a new simulator exception with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause
     */
    public VSSimulatorException(String message, Throwable cause) {
        super(message, cause);
    }
    
    /**
     * Constructs a new simulator exception with the specified cause.
     *
     * @param cause the cause
     */
    public VSSimulatorException(Throwable cause) {
        super(cause);
    }
}