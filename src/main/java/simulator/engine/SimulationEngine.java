package simulator.engine;

import core.VSInternalProcess;
import core.VSTaskManager;
import core.VSMessage;
import java.util.List;

/**
 * Core simulation engine interface that defines all simulation operations
 * without any GUI dependencies.
 */
public interface SimulationEngine {
    
    /**
     * Send a message between processes.
     * @param message The message to send
     */
    void sendMessage(VSMessage message);
    
    /**
     * Add a process to the simulation.
     * @param process The process to add
     */
    void addProcess(VSInternalProcess process);
    
    /**
     * Remove a process from the simulation.
     * @param process The process to remove
     */
    void removeProcess(VSInternalProcess process);
    
    /**
     * Get all processes in the simulation.
     * @return List of processes
     */
    List<VSInternalProcess> getProcesses();
    
    /**
     * Get a specific process by index.
     * @param index The process index
     * @return The process or null if not found
     */
    VSInternalProcess getProcess(int index);
    
    /**
     * Get the number of processes.
     * @return Process count
     */
    int getNumProcesses();
    
    /**
     * Get the task manager.
     * @return The task manager
     */
    VSTaskManager getTaskManager();
    
    /**
     * Get the current simulation time.
     * @return Current time in milliseconds
     */
    long getTime();
    
    /**
     * Set the simulation time.
     * @param time Time in milliseconds
     */
    void setTime(long time);
    
    /**
     * Reset the simulation to initial state.
     */
    void reset();
    
    /**
     * Start or resume the simulation.
     */
    void play();
    
    /**
     * Pause the simulation.
     */
    void pause();
    
    /**
     * Check if simulation is paused.
     * @return true if paused
     */
    boolean isPaused();
    
    /**
     * Check if simulation has been reset.
     * @return true if reset
     */
    boolean isResetted();
    
    /**
     * Check if simulation has finished.
     * @return true if finished
     */
    boolean hasFinished();
    
    /**
     * Set finished state.
     * @param finished The finished state
     */
    void setFinished(boolean finished);
    
    /**
     * Add a visualization observer.
     * @param visualizer The visualizer to add
     */
    void addVisualizer(SimulationVisualizer visualizer);
    
    /**
     * Remove a visualization observer.
     * @param visualizer The visualizer to remove
     */
    void removeVisualizer(SimulationVisualizer visualizer);
}