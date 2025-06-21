# Headless Protocol Testing Framework Proposal

## Executive Summary

This proposal outlines a comprehensive headless testing framework for DS-Sim that enables automated verification of distributed protocols by:
1. Loading saved simulations without GUI dependencies
2. Replaying simulations in a controlled environment
3. Capturing and analyzing log outputs
4. Verifying protocol behavior through log pattern matching

## Architecture Overview

### Core Components

```
┌─────────────────────────────────────────────────────────────┐
│                  Headless Testing Framework                  │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────────────┐    ┌──────────────┐    ┌──────────┐  │
│  │ Headless Runner │───▶│ Log Capturer │───▶│ Verifier │  │
│  └────────┬────────┘    └──────────────┘    └──────────┘  │
│           │                                                 │
│           ▼                                                 │
│  ┌─────────────────┐    ┌──────────────┐                  │
│  │ Simulation Mgr  │───▶│   Protocol   │                  │
│  │   (No Frame)    │    │   Executor   │                  │
│  └─────────────────┘    └──────────────┘                  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## Detailed Design

### 1. HeadlessSimulationRunner

The main entry point for running simulations without GUI:

```java
public class HeadlessSimulationRunner {
    private VSSimulator simulator;
    private VSSimulatorVisualization viz;
    private LogCapture logCapture;
    private boolean running;
    
    public SimulationResult runSimulation(String simulationFile, long maxTime) {
        // 1. Load simulation without frame
        // 2. Install log capture
        // 3. Run simulation steps
        // 4. Return captured logs and metrics
    }
}
```

### 2. LogCapture System

A custom logging implementation that intercepts all log messages:

```java
public class LogCapture extends VSLogging {
    private final List<LogEntry> capturedLogs;
    private final Map<Integer, List<LogEntry>> processlogs;
    
    @Override
    public synchronized void log(String message, long time) {
        LogEntry entry = new LogEntry(time, message, LogType.GLOBAL);
        capturedLogs.add(entry);
        notifyListeners(entry);
    }
    
    @Override
    public synchronized void log(VSInternalProcess process, String message) {
        LogEntry entry = new LogEntry(
            process.getTime(), 
            message, 
            LogType.PROCESS,
            process.getProcessNum()
        );
        capturedLogs.add(entry);
        processLogs.computeIfAbsent(process.getProcessNum(), k -> new ArrayList<>())
                   .add(entry);
    }
}
```

### 3. Protocol Verifier

A flexible verification system using pattern matching and assertions:

```java
public class ProtocolVerifier {
    private final List<VerificationRule> rules;
    
    public VerificationResult verify(List<LogEntry> logs) {
        List<RuleResult> results = new ArrayList<>();
        
        for (VerificationRule rule : rules) {
            results.add(rule.verify(logs));
        }
        
        return new VerificationResult(results);
    }
}

public interface VerificationRule {
    RuleResult verify(List<LogEntry> logs);
}
```

### 4. Test Definition Format

Tests are defined using a fluent API or configuration files:

```java
@Test
public void testRaftLeaderElection() {
    HeadlessTest test = HeadlessTest.builder()
        .withSimulation("saved-simulations/raft-working.dat")
        .runFor(5000) // milliseconds
        .expectLog().containing("CANDIDATE").atLeastOnce()
        .expectLog().matching("Elected as LEADER").exactly(1)
        .expectLog().containing("REQUEST_VOTE").atLeast(2)
        .expectSequence()
            .first("FOLLOWER")
            .then("CANDIDATE") 
            .finally("LEADER")
            .withinTime(3000)
        .build();
        
    TestResult result = test.run();
    assertTrue(result.passed());
}
```

## Implementation Classes

### HeadlessSimulationRunner.java

```java
package testing;

import simulator.*;
import core.*;
import prefs.*;
import events.*;
import serialize.VSSerialize;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;

public class HeadlessSimulationRunner {
    private final VSDefaultPrefs prefs;
    private VSSimulator simulator;
    private VSSimulatorVisualization viz;
    private LogCapture logCapture;
    private final ExecutorService executor;
    
    public HeadlessSimulationRunner() {
        this.prefs = new VSDefaultPrefs();
        this.prefs.fillWithDefaults();
        VSRegisteredEvents.init(prefs);
        this.executor = Executors.newSingleThreadExecutor();
    }
    
