# Timestamp-Triggered Events Guide

This guide explains how to use DS-Sim's timestamp-triggered event system to create events that fire based on logical time conditions.

## Overview

Timestamp-triggered events allow you to:
- Fire events when Lamport timestamps reach specific values
- Trigger actions based on vector clock conditions
- Create complex temporal conditions for distributed algorithms
- Visualize causality and ordering in distributed systems

## Event Types

### 1. VSLamportTimestampEvent

Triggers based on Lamport logical clock conditions.

```java
// Fire when Lamport time reaches exactly 100
VSLamportTimestampEvent event1 = new VSLamportTimestampEvent(
    100, ComparisonOperator.EQUAL, "Reached checkpoint"
);

// Fire when Lamport time exceeds 50
VSLamportTimestampEvent event2 = new VSLamportTimestampEvent(
    50, ComparisonOperator.GREATER_THAN, "Passed threshold"
);

// With custom action
VSLamportTimestampEvent event3 = new VSLamportTimestampEvent(
    200, ComparisonOperator.GREATER_EQUAL, "Major milestone",
    () -> {
        System.out.println("Milestone reached!");
        // Custom logic here
    }
);
```

### 2. VSVectorTimestampEvent

Triggers based on vector clock conditions.

```java
// Create target vector time [10, 5, 8]
VSVectorTime targetVector = new VSVectorTime(3);
targetVector.set(0, 10);
targetVector.set(1, 5);
targetVector.set(2, 8);

// Fire when vector time equals target
VSVectorTimestampEvent event1 = new VSVectorTimestampEvent(
    targetVector, ComparisonOperator.EQUAL, "Vectors synchronized"
);

// Fire when current vector >= target (happens-before relation)
VSVectorTimestampEvent event2 = new VSVectorTimestampEvent(
    targetVector, ComparisonOperator.GREATER_EQUAL, "Causality met"
);
```

### 3. VSTimestampMonitorEvent

Monitors multiple timestamp conditions simultaneously.

```java
VSTimestampMonitorEvent monitor = new VSTimestampMonitorEvent();

// Add multiple Lamport conditions
monitor.addLamportEvent(new VSLamportTimestampEvent(
    10, ComparisonOperator.EQUAL, "Early checkpoint"
));
monitor.addLamportEvent(new VSLamportTimestampEvent(
    50, ComparisonOperator.GREATER_THAN, "Mid checkpoint"
));

// Add vector conditions
monitor.addVectorEvent(new VSVectorTimestampEvent(
    targetVector, ComparisonOperator.EQUAL, "Vector match"
));
```

## Comparison Operators

| Operator | Symbol | Description | Example |
|----------|--------|-------------|---------|
| EQUAL | == | Exact match | Lamport == 100 |
| GREATER_THAN | > | Strictly greater | Lamport > 50 |
| LESS_THAN | < | Strictly less | Lamport < 30 |
| GREATER_EQUAL | >= | Greater or equal | Lamport >= 75 |
| LESS_EQUAL | <= | Less or equal | Lamport <= 25 |

## Vector Clock Comparisons

Vector clock comparisons follow the happens-before relation:

- **Equal**: All components are equal
- **Greater**: v1 >= v2 in all components AND v1 > v2 in at least one
- **Less**: v2 > v1 (inverse of greater)
- **Concurrent**: Neither v1 > v2 nor v2 > v1

## Usage Examples

### Example 1: Debugging Protocol Synchronization

```java
// In your protocol initialization
public void onServerInit() {
    // Fire when all clients have caught up
    VSVectorTime syncPoint = new VSVectorTime(getNumProcesses());
    for (int i = 0; i < getNumProcesses(); i++) {
        syncPoint.set(i, 10); // All processes at time 10
    }
    
    VSVectorTimestampEvent syncEvent = new VSVectorTimestampEvent(
        syncPoint, ComparisonOperator.GREATER_EQUAL,
        "All processes synchronized"
    );
    
    // Register with process
    VSInternalProcess internal = (VSInternalProcess) process;
    internal.getVectorClockMonitor().registerVectorEvent(syncEvent);
}
```

### Example 2: Phased Protocol Execution

