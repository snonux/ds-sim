package exceptions;

/**
 * Exception thrown when a negative number is encountered where a positive number is expected.
 * This is typically used for validation of numeric inputs that must be non-negative.
 *
 * @author Paul C. Buetow
 */
public class VSNegativeNumberException extends VSSimulatorException {
    /** The serial version uid */
    private static final long serialVersionUID = 1L;
    
    /**
     * Constructs a new negative number exception.
     */
    public VSNegativeNumberException() {
        super("Negative number not allowed");
    }
    
    /**
     * Constructs a new negative number exception with the specified field name.
     *
     * @param fieldName the name of the field that contained the negative number
     */
    public VSNegativeNumberException(String fieldName) {
        super("Negative number not allowed for field: " + fieldName);
    }
    
    /**
     * Constructs a new negative number exception with the specified field name and value.
     *
     * @param fieldName the name of the field that contained the negative number
     * @param value the negative value that was encountered
     */
    public VSNegativeNumberException(String fieldName, long value) {
        super(String.format("Negative number not allowed for field '%s': %d", fieldName, value));
    }
}
