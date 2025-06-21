package testing;

import java.util.Collections;
import java.util.List;

/**
 * Result of applying a verification rule to simulation logs.
 */
public class RuleResult {
    private final boolean passed;
    private final String message;
    private final List<LogEntry> relevantLogs;
    
    public RuleResult(boolean passed, String message, List<LogEntry> relevantLogs) {
        this.passed = passed;
        this.message = message;
        this.relevantLogs = Collections.unmodifiableList(relevantLogs);
    }
    
    public RuleResult(boolean passed, String message) {
        this(passed, message, Collections.emptyList());
    }
    
    public boolean isPassed() {
        return passed;
    }
    
    public String getMessage() {
        return message;
    }
    
    public List<LogEntry> getRelevantLogs() {
        return relevantLogs;
    }
    
    @Override
    public String toString() {
        return (passed ? "PASS" : "FAIL") + ": " + message;
    }
}