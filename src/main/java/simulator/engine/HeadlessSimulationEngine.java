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
        
        // Use the process's getDurationTime method to get the message duration
        // This respects the message.sendingtime.min and message.sendingtime.max preferences
        long durationTime = source.getDurationTime();
        
        // Calculate delivery time based on source process's global time + duration
        return source.getGlobalTime() + durationTime;
    }
    
    @Override
    protected void scheduleMessageDelivery(VSMessage message, long deliveryTime) {
        // In DS-Sim, messages are broadcast to all processes
        VSInternalProcess sendingProcess = (VSInternalProcess) message.getSendingProcess();
        boolean recvOwn = prefs.getBoolean("sim.message.own.recv");
        
        // Debug logging
        if (loging != null) {
            loging.log("Message " + message.getMessageID() + " scheduled for delivery at time " + 
                      deliveryTime + " (sent at globalTime=" + sendingProcess.getGlobalTime() + 
                      ", duration=" + (deliveryTime - sendingProcess.getGlobalTime()) + "ms)");
        }
        
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