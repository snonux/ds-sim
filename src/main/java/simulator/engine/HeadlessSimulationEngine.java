package simulator.engine;

import core.*;
import prefs.VSPrefs;
import events.internal.VSMessageReceiveEvent;
import simulator.VSLogging;

/**
 * Headless implementation of the simulation engine that runs without any GUI dependencies.
 * This engine focuses purely on simulation logic without any visualization concerns.
 */
public class HeadlessSimulationEngine extends AbstractSimulationEngine {
    
    public HeadlessSimulationEngine(VSPrefs prefs, VSLogging loging) {
        super(prefs, loging);
    }
    
    @Override
    protected long calculateDeliveryTime(VSMessage message) {
        // Get source process
        VSInternalProcess source = (VSInternalProcess) message.getSendingProcess();
        
        if (source == null) {
            return time; // Deliver immediately if process not found
        }
        
        // Calculate network delay
        long networkDelay = prefs.getLong("sim.network.delay");
        long variability = prefs.getLong("sim.network.variability");
        
        // Add random variability
        if (variability > 0) {
            long variance = (long)(Math.random() * variability * 2) - variability;
            networkDelay += variance;
        }
        
        // Ensure minimum delay
        if (networkDelay < 0) {
            networkDelay = 0;
        }
        
        // Calculate delivery time based on source process time
        return source.getTime() + networkDelay;
    }
    
    @Override
    protected void scheduleMessageDelivery(VSMessage message, long deliveryTime) {
        // In DS-Sim, messages are broadcast to all processes
        VSInternalProcess sendingProcess = (VSInternalProcess) message.getSendingProcess();
        boolean recvOwn = prefs.getBoolean("sim.message.own.recv");
        
        // Schedule delivery to all processes
        for (VSInternalProcess receiverProcess : processes) {
            if (receiverProcess.equals(sendingProcess)) {
                // Only deliver to self if configured
                if (!recvOwn) {
                    continue;
                }
            }
            
            // Create receive event for this process
            VSMessageReceiveEvent receiveEvent = new VSMessageReceiveEvent(message);
            VSTask task = new VSTask(deliveryTime, receiverProcess, receiveEvent, VSTask.GLOBAL);
            taskManager.addTask(task);
            
            if (loging != null) {
                loging.log("Message scheduled for delivery to process " + 
                          receiverProcess.getProcessNum() + "; ID: " + 
                          message.getMessageID() + "; Time: " + deliveryTime);
            }
        }
    }
    
    /**
     * Run one simulation step.
     * This method advances time and executes all tasks scheduled for the current time.
     */
    public void runStep() {
        if (isPaused || hasFinished) {
            return;
        }
        
        // Sync all process times
        for (VSInternalProcess process : processes) {
            process.syncTime(time);
        }
        
        // Run tasks for current time
        taskManager.runTasks(time, 0, time - 1);
        
        // Check if simulation has finished
        // TODO: Implement proper finish detection
        // For now, rely on external control or time limits
    }
    
    /**
     * Run the simulation for a specified duration.
     * @param duration Duration in milliseconds
     */
    public void runFor(long duration) {
        long endTime = time + duration;
        
        while (time < endTime && !hasFinished) {
            runStep();
            time++;
        }
    }
    
    /**
     * Check if any protocols are still active.
     * TODO: Implement this when protocol tracking is available
     */
    private boolean hasActiveProtocols() {
        return false; // For now, assume no active protocols
    }
}