    public SimulationResult runSimulation(String simulationFile, long maxTime) 
            throws Exception {
        // Load simulation without frame
        VSSerialize serialize = new VSSerialize();
        simulator = serialize.openSimulator(simulationFile, null);
        
        if (simulator == null) {
            throw new IllegalStateException("Failed to load simulation");
        }
        
        // Access visualization via reflection
        Field vizField = VSSimulator.class.getDeclaredField("simulatorVisualization");
        vizField.setAccessible(true);
        viz = (VSSimulatorVisualization) vizField.get(simulator);
        
        // Install log capture
        logCapture = new LogCapture();
        installLogCapture();
        
        // Run simulation
        Future<Void> runFuture = executor.submit(() -> {
            runSimulationSteps(maxTime);
            return null;
        });
        
        // Wait for completion or timeout
        try {
            runFuture.get(maxTime * 2, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            runFuture.cancel(true);
        }
        
        return new SimulationResult(
            logCapture.getCapturedLogs(),
            logCapture.getProcessLogs(),
            getSimulationMetrics()
        );
    }
    
    private void runSimulationSteps(long maxTime) throws Exception {
        VSTaskManager taskManager = viz.getTaskManager();
        Field globalTimeField = VSSimulatorVisualization.class
            .getDeclaredField("globalTime");
        globalTimeField.setAccessible(true);
        
        Method runTasksMethod = VSTaskManager.class
            .getDeclaredMethod("runTasks", long.class);
        runTasksMethod.setAccessible(true);
        
        long startTime = globalTimeField.getLong(viz);
        
        while (globalTimeField.getLong(viz) - startTime < maxTime) {
            long currentTime = globalTimeField.getLong(viz);
            runTasksMethod.invoke(taskManager, currentTime);
            
            // Advance time
            globalTimeField.setLong(viz, currentTime + 1);
            
            // Sync process times
            for (int i = 0; i < viz.getNumProcesses(); i++) {
                viz.getProcess(i).syncTime(currentTime + 1);
            }
        }
    }
    
    private void installLogCapture() throws Exception {
        // Install on visualization
        Field logingField = VSSimulatorVisualization.class
            .getDeclaredField("loging");
        logingField.setAccessible(true);
        logingField.set(viz, logCapture);
        
        // Install on all processes
        for (int i = 0; i < viz.getNumProcesses(); i++) {
            VSInternalProcess process = viz.getProcess(i);
            Field processLogingField = VSAbstractProcess.class
                .getDeclaredField("loging");
            processLogingField.setAccessible(true);
            processLogingField.set(process, logCapture);
        }
    }
    
    private SimulationMetrics getSimulationMetrics() {
        return new SimulationMetrics(
            viz.getNumProcesses(),
            logCapture.getTotalLogCount(),
            logCapture.getProcessMessageCounts()
        );
    }
    
    public void shutdown() {
        executor.shutdown();
    }
}
```

### LogCapture.java

```java
package testing;

import simulator.VSLogging;
import core.VSInternalProcess;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class LogCapture extends VSLogging {
    private final List<LogEntry> capturedLogs;
    private final Map<Integer, List<LogEntry>> processLogs;
    private final List<LogListener> listeners;
    
    public LogCapture() {
        super();
        this.capturedLogs = new CopyOnWriteArrayList<>();
        this.processLogs = new ConcurrentHashMap<>();
        this.listeners = new CopyOnWriteArrayList<>();
    }
    
    @Override
    public synchronized void log(String message) {
        // Call parent to maintain compatibility
        super.log(message);
        
        long time = simulatorVisualization != null ? 
            simulatorVisualization.getTime() : 0;
        
        LogEntry entry = new LogEntry(time, message, LogType.GLOBAL, -1);
        capturedLogs.add(entry);
        notifyListeners(entry);
    }
    
    @Override
    public synchronized void log(String message, long time) {
        super.log(message, time);
        
        LogEntry entry = new LogEntry(time, message, LogType.GLOBAL, -1);
        capturedLogs.add(entry);
        notifyListeners(entry);
    }
    
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
    }
    