```java
public class PhasedProtocol extends VSAbstractProtocol {
    
    @Override
    public void onServerInit() {
        // Phase 1: Initialization (Lamport 0-20)
        schedulePhase(20, "Phase 1 complete", this::startPhase2);
        
        // Phase 2: Data exchange (Lamport 21-50)
        schedulePhase(50, "Phase 2 complete", this::startPhase3);
        
        // Phase 3: Finalization (Lamport > 50)
        schedulePhase(100, "Protocol complete", this::finishProtocol);
    }
    
    private void schedulePhase(long timestamp, String desc, Runnable action) {
        VSLamportTimestampEvent phase = new VSLamportTimestampEvent(
            timestamp, ComparisonOperator.GREATER_EQUAL, desc, action
        );
        
        // Add to process's task manager
        VSTask task = new VSTask(0, process, phase, VSTask.LOCAL);
        // ... add task to manager
    }
}
```

### Example 3: Detecting Causality Violations

```java
// Monitor for out-of-order message delivery
VSTimestampMonitorEvent causalityMonitor = new VSTimestampMonitorEvent() {
    @Override
    protected void onTimestampReached() {
        // Check if vector times indicate causality violation
        VSVectorTime current = process.getVectorTime();
        VSVectorTime expected = getExpectedVector();
        
        if (!isValidCausalOrder(current, expected)) {
            process.log("WARNING: Causality violation detected!");
            highlightProcess();
        }
    }
};
```

### Example 4: Performance Benchmarking

```java
// Measure time to reach consensus
long startGlobal = process.getGlobalTime();

VSLamportTimestampEvent consensusReached = new VSLamportTimestampEvent(
    CONSENSUS_LAMPORT_TIME, 
    ComparisonOperator.EQUAL,
    "Consensus reached",
    () -> {
        long elapsed = process.getGlobalTime() - startGlobal;
        process.log("Consensus took " + elapsed + "ms global time");
        process.log("Lamport time: " + process.getLamportTime());
    }
);
```

## Implementation Details

### Registration and Monitoring

1. **Process Registration**: Events are registered with the process
2. **Continuous Monitoring**: Vector events are checked on every vector clock update
3. **One-Time Trigger**: Events fire only once when condition is first met
4. **No Retroactive Firing**: Events won't fire if condition was already true

### Memory Considerations

- Events are kept in memory until triggered
- Use `reset()` to reuse events
- Remove completed events to free memory
- Vector clock monitors have O(n) checking cost

### Visual Feedback

```java
@Override
protected void onTimestampReached() {
    VSInternalProcess internal = (VSInternalProcess) process;
    
    // Highlight process
    internal.highlightOn();
    
    // Log event
    internal.log("Timestamp event: " + getActionDescription());
    
    // Change process color temporarily
    scheduleColorReset(internal, 2000); // Reset after 2 seconds
}
```

## Best Practices

1. **Use Descriptive Names**: Make event descriptions clear
2. **Avoid Tight Conditions**: Use >= instead of == when possible
3. **Consider Clock Drift**: Local clocks may not advance uniformly
4. **Test Edge Cases**: Test with 1, 2, and many processes
5. **Clean Up**: Remove or reset events after use

## Troubleshooting

### Event Not Firing

1. Check operator logic (>= vs >)
2. Verify timestamp is actually increasing
3. Ensure event is properly registered
4. Check `hasTriggered` flag hasn't been set

### Multiple Firings

- Events fire only once by design
- Use `reset()` to allow re-firing
- Create new events for repeated conditions

### Performance Issues

- Limit number of active monitors
- Use Lamport events when possible (faster than vector)
- Consider sampling instead of continuous monitoring

## Advanced Usage

### Custom Timestamp Events

```java
public class ComplexTimestampEvent extends VSTimestampTriggeredEvent {
    
    @Override
    protected boolean checkCondition(VSInternalProcess process) {
        // Custom complex condition
        long lamport = process.getLamportTime();
        VSVectorTime vector = process.getVectorTime();
        
        // Example: Fire when Lamport > 50 AND vector[0] > vector[1]
        return lamport > 50 && 
               vector.get(0) > vector.get(1);
    }
    
    @Override
    protected void onTimestampReached() {
        process.log("Complex condition met!");
    }
}
```

### Chaining Events

```java
VSLamportTimestampEvent phase1 = new VSLamportTimestampEvent(
    50, ComparisonOperator.EQUAL, "Phase 1",
    () -> {
        // Phase 1 complete, schedule phase 2
        VSLamportTimestampEvent phase2 = new VSLamportTimestampEvent(
            100, ComparisonOperator.EQUAL, "Phase 2"
        );
        // Register phase2...
    }
);
```