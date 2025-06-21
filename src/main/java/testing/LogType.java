package testing;

/**
 * Enum representing the type of log entry.
 */
public enum LogType {
    /**
     * Global log message not associated with a specific process
     */
    GLOBAL,
    
    /**
     * Process-specific log message
     */
    PROCESS,
    
    /**
     * System-level message (errors, warnings)
     */
    SYSTEM
}