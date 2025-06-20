package exceptions;

/**
 * Base runtime exception class for DS-Sim simulator.
 * Used for unrecoverable errors that should not be caught in normal flow.
 *
 * @author Paul C. Buetow
 */
public class VSSimulatorRuntimeException extends RuntimeException {
    /** The serial version uid */
    private static final long serialVersionUID = 1L;
    
    /**
     * Constructs a new simulator runtime exception with null as its detail message.
     */
    public VSSimulatorRuntimeException() {
        super();
    }
    
    /**
     * Constructs a new simulator runtime exception with the specified detail message.
     *
     * @param message the detail message
     */
    public VSSimulatorRuntimeException(String message) {
        super(message);
    }
    
    /**
     * Constructs a new simulator runtime exception with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause
     */
    public VSSimulatorRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }
    
    /**
     * Constructs a new simulator runtime exception with the specified cause.
     *
     * @param cause the cause
     */
    public VSSimulatorRuntimeException(Throwable cause) {
        super(cause);
    }
}