    private void notifyListeners(LogEntry entry) {
        for (LogListener listener : listeners) {
            listener.onLogEntry(entry);
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
}
```

### ProtocolVerifier.java

```java
package testing;

import java.util.*;
import java.util.regex.*;
import java.util.function.Predicate;

public class ProtocolVerifier {
    private final List<VerificationRule> rules;
    
    public ProtocolVerifier() {
        this.rules = new ArrayList<>();
    }
    
    public ProtocolVerifier withRule(VerificationRule rule) {
        rules.add(rule);
        return this;
    }
    
    public ProtocolVerifier expectLog(String pattern) {
        rules.add(new PatternRule(pattern, 1, Integer.MAX_VALUE));
        return this;
    }
    
    public ProtocolVerifier expectLogExactly(String pattern, int count) {
        rules.add(new PatternRule(pattern, count, count));
        return this;
    }
    
    public ProtocolVerifier expectLogAtLeast(String pattern, int minCount) {
        rules.add(new PatternRule(pattern, minCount, Integer.MAX_VALUE));
        return this;
    }
    
    public ProtocolVerifier expectSequence(String... patterns) {
        rules.add(new SequenceRule(Arrays.asList(patterns)));
        return this;
    }
    
    public ProtocolVerifier expectNoLog(String pattern) {
        rules.add(new PatternRule(pattern, 0, 0));
        return this;
    }
    
    public VerificationResult verify(List<LogEntry> logs) {
        List<RuleResult> results = new ArrayList<>();
        
        for (VerificationRule rule : rules) {
            results.add(rule.verify(logs));
        }
        
        return new VerificationResult(results);
    }
    
    // Rule implementations
    
    private static class PatternRule implements VerificationRule {
        private final Pattern pattern;
        private final int minCount;
        private final int maxCount;
        private final String description;
        
        public PatternRule(String pattern, int minCount, int maxCount) {
            this.pattern = Pattern.compile(pattern);
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
    
    private static class SequenceRule implements VerificationRule {
        private final List<Pattern> patterns;
        private final String description;
        
        public SequenceRule(List<String> patterns) {
            this.patterns = patterns.stream()
                .map(Pattern::compile)
                .toList();
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
}
```

### Test Example: RaftProtocolTest.java

```java
package testing.protocols;

import testing.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

public class RaftProtocolTest {
    private HeadlessSimulationRunner runner;
    
    @BeforeEach
    public void setup() {
        runner = new HeadlessSimulationRunner();
    }
    
    @AfterEach
    public void teardown() {
        runner.shutdown();
    }
    
    @Test
    @DisplayName("Test Raft leader election completes within timeout")
    public void testLeaderElection() throws Exception {
        // Run simulation
        SimulationResult result = runner.runSimulation(
            "saved-simulations/raft-working.dat", 
            3000 // 3 seconds
        );
        
        // Verify leader election
        ProtocolVerifier verifier = new ProtocolVerifier()
            .expectLog("Starting election")
            .expectLog("Elected as LEADER").expectLogExactly(1)
            .expectLog("REQUEST_VOTE").expectLogAtLeast(2)
            .expectSequence("FOLLOWER", "CANDIDATE", "LEADER");
            
        VerificationResult verification = verifier.verify(result.getAllLogs());
        
        assertTrue(verification.passed(), verification.getFailureMessage());
        
        // Additional assertions
        assertTrue(result.getMetrics().getTotalLogCount() > 10, 
                  "Should have substantial log activity");
    }
    
    @Test
    @DisplayName("Test Raft handles server crashes correctly")
    public void testCrashRecovery() throws Exception {
        SimulationResult result = runner.runSimulation(
            "saved-simulations/raft-working.dat", 
            6000 // Include crash/recovery events
        );
        
        ProtocolVerifier verifier = new ProtocolVerifier()
            .expectLog("Process.*crashed")
            .expectLog("Process.*recovered")
            .expectLog("Starting new election.*timeout");
            
        VerificationResult verification = verifier.verify(result.getAllLogs());
        assertTrue(verification.passed(), verification.getFailureMessage());
    }
    
    @Test
    @DisplayName("Test Raft client requests are handled")
    public void testClientRequests() throws Exception {
        SimulationResult result = runner.runSimulation(
            "saved-simulations/raft-working.dat",
            2000
        );
        
        // Check client activation and requests
        ProtocolVerifier verifier = new ProtocolVerifier()
            .expectLog("Client.*activated")
            .expectLog("CLIENT_REQUEST|Sending request");
            
        VerificationResult verification = verifier.verify(result.getAllLogs());
        
        // May not have client requests if no leader elected yet
        if (!verification.passed()) {
            System.out.println("Note: " + verification.getFailureMessage());
        }
    }
}
```

## Usage Examples

### 1. Simple Test Case

```java
@Test
public void testBasicProtocol() throws Exception {
    HeadlessTest.runAndVerify("simulation.dat")
        .expectLog("Protocol started")
        .expectLog("Message sent")
        .expectLog("Message received");
}
```

### 2. Performance Test

```java
@Test
public void testProtocolPerformance() throws Exception {
    SimulationResult result = runner.runSimulation("perf-test.dat", 10000);
    
    // Verify throughput
    int messageCount = result.countLogs("Message processed");
    double throughput = messageCount / 10.0; // messages per second
    
    assertTrue(throughput > 100, "Throughput too low: " + throughput);
}
```

### 3. Regression Test

```java
@Test
public void testKnownScenario() throws Exception {
    SimulationResult result = runner.runSimulation("regression-test.dat", 5000);
    
    // Compare with golden output
    List<String> golden = Files.readAllLines(Paths.get("golden-output.txt"));
    List<String> actual = result.getAllLogs().stream()
        .map(LogEntry::getMessage)
        .collect(Collectors.toList());
        
    assertEquals(golden, actual, "Output differs from golden file");
}
```

## Benefits

1. **Automated Testing**: No manual GUI interaction required
2. **Reproducible**: Same simulation produces same results
3. **Fast**: No GUI overhead, can run many tests quickly
4. **CI/CD Ready**: Can be integrated into build pipelines
5. **Comprehensive**: Can verify complex protocol behaviors
6. **Debugging**: Failed tests provide detailed log traces

## Implementation Timeline

1. **Phase 1** (1-2 days): Core headless runner and log capture
2. **Phase 2** (1-2 days): Verification framework and rules
3. **Phase 3** (1 day): Test utilities and helpers
4. **Phase 4** (1 day): Example tests for existing protocols
5. **Phase 5** (1 day): Documentation and integration guides

## Next Steps

1. Review and approve the design
2. Implement core components
3. Create test suite for Raft protocol
4. Extend to other protocols
5. Integrate with Maven test lifecycle