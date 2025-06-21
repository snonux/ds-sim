package testing;

import simulator.VSLogging;
import simulator.VSSimulatorVisualization;
import core.VSInternalProcess;
import java.util.*;
import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Custom logging implementation that captures all log messages during
 * headless simulation execution for later verification.
 */
public class LogCapture extends VSLogging {
    private final List<LogEntry> capturedLogs;
    private final Map<Integer, List<LogEntry>> processLogs;
    private final List<LogListener> listeners;
    private boolean printLogs = false;
    private String logPrefix = "[LOG] ";
    
    public LogCapture() {
        super();
        this.capturedLogs = new CopyOnWriteArrayList<>();
        this.processLogs = new ConcurrentHashMap<>();
        this.listeners = new CopyOnWriteArrayList<>();
    }
    
    public void setPrintLogs(boolean printLogs) {
        this.printLogs = printLogs;
    }
    
    public void setLogPrefix(String prefix) {
        this.logPrefix = prefix;
    }
    
    @Override
    public synchronized void log(String message) {
        // Call parent to maintain compatibility
        super.log(message);
        
        long time = 0;
        if (getSimulatorVisualization() != null) {
            time = getSimulatorVisualization().getTime();
        }
        
        LogEntry entry = new LogEntry(time, message, LogType.GLOBAL, -1);
        capturedLogs.add(entry);
        notifyListeners(entry);
        
        if (printLogs) {
            System.out.println(logPrefix + entry);
        }
    }
    
    @Override
    public synchronized void log(String message, long time) {
        super.log(message, time);
        
        LogEntry entry = new LogEntry(time, message, LogType.GLOBAL, -1);
        capturedLogs.add(entry);
        notifyListeners(entry);
        
        if (printLogs) {
            System.out.println(logPrefix + entry);
        }
    }
    
    /**
     * Log a message from a specific process.
     * Note: This method is called by protocols and events.
     */
    public synchronized void log(VSInternalProcess process, String message) {
        // Create formatted message for parent
        String formattedMessage = "Process " + process.getProcessNum() + 
                                ": " + message;
        super.log(formattedMessage, process.getTime());
        
        LogEntry entry = new LogEntry(
            process.getTime(),
            message,
            LogType.PROCESS,
            process.getProcessNum()
        );
        
        capturedLogs.add(entry);
        processLogs.computeIfAbsent(process.getProcessNum(), 
                                   k -> new CopyOnWriteArrayList<>())
                   .add(entry);
        notifyListeners(entry);
        
        if (printLogs) {
            System.out.println(logPrefix + "[P" + process.getProcessNum() + "] " + message);
        }
    }
    
    private void notifyListeners(LogEntry entry) {
        for (LogListener listener : listeners) {
            try {
                listener.onLogEntry(entry);
            } catch (Exception e) {
                System.err.println("Error notifying log listener: " + e.getMessage());
            }
        }
    }
    
    /**
     * Get the simulator visualization reference.
     */
    private VSSimulatorVisualization getSimulatorVisualization() {
        try {
            Field field = VSLogging.class.getDeclaredField("simulatorVisualization");
            field.setAccessible(true);
            return (VSSimulatorVisualization) field.get(this);
        } catch (Exception e) {
            return null;
        }
    }
    
    public List<LogEntry> getCapturedLogs() {
        return new ArrayList<>(capturedLogs);
    }
    
    public Map<Integer, List<LogEntry>> getProcessLogs() {
        Map<Integer, List<LogEntry>> result = new HashMap<>();
        for (Map.Entry<Integer, List<LogEntry>> entry : processLogs.entrySet()) {
            result.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        return result;
    }
    
    public int getTotalLogCount() {
        return capturedLogs.size();
    }
    
    public Map<Integer, Integer> getProcessMessageCounts() {
        Map<Integer, Integer> counts = new HashMap<>();
        for (Map.Entry<Integer, List<LogEntry>> entry : processLogs.entrySet()) {
            counts.put(entry.getKey(), entry.getValue().size());
        }
        return counts;
    }
    
    public void addListener(LogListener listener) {
        listeners.add(listener);
    }
    
    public void removeListener(LogListener listener) {
        listeners.remove(listener);
    }
    
    @Override
    public synchronized void clear() {
        super.clear();
        capturedLogs.clear();
        processLogs.clear();
    }
}