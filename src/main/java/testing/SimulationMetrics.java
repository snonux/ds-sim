package testing;

import java.util.Collections;
import java.util.Map;

/**
 * Metrics collected during simulation execution.
 */
public class SimulationMetrics {
    private final int numProcesses;
    private final int totalLogCount;
    private final Map<Integer, Integer> processMessageCounts;
    
    public SimulationMetrics(int numProcesses, 
                            int totalLogCount,
                            Map<Integer, Integer> processMessageCounts) {
        this.numProcesses = numProcesses;
        this.totalLogCount = totalLogCount;
        this.processMessageCounts = Collections.unmodifiableMap(processMessageCounts);
    }
    
    public int getNumProcesses() {
        return numProcesses;
    }
    
    public int getTotalLogCount() {
        return totalLogCount;
    }
    
    public Map<Integer, Integer> getProcessMessageCounts() {
        return processMessageCounts;
    }
    
    public int getMessageCountForProcess(int processNum) {
        return processMessageCounts.getOrDefault(processNum, 0);
    }
    
    public double getAverageMessagesPerProcess() {
        if (numProcesses == 0) return 0;
        
        int totalProcessMessages = processMessageCounts.values().stream()
            .mapToInt(Integer::intValue)
            .sum();
            
        return (double) totalProcessMessages / numProcesses;
    }
}