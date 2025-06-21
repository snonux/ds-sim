package testing;

/**
 * Interface for receiving log events in real-time during simulation execution.
 * Useful for monitoring, debugging, or implementing custom verification logic.
 */
public interface LogListener {
    /**
     * Called when a new log entry is captured.
     * 
     * @param entry The captured log entry
     */
    void onLogEntry(LogEntry entry);
}