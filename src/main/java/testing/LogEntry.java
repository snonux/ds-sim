package testing;

/**
 * Represents a single log entry captured during simulation execution.
 * Immutable data class for thread-safe log collection.
 */
public class LogEntry {
    private final long timestamp;
    private final String message;
    private final LogType type;
    private final int processNum;
    
    public LogEntry(long timestamp, String message, LogType type, int processNum) {
        this.timestamp = timestamp;
        this.message = message;
        this.type = type;
        this.processNum = processNum;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public String getMessage() {
        return message;
    }
    
    public LogType getType() {
        return type;
    }
    
    public int getProcessNum() {
        return processNum;
    }
    
    public boolean isFromProcess(int processNum) {
        return this.processNum == processNum;
    }
    
    public boolean isGlobal() {
        return type == LogType.GLOBAL;
    }
    
    @Override
    public String toString() {
        if (type == LogType.PROCESS) {
            return String.format("[%d] Process %d: %s", timestamp, processNum, message);
        } else {
            return String.format("[%d] %s", timestamp, message);
        }
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        LogEntry logEntry = (LogEntry) o;
        return timestamp == logEntry.timestamp &&
               processNum == logEntry.processNum &&
               type == logEntry.type &&
               message.equals(logEntry.message);
    }
    
    @Override
    public int hashCode() {
        int result = Long.hashCode(timestamp);
        result = 31 * result + message.hashCode();
        result = 31 * result + type.hashCode();
        result = 31 * result + processNum;
        return result;
    }
}