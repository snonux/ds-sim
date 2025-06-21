package simulator.engine;

import core.*;
import prefs.VSPrefs;
import simulator.VSLogging;
import events.internal.VSMessageReceiveEvent;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Abstract base implementation of SimulationEngine that provides common
 * functionality for both headless and visual simulation engines.
 */
public abstract class AbstractSimulationEngine implements SimulationEngine {
    
    protected final VSPrefs prefs;
    protected final List<VSInternalProcess> processes;
    protected final List<SimulationVisualizer> visualizers;
    protected VSTaskManager taskManager;
    protected VSLogging loging;
    
    protected long time;
    protected boolean isPaused;
    protected boolean isResetted;
    protected boolean hasFinished;
    
    public AbstractSimulationEngine(VSPrefs prefs, VSLogging loging) {
        this.prefs = prefs;
        this.loging = loging;
        this.processes = new ArrayList<>();
        this.visualizers = new CopyOnWriteArrayList<>();
        this.taskManager = new VSTaskManager(prefs, null); // We'll inject visualization later
        this.time = 0;
        this.isPaused = true;
        this.isResetted = true;
        this.hasFinished = false;
    }
    
    @Override
    public void sendMessage(VSMessage message) {
        // Calculate proper delivery time
        long deliveryTime = calculateDeliveryTime(message);
        
        // Schedule message delivery to all processes (broadcast model)
        scheduleMessageDelivery(message, deliveryTime);
        
        // Notify visualizers
        for (SimulationVisualizer visualizer : visualizers) {
            visualizer.onMessageSent(message);
        }
    }
    
    protected abstract long calculateDeliveryTime(VSMessage message);
    
    protected abstract void scheduleMessageDelivery(VSMessage message, long deliveryTime);
    
    @Override
    public void addProcess(VSInternalProcess process) {
        processes.add(process);
        
        // Notify visualizers
        for (SimulationVisualizer visualizer : visualizers) {
            visualizer.onProcessAdded(process);
        }
    }
    
    @Override
    public void removeProcess(VSInternalProcess process) {
        processes.remove(process);
        
        // Notify visualizers
        for (SimulationVisualizer visualizer : visualizers) {
            visualizer.onProcessRemoved(process);
        }
    }
    
    @Override
    public List<VSInternalProcess> getProcesses() {
        return new ArrayList<>(processes);
    }
    
    @Override
    public VSInternalProcess getProcess(int index) {
        if (index >= 0 && index < processes.size()) {
            return processes.get(index);
        }
        return null;
    }
    
    @Override
    public int getNumProcesses() {
        return processes.size();
    }
    
    @Override
    public VSTaskManager getTaskManager() {
        return taskManager;
    }
    
    /**
     * Set the task manager for this engine.
     * Used when integrating with existing simulation infrastructure.
     */
    public void setTaskManager(VSTaskManager taskManager) {
        this.taskManager = taskManager;
    }
    
    @Override
    public long getTime() {
        return time;
    }
    
    @Override
    public void setTime(long time) {
        this.time = time;
        
        // Notify visualizers
        for (SimulationVisualizer visualizer : visualizers) {
            visualizer.onTimeChanged(time);
        }
    }
    
    @Override
    public void reset() {
        // Reset state
        isResetted = true;
        isPaused = true;
        hasFinished = false;
        time = 0;
        
        // Reset all processes
        for (VSInternalProcess process : processes) {
            process.reset();
        }
        
        // Reset task manager
        taskManager.reset();
        
        // Notify visualizers
        for (SimulationVisualizer visualizer : visualizers) {
            visualizer.onSimulationReset();
        }
    }
    
    @Override
    public void play() {
        isPaused = false;
        isResetted = false;
        
        // Notify visualizers
        for (SimulationVisualizer visualizer : visualizers) {
            visualizer.onSimulationStarted();
        }
    }
    
    @Override
    public void pause() {
        isPaused = true;
        
        // Notify visualizers
        for (SimulationVisualizer visualizer : visualizers) {
            visualizer.onSimulationPaused();
        }
    }
    
    @Override
    public boolean isPaused() {
        return isPaused;
    }
    
    @Override
    public boolean isResetted() {
        return isResetted;
    }
    
    @Override
    public boolean hasFinished() {
        return hasFinished;
    }
    
    @Override
    public void setFinished(boolean finished) {
        this.hasFinished = finished;
        
        if (finished) {
            // Notify visualizers
            for (SimulationVisualizer visualizer : visualizers) {
                visualizer.onSimulationFinished();
            }
        }
    }
    
    @Override
    public void addVisualizer(SimulationVisualizer visualizer) {
        visualizers.add(visualizer);
    }
    
    @Override
    public void removeVisualizer(SimulationVisualizer visualizer) {
        visualizers.remove(visualizer);
    }
    
    /**
     * Set the logging instance.
     */
    public void setLogging(VSLogging loging) {
        this.loging = loging;
    }
}