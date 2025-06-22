package testing;

import java.util.*;
import java.util.regex.*;
import java.util.function.Predicate;

/**
 * Flexible verification system for checking protocol behavior through log analysis.
 * Supports pattern matching, sequence verification, and count-based assertions.
 */
public class ProtocolVerifier {
    private final List<VerificationRule> rules;
    
    public ProtocolVerifier() {
        this.rules = new ArrayList<>();
    }
    
    /**
     * Add a custom verification rule.
     */
    public ProtocolVerifier withRule(VerificationRule rule) {
        rules.add(rule);
        return this;
    }
    
    /**
     * Expect a log message containing the pattern at least once.
     */
    public ProtocolVerifier expectLog(String pattern) {
        rules.add(new PatternRule(pattern, 1, Integer.MAX_VALUE));
        return this;
    }
    
    /**
     * Expect a log message containing the pattern exactly n times.
     */
    public ProtocolVerifier expectLogExactly(String pattern, int count) {
        rules.add(new PatternRule(pattern, count, count));
        return this;
    }
    
    /**
     * Expect a log message containing the pattern at least n times.
     */
    public ProtocolVerifier expectLogAtLeast(String pattern, int minCount) {
        rules.add(new PatternRule(pattern, minCount, Integer.MAX_VALUE));
        return this;
    }
    
    /**
     * Expect a log message containing the pattern at most n times.
     */
    public ProtocolVerifier expectLogAtMost(String pattern, int maxCount) {
        rules.add(new PatternRule(pattern, 0, maxCount));
        return this;
    }
    
    /**
     * Expect a sequence of patterns in order.
     */
    public ProtocolVerifier expectSequence(String... patterns) {
        rules.add(new SequenceRule(Arrays.asList(patterns)));
        return this;
    }
    
    /**
     * Expect no log messages containing the pattern.
     */
    public ProtocolVerifier expectNoLog(String pattern) {
        rules.add(new PatternRule(pattern, 0, 0));
        return this;
    }
    
    /**
     * Expect a log from a specific process.
     */
    public ProtocolVerifier expectLogFromProcess(int processNum, String pattern) {
        rules.add(new ProcessPatternRule(processNum, pattern, 1, Integer.MAX_VALUE));
        return this;
    }
    
    /**
     * Expect at least n messages to be sent during the simulation.
     */
    public ProtocolVerifier expectAtLeastNMessages(int minMessages) {
        rules.add(new MessageCountRule(minMessages, Integer.MAX_VALUE));
        return this;
    }
    
    /**
     * Expect exactly n messages to be sent during the simulation.
     */
    public ProtocolVerifier expectExactlyNMessages(int count) {
        rules.add(new MessageCountRule(count, count));
        return this;
    }
    
    /**
     * Expect messages to be sent (at least 1).
     */
    public ProtocolVerifier expectMessages() {
        return expectAtLeastNMessages(1);
    }
    
    /**
     * Verify all rules against the provided logs.
     */
    public VerificationResult verify(List<LogEntry> logs) {
        List<RuleResult> results = new ArrayList<>();
        
        for (VerificationRule rule : rules) {
            results.add(rule.verify(logs));
        }
        
        return new VerificationResult(results);
    }
    
    // Rule implementations
    
    /**
     * Rule that matches log messages against a pattern.
     */
    private static class PatternRule implements VerificationRule {
        private final Pattern pattern;
        private final int minCount;
        private final int maxCount;
        private final String description;
        
        public PatternRule(String pattern, int minCount, int maxCount) {
            // Try to compile as regex first, if it fails, use literal matching
            Pattern compiledPattern;
            try {
                compiledPattern = Pattern.compile(pattern);
            } catch (PatternSyntaxException e) {
                // If not a valid regex, escape it for literal matching
                compiledPattern = Pattern.compile(Pattern.quote(pattern));
            }
            this.pattern = compiledPattern;
            this.minCount = minCount;
            this.maxCount = maxCount;
            this.description = String.format(
                "Pattern '%s' should appear %s times",
                pattern,
                minCount == maxCount ? 
                    String.valueOf(minCount) : 
                    minCount + "-" + (maxCount == Integer.MAX_VALUE ? "∞" : maxCount)
            );
        }
        
