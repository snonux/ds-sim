package testing;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Contains the results of a headless simulation run, including
 * captured logs and execution metrics.
 */
public class SimulationResult {
    private final List<LogEntry> allLogs;
    private final Map<Integer, List<LogEntry>> processLogs;
    private final SimulationMetrics metrics;
    
    public SimulationResult(List<LogEntry> allLogs, 
                           Map<Integer, List<LogEntry>> processLogs,
                           SimulationMetrics metrics) {
        this.allLogs = Collections.unmodifiableList(allLogs);
        this.processLogs = Collections.unmodifiableMap(processLogs);
        this.metrics = metrics;
    }
    
    public List<LogEntry> getAllLogs() {
        return allLogs;
    }
    
    public List<LogEntry> getLogsForProcess(int processNum) {
        return processLogs.getOrDefault(processNum, Collections.emptyList());
    }
    
    public Map<Integer, List<LogEntry>> getProcessLogs() {
        return processLogs;
    }
    
    public SimulationMetrics getMetrics() {
        return metrics;
    }
    
    /**
     * Count logs matching a pattern
     */
    public int countLogs(String pattern) {
        return (int) allLogs.stream()
            .filter(log -> log.getMessage().contains(pattern))
            .count();
    }
    
    /**
     * Find first log matching pattern
     */
    public Optional<LogEntry> findFirst(String pattern) {
        return allLogs.stream()
            .filter(log -> log.getMessage().contains(pattern))
            .findFirst();
    }
    
    /**
     * Find all logs matching pattern
     */
    public List<LogEntry> findAll(String pattern) {
        return allLogs.stream()
            .filter(log -> log.getMessage().contains(pattern))
            .collect(Collectors.toList());
    }
    
    /**
     * Get logs in time range
     */
    public List<LogEntry> getLogsInTimeRange(long startTime, long endTime) {
        return allLogs.stream()
            .filter(log -> log.getTimestamp() >= startTime && 
                          log.getTimestamp() <= endTime)
            .collect(Collectors.toList());
    }
    
    /**
     * Generate summary report
     */
    public String generateSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Simulation Result Summary ===\n");
        sb.append("Total logs: ").append(allLogs.size()).append("\n");
        sb.append("Processes: ").append(metrics.getNumProcesses()).append("\n");
        sb.append("\nLogs per process:\n");
        
        for (Map.Entry<Integer, Integer> entry : 
             metrics.getProcessMessageCounts().entrySet()) {
            sb.append("  Process ").append(entry.getKey())
              .append(": ").append(entry.getValue()).append(" logs\n");
        }
        
        return sb.toString();
    }
}