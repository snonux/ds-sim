package simulator.engine;

import core.VSInternalProcess;
import core.VSMessage;

/**
 * Interface for visualization components that observe simulation events.
 * Implementations can choose to display these events visually or ignore them.
 */
public interface SimulationVisualizer {
    
    /**
     * Called when a message is sent in the simulation.
     * @param message The message that was sent
     */
    void onMessageSent(VSMessage message);
    
    /**
     * Called when a process is added to the simulation.
     * @param process The process that was added
     */
    void onProcessAdded(VSInternalProcess process);
    
    /**
     * Called when a process is removed from the simulation.
     * @param process The process that was removed
     */
    void onProcessRemoved(VSInternalProcess process);
    
    /**
     * Called when the simulation time changes.
     * @param time The new time value
     */
    void onTimeChanged(long time);
    
    /**
     * Called when the simulation is reset.
     */
    void onSimulationReset();
    
    /**
     * Called when the simulation starts or resumes.
     */
    void onSimulationStarted();
    
    /**
     * Called when the simulation is paused.
     */
    void onSimulationPaused();
    
    /**
     * Called when the simulation finishes.
     */
    void onSimulationFinished();
    
    /**
     * Called when a process state changes.
     * @param process The process whose state changed
     */
    void onProcessStateChanged(VSInternalProcess process);
}