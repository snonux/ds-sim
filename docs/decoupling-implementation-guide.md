# DS-Sim GUI Decoupling - Implementation Guide

## Overview

This guide provides step-by-step instructions for implementing the GUI decoupling in DS-Sim to eliminate all GUI errors in headless mode.

## Key Principle

The core issue is that `VSSimulatorVisualization` extends `Canvas`, making it inherently a GUI component. Our solution extracts all simulation logic into a separate `SimulationEngine` that has no GUI dependencies.

## Implementation Steps

### Step 1: Create Core Interfaces (✓ Completed)

1. **SimulationEngine.java** - Core simulation operations
2. **SimulationVisualizer.java** - Observer interface for visualization
3. **MessageHandler.java** - Message handling abstraction

### Step 2: Implement Headless Engine (✓ Completed)

1. **AbstractSimulationEngine.java** - Base implementation
2. **HeadlessSimulationEngine.java** - Headless-specific logic

### Step 3: Modify VSInternalProcess

Current code in `VSInternalProcess.sendMessage()`:
```java
public void sendMessage(VSMessage message) {
    incSentMessages();
    simulatorVisualization.sendMessage(this, destProcess, message, delay);
}
```

Modified code:
```java
public class VSInternalProcess extends VSAbstractProcess {
    private MessageHandler messageHandler; // Injected
    
    public void sendMessage(VSMessage message) {
        incSentMessages();
        
        if (messageHandler != null) {
            messageHandler.handleMessage(message);
        } else {
            // Fallback to old behavior for compatibility
            simulatorVisualization.sendMessage(this, destProcess, message, delay);
        }
    }
    
    public void setMessageHandler(MessageHandler handler) {
        this.messageHandler = handler;
    }
}
```

### Step 4: Create Message Handler Implementations

```java
// Headless implementation
public class HeadlessMessageHandler implements MessageHandler {
    private final SimulationEngine engine;
    
    public void handleMessage(VSMessage message) {
        engine.sendMessage(message); // Pure logic, no visualization
    }
    
    public void visualizeMessage(VSMessage message) {
        // No-op in headless mode
    }
}

// Visual implementation  
public class VisualMessageHandler implements MessageHandler {
    private final SimulationEngine engine;
    private final VSSimulatorVisualization viz;
    
    public void handleMessage(VSMessage message) {
        engine.sendMessage(message);
        visualizeMessage(message);
    }
    
    public void visualizeMessage(VSMessage message) {
        if (viz.isDisplayable()) {
            // Create visual message line
            new VSMessageLine(message, viz);
        }
    }
}
```

### Step 5: Modify VSSimulatorVisualization

Change the `paint()` method to check for headless mode:

```java
public void paint() {
    // Check if we're in headless mode
    if (Boolean.getBoolean("ds.sim.headless")) {
        return; // Don't paint in headless mode
    }
    
    // Original paint code...
    while (getBufferStrategy() == null) {
        createBufferStrategy(3);
        // ...
    }
}
```

### Step 6: Update VSSimulator Constructor

```java
public VSSimulator(VSPrefs prefs, VSSimulatorFrame simulatorFrame) {
    boolean headless = simulatorFrame == null || 
                      Boolean.getBoolean("ds.sim.headless");
    
    if (headless) {
        // Create headless engine
        this.engine = new HeadlessSimulationEngine(prefs, loging);
        this.messageHandler = new HeadlessMessageHandler(engine);
    } else {
        // Create visual engine with visualization
        this.simulatorVisualization = new VSSimulatorVisualization(prefs, this, loging);
        this.engine = new VisualizableSimulationEngine(prefs, loging, simulatorVisualization);
        this.messageHandler = new VisualMessageHandler(engine, simulatorVisualization);
    }
}
```

### Step 7: Create Factory Methods

```java
public class SimulationFactory {
    public static VSSimulator createSimulator(VSPrefs prefs, boolean headless) {
        if (headless) {
            System.setProperty("ds.sim.headless", "true");
            return new VSSimulator(prefs, null);
        } else {
            VSSimulatorFrame frame = new VSSimulatorFrame(prefs, null);
            return new VSSimulator(prefs, frame);
        }
    }
}
```

## Minimal Changes for Immediate Fix

If full refactoring is too extensive, here's a minimal fix:

### Option 1: Modify VSSimulatorVisualization.paint()

Add this at the beginning of the paint() method:
```java
public void paint() {
    // Skip painting in headless mode
    if (GraphicsEnvironment.isHeadless() || 
        Boolean.getBoolean("ds.sim.headless") ||
        !isDisplayable() ||
        getParent() == null) {
        return;
    }
    
    // Original paint code...
}
```

### Option 2: Override paint() in Subclass

Create a headless subclass:
```java
public class HeadlessVisualization extends VSSimulatorVisualization {
    @Override
    public void paint() {
        // Do nothing
    }
    
    @Override
    public void sendMessage(VSMessage message) {
        // Just update counters, no visual elements
        VSInternalProcess src = getProcess(message.getSourceProcess());
        VSInternalProcess dst = getProcess(message.getDestProcess());
        if (src != null) src.incSentMessages();
        if (dst != null) dst.incReceivedMessages();
        
        // Schedule delivery without creating visual elements
        scheduleMessageDelivery(message);
    }
}
```

## Testing the Implementation

1. Run existing GUI tests to ensure compatibility
2. Run headless tests with no GUI errors:
   ```bash
   java -Dds.sim.headless=true -cp target/classes testing.EngineBasedHeadlessRunner
   ```

## Benefits of Full Implementation

1. **Clean Architecture** - Clear separation of concerns
2. **No GUI Errors** - True headless operation
3. **Better Testing** - Can unit test simulation logic without GUI
4. **Performance** - Headless mode runs faster without painting overhead
5. **Flexibility** - Easy to add new visualization types

## Risks and Mitigation

1. **Backward Compatibility**
   - Keep old methods with deprecation warnings
   - Provide adapter classes for smooth transition

2. **Serialization**
   - May need to update serialization format
   - Provide migration tools

3. **Third-party Code**
   - Document API changes clearly
   - Provide migration guide

## Conclusion

The full decoupling requires significant changes but results in a much cleaner architecture. The minimal fix options provide immediate relief from GUI errors with less risk. Choose based on available time and risk tolerance.