        @Override
        public RuleResult verify(List<LogEntry> logs) {
            int count = 0;
            List<LogEntry> matches = new ArrayList<>();
            
            for (LogEntry log : logs) {
                if (pattern.matcher(log.getMessage()).find()) {
                    count++;
                    matches.add(log);
                }
            }
            
            boolean passed = count >= minCount && count <= maxCount;
            String message = String.format(
                "%s (found %d occurrences)",
                description, count
            );
            
            return new RuleResult(passed, message, matches);
        }
    }
    
    /**
     * Rule that verifies a sequence of patterns appear in order.
     */
    private static class SequenceRule implements VerificationRule {
        private final List<Pattern> patterns;
        private final String description;
        
        public SequenceRule(List<String> patterns) {
            this.patterns = new ArrayList<>();
            for (String p : patterns) {
                try {
                    this.patterns.add(Pattern.compile(p));
                } catch (PatternSyntaxException e) {
                    this.patterns.add(Pattern.compile(Pattern.quote(p)));
                }
            }
            this.description = "Sequence: " + String.join(" → ", patterns);
        }
        
        @Override
        public RuleResult verify(List<LogEntry> logs) {
            int patternIndex = 0;
            List<LogEntry> matches = new ArrayList<>();
            
            for (LogEntry log : logs) {
                if (patternIndex < patterns.size() && 
                    patterns.get(patternIndex).matcher(log.getMessage()).find()) {
                    matches.add(log);
                    patternIndex++;
                }
            }
            
            boolean passed = patternIndex == patterns.size();
            String message = String.format(
                "%s (%d/%d patterns matched)",
                description, patternIndex, patterns.size()
            );
            
            return new RuleResult(passed, message, matches);
        }
    }
    
    /**
     * Rule that matches patterns from a specific process.
     */
    private static class ProcessPatternRule implements VerificationRule {
        private final int processNum;
        private final Pattern pattern;
        private final int minCount;
        private final int maxCount;
        private final String description;
        
        public ProcessPatternRule(int processNum, String pattern, int minCount, int maxCount) {
            this.processNum = processNum;
            Pattern compiledPattern;
            try {
                compiledPattern = Pattern.compile(pattern);
            } catch (PatternSyntaxException e) {
                compiledPattern = Pattern.compile(Pattern.quote(pattern));
            }
            this.pattern = compiledPattern;
            this.minCount = minCount;
            this.maxCount = maxCount;
            this.description = String.format(
                "Process %d: Pattern '%s' should appear %s times",
                processNum, pattern,
                minCount == maxCount ? 
                    String.valueOf(minCount) : 
                    minCount + "-" + (maxCount == Integer.MAX_VALUE ? "∞" : maxCount)
            );
        }
        
        @Override
        public RuleResult verify(List<LogEntry> logs) {
            int count = 0;
            List<LogEntry> matches = new ArrayList<>();
            
            for (LogEntry log : logs) {
                if (log.isFromProcess(processNum) && 
                    pattern.matcher(log.getMessage()).find()) {
                    count++;
                    matches.add(log);
                }
            }
            
            boolean passed = count >= minCount && count <= maxCount;
            String message = String.format(
                "%s (found %d occurrences)",
                description, count
            );
            
            return new RuleResult(passed, message, matches);
        }
    }
    
    /**
     * Rule that verifies message count.
     */
    private static class MessageCountRule implements VerificationRule {
        private final int minCount;
        private final int maxCount;
        private final String description;
        
        public MessageCountRule(int minCount, int maxCount) {
            this.minCount = minCount;
            this.maxCount = maxCount;
            this.description = String.format(
                "Message count should be %s",
                minCount == maxCount ? 
                    String.valueOf(minCount) : 
                    minCount + "-" + (maxCount == Integer.MAX_VALUE ? "∞" : maxCount)
            );
        }
        
        @Override
        public RuleResult verify(List<LogEntry> logs) {
            int messageCount = 0;
            List<LogEntry> messageLogs = new ArrayList<>();
            
            // Count all "Message sent" logs
            for (LogEntry log : logs) {
                if (log.getMessage().contains("Message sent")) {
                    messageCount++;
                    messageLogs.add(log);
                }
            }
            
            boolean passed = messageCount >= minCount && messageCount <= maxCount;
            String message = String.format(
                "%s (found %d messages)",
                description, messageCount
            );
            
            return new RuleResult(passed, message, messageLogs);
        }
    }
}