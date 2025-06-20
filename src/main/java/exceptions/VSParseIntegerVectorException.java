package exceptions;

/**
 * Exception thrown when the VSAbstractEditor cannot parse vector field input.
 * This typically occurs when user input for vector values is malformed.
 *
 * @author Paul C. Buetow
 */
public class VSParseIntegerVectorException extends VSSimulatorException {
    /** The serial version uid */
    private static final long serialVersionUID = 1L;
    
    /**
     * Constructs a new parse integer vector exception.
     */
    public VSParseIntegerVectorException() {
        super("Failed to parse integer vector from input");
    }
    
    /**
     * Constructs a new parse integer vector exception with the specified input string.
     *
     * @param input the input string that could not be parsed
     */
    public VSParseIntegerVectorException(String input) {
        super("Failed to parse integer vector from input: " + input);
    }
    
    /**
     * Constructs a new parse integer vector exception with the specified input and cause.
     *
     * @param input the input string that could not be parsed
     * @param cause the underlying cause of the parse failure
     */
    public VSParseIntegerVectorException(String input, Throwable cause) {
        super("Failed to parse integer vector from input: " + input, cause);
    }
}
