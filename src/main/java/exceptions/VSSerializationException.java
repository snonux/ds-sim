package exceptions;

/**
 * Exception thrown when serialization or deserialization of simulator objects fails.
 * This is used when saving or loading simulation states.
 *
 * @author Paul C. Buetow
 */
public class VSSerializationException extends VSSimulatorException {
    /** The serial version uid */
    private static final long serialVersionUID = 1L;
    
    /**
     * Constructs a new serialization exception with the specified detail message.
     *
     * @param message the detail message
     */
    public VSSerializationException(String message) {
        super(message);
    }
    
    /**
     * Constructs a new serialization exception with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause (typically an IOException)
     */
    public VSSerializationException(String message, Throwable cause) {
        super(message, cause);
    }
    
    /**
     * Constructs a new serialization exception for a specific object type.
     *
     * @param objectType the type of object that failed to serialize
     * @param operation either "serialize" or "deserialize"
     * @param cause the underlying cause
     */
    public VSSerializationException(String objectType, String operation, Throwable cause) {
        super(String.format("Failed to %s object of type: %s", operation, objectType), cause);
    }
}