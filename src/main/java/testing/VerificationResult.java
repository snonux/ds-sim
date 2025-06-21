package testing;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Aggregated result of all verification rules applied to a simulation.
 */
public class VerificationResult {
    private final List<RuleResult> ruleResults;
    private final boolean allPassed;
    
    public VerificationResult(List<RuleResult> ruleResults) {
        this.ruleResults = Collections.unmodifiableList(ruleResults);
        this.allPassed = ruleResults.stream().allMatch(RuleResult::isPassed);
    }
    
    public boolean passed() {
        return allPassed;
    }
    
    public List<RuleResult> getRuleResults() {
        return ruleResults;
    }
    
    public List<RuleResult> getFailedRules() {
        return ruleResults.stream()
            .filter(r -> !r.isPassed())
            .collect(Collectors.toList());
    }
    
    public String getFailureMessage() {
        if (allPassed) {
            return "All verification rules passed";
        }
        
        return "Failed rules:\n" + getFailedRules().stream()
            .map(r -> "  - " + r.getMessage())
            .collect(Collectors.joining("\n"));
    }
    
    public String generateReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Verification Report ===\n");
        sb.append("Total rules: ").append(ruleResults.size()).append("\n");
        sb.append("Passed: ").append(ruleResults.size() - getFailedRules().size()).append("\n");
        sb.append("Failed: ").append(getFailedRules().size()).append("\n\n");
        
        sb.append("Results:\n");
        for (RuleResult result : ruleResults) {
            sb.append("  ").append(result).append("\n");
        }
        
        return sb.toString();
    }
}