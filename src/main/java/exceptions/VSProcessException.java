package exceptions;

/**
 * Exception thrown when there is an error related to process operations in the simulator.
 * This includes process creation, communication, or state management errors.
 *
 * @author Paul C. Buetow
 */
public class VSProcessException extends VSSimulatorException {
    /** The serial version uid */
    private static final long serialVersionUID = 1L;
    
    private final int processId;
    
    /**
     * Constructs a new process exception with the specified detail message.
     *
     * @param message the detail message
     */
    public VSProcessException(String message) {
        super(message);
        this.processId = -1;
    }
    
    /**
     * Constructs a new process exception for a specific process.
     *
     * @param processId the ID of the process that encountered the error
     * @param message the detail message
     */
    public VSProcessException(int processId, String message) {
        super(String.format("Process %d: %s", processId, message));
        this.processId = processId;
    }
    
    /**
     * Constructs a new process exception with the specified detail message and cause.
     *
     * @param processId the ID of the process that encountered the error
     * @param message the detail message
     * @param cause the cause
     */
    public VSProcessException(int processId, String message, Throwable cause) {
        super(String.format("Process %d: %s", processId, message), cause);
        this.processId = processId;
    }
    
    /**
     * Gets the process ID associated with this exception.
     *
     * @return the process ID, or -1 if not specific to a process
     */
    public int getProcessId() {
        return processId;
    }
}