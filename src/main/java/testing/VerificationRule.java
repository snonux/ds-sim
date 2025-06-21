package testing;

import java.util.List;

/**
 * Interface for defining verification rules that can be applied
 * to simulation logs to verify protocol behavior.
 */
public interface VerificationRule {
    /**
     * Verify the rule against the provided logs.
     * 
     * @param logs The complete list of log entries from the simulation
     * @return Result indicating whether the rule passed and details
     */
    RuleResult verify(List<LogEntry> logs);
    
    /**
     * Get a human-readable description of what this rule verifies.
     * 
     * @return Description of the rule
     */
    default String getDescription() {
        return this.getClass().getSimpleName();
    }
}