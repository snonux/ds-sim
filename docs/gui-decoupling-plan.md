# DS-Sim GUI Decoupling Plan

## Problem Analysis

### Current Architecture Issues

1. **VSSimulatorVisualization extends Canvas**
   - Inherits from java.awt.Canvas, making it inherently a GUI component
   - paint() method is called automatically by AWT/Swing framework
   - Cannot function without a valid GUI peer in headless mode

2. **Tight Coupling Points**
   ```
   Protocol → Process.sendMessage() → Visualization.sendMessage() → VSMessageLine → paint()
                                   ↓
                             Creates visual elements
                             Triggers canvas repaint
   ```

3. **Violations of Separation of Concerns**
   - Business logic (simulation) mixed with presentation (visualization)
   - Message passing logic coupled with visual message lines
   - Process state management tied to canvas updates

## Decoupling Strategy

### Phase 1: Create Abstraction Layer

#### 1.1 Define Core Interfaces

```java
// Core simulation interface
public interface SimulationEngine {
    void sendMessage(VSMessage message);
    void addProcess(VSInternalProcess process);
    void removeProcess(VSInternalProcess process);
    List<VSInternalProcess> getProcesses();
    VSTaskManager getTaskManager();
    long getTime();
    void setTime(long time);
    void reset();
    void play();
    void pause();
}

// Visualization interface (optional)
public interface SimulationVisualizer {
    void onMessageSent(VSMessage message);
    void onProcessAdded(VSInternalProcess process);
    void onProcessRemoved(VSInternalProcess process);
    void onTimeChanged(long time);
    void onSimulationReset();
    void onSimulationStarted();
    void onSimulationPaused();
}

// Message handler interface
public interface MessageHandler {
    void handleMessage(VSMessage message);
    void visualizeMessage(VSMessage message); // Optional
}
```

#### 1.2 Create Headless Implementation

```java
public class HeadlessSimulationEngine implements SimulationEngine {
    private final List<VSInternalProcess> processes;
    private final VSTaskManager taskManager;
    private final List<SimulationVisualizer> visualizers;
    private long time;
    
    public void sendMessage(VSMessage message) {
        // Pure logic - no visualization
        message.updateTimestamps();
        
        // Notify visualizers (if any)
        for (SimulationVisualizer viz : visualizers) {
            viz.onMessageSent(message);
        }
        
        // Process the message
        deliverMessage(message);
    }
}
```

### Phase 2: Refactor VSSimulatorVisualization

#### 2.1 Extract Simulation Logic

Create new class hierarchy:
```
SimulationEngine (interface)
├── AbstractSimulationEngine
│   ├── HeadlessSimulationEngine
│   └── VisualizableSimulationEngine
```

#### 2.2 Refactor VSSimulatorVisualization

```java
public class VSSimulatorVisualization extends Canvas implements SimulationVisualizer {
    private SimulationEngine engine; // Composition instead of doing everything
    
    @Override
    public void onMessageSent(VSMessage message) {
        if (isDisplayable() && getBufferStrategy() != null) {
            createMessageLine(message);
            repaint();
        }
    }
    
    // Delegate simulation operations to engine
    public void sendMessage(VSMessage message) {
        engine.sendMessage(message);
    }
}
```

### Phase 3: Refactor Message Handling

#### 3.1 Separate Message Logic from Visualization

```java
public class MessageDispatcher {
    private final Map<Integer, VSInternalProcess> processes;
    
    public void dispatchMessage(VSMessage message) {
        VSInternalProcess destination = processes.get(message.getDestinationId());
        if (destination != null) {
            destination.receiveMessage(message);
        }
    }
}

public class VisualMessageHandler implements MessageHandler {
    private final MessageDispatcher dispatcher;
    private final Canvas canvas;
    
    public void handleMessage(VSMessage message) {
        dispatcher.dispatchMessage(message);
        visualizeMessage(message);
    }
    
    public void visualizeMessage(VSMessage message) {
        if (canvas != null && canvas.isDisplayable()) {
            new VSMessageLine(message, canvas);
        }
    }
}

public class HeadlessMessageHandler implements MessageHandler {
    private final MessageDispatcher dispatcher;
    
    public void handleMessage(VSMessage message) {
        dispatcher.dispatchMessage(message);
    }
    
    public void visualizeMessage(VSMessage message) {
        // No-op in headless mode
    }
}
```

### Phase 4: Modify Core Classes

#### 4.1 Update VSInternalProcess

```java
public class VSInternalProcess extends VSAbstractProcess {
    private MessageHandler messageHandler; // Injected
    
    public void sendMessage(VSMessage message) {
        incSentMessages();
        messageHandler.handleMessage(message);
    }
}
```

#### 4.2 Create Factory for Mode Selection

```java
public class SimulationFactory {
    public static SimulationEngine createEngine(boolean headless) {
        if (headless) {
            return new HeadlessSimulationEngine();
        } else {
            return new VisualizableSimulationEngine();
        }
    }
    
    public static MessageHandler createMessageHandler(boolean headless, 
                                                     MessageDispatcher dispatcher,
                                                     Canvas canvas) {
        if (headless) {
            return new HeadlessMessageHandler(dispatcher);
        } else {
            return new VisualMessageHandler(dispatcher, canvas);
        }
    }
}
```

### Phase 5: Integration Points

#### 5.1 Modify VSSimulator

```java
public class VSSimulator extends JPanel {
    private final SimulationEngine engine;
    private final VSSimulatorVisualization visualization; // Optional
    
    public VSSimulator(VSPrefs prefs, VSSimulatorFrame frame) {
        boolean headless = System.getProperty("ds.sim.headless", "false").equals("true");
        
        this.engine = SimulationFactory.createEngine(headless);
        
        if (!headless && frame != null) {
            this.visualization = new VSSimulatorVisualization(prefs, this, engine);
            engine.addVisualizer(visualization);
        }
    }
}
```

#### 5.2 Update Serialization

```java
public class VSSerialize {
    public VSSimulator openSimulator(String filename, VSSimulatorFrame frame) {
        // Detect headless mode
        boolean headless = frame == null || 
                          System.getProperty("ds.sim.headless", "false").equals("true");
        
        // Load with appropriate components
        if (headless) {
            return loadHeadlessSimulator(filename);
        } else {
            return loadVisualSimulator(filename, frame);
        }
    }
}
```

## Implementation Steps

1. **Create new package structure**
   ```
   simulator.engine/
   ├── SimulationEngine.java
   ├── AbstractSimulationEngine.java
   ├── HeadlessSimulationEngine.java
   └── VisualizableSimulationEngine.java
   
   simulator.messaging/
   ├── MessageHandler.java
   ├── MessageDispatcher.java
   ├── HeadlessMessageHandler.java
   └── VisualMessageHandler.java
   
   simulator.visualization/
   ├── SimulationVisualizer.java
   └── VSMessageLine.java (moved)
   ```

2. **Gradual refactoring approach**
   - Start with message handling
   - Extract simulation logic from VSSimulatorVisualization
   - Create headless implementations
   - Update dependent classes
   - Maintain backward compatibility

3. **Testing strategy**
   - Create unit tests for new components
   - Ensure existing GUI functionality still works
   - Verify headless mode has zero GUI dependencies

## Benefits

1. **Clean Architecture**
   - Separation of concerns
   - Testable components
   - Flexible deployment options

2. **True Headless Operation**
   - No GUI errors in headless mode
   - Faster test execution
   - Suitable for CI/CD pipelines

3. **Maintainability**
   - Clear interfaces
   - Easier to extend
   - Better code organization

## Risks and Mitigation

1. **Breaking Changes**
   - Mitigation: Use adapter pattern to maintain compatibility
   - Provide migration guide

2. **Performance Impact**
   - Mitigation: Profile and optimize critical paths
   - Use efficient data structures

3. **Complexity**
   - Mitigation: Incremental implementation
   - Comprehensive documentation

## Timeline Estimate

- Phase 1: 2-3 days (interfaces and abstractions)
- Phase 2: 3-4 days (refactor VSSimulatorVisualization)
- Phase 3: 2-3 days (message handling)
- Phase 4: 3-4 days (core class updates)
- Phase 5: 2-3 days (integration and testing)

Total: 12-17 days for complete implementation