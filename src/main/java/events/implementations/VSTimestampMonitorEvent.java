package events.implementations;

import java.util.ArrayList;
import java.util.List;

import core.VSInternalProcess;
import core.VSTask;
import events.VSAbstractEvent;
import events.VSCopyableEvent;

/**
 * A monitoring event that checks for Lamport timestamp conditions.
 * Vector timestamp events should use VSVectorClockMonitor instead,
 * as they need to be checked when vector clocks change, not on time intervals.
 * 
 * This event reschedules itself to run periodically, monitoring only
 * Lamport timestamp events for the process.
 *
 * @author Paul C. Buetow
 */
public class VSTimestampMonitorEvent extends VSAbstractEvent implements VSCopyableEvent {
    
    private List<VSTimestampTriggeredEvent> lamportEvents;
    private long monitorInterval;
    private boolean isActive;
    
    /**
     * Constructor with default monitoring interval
     */
    public VSTimestampMonitorEvent() {
        this.lamportEvents = new ArrayList<>();
        this.monitorInterval = constants.VSConstants.DEFAULT_MONITOR_INTERVAL;
        this.isActive = true;
    }
    
    /**
     * Constructor with custom monitoring interval
     */
    public VSTimestampMonitorEvent(long interval) {
        this.lamportEvents = new ArrayList<>();
        this.monitorInterval = interval;
        this.isActive = true;
    }
    
    @Override
    public void onInit() {
        setClassname(getClass().getName());
    }
    
    @Override
    public void onStart() {
        if (!isActive) {
            return;
        }
        
        VSInternalProcess internalProcess = (VSInternalProcess) process;
        
        // Check only Lamport timestamp events (vector events are handled separately)
        List<VSTimestampTriggeredEvent> triggeredEvents = new ArrayList<>();
        
        for (VSTimestampTriggeredEvent event : lamportEvents) {
            if (!event.hasTriggered() && 
                event.getTimestampType() == VSTimestampTriggeredEvent.TimestampType.LAMPORT) {
                
                // Initialize the event if needed
                if (event.getProcess() == null) {
                    event.init(internalProcess);
                }
                
                // Check if condition is met
                if (event.checkCondition(internalProcess)) {
                    event.onStart(); // This will trigger the event
                    triggeredEvents.add(event);
                }
            }
        }
        
        // Remove triggered events from monitoring list
        lamportEvents.removeAll(triggeredEvents);
        
        // Reschedule this monitor if there are still events to monitor
        if (!lamportEvents.isEmpty()) {
            rescheduleMonitor();
        }
    }
    
    /**
     * Add a Lamport timestamp event to monitor
     */
    public void addLamportEvent(VSTimestampTriggeredEvent event) {
        if (event.getTimestampType() == VSTimestampTriggeredEvent.TimestampType.LAMPORT &&
            !lamportEvents.contains(event)) {
            lamportEvents.add(event);
            
            // If this is the first event, start monitoring
            if (lamportEvents.size() == 1 && process != null) {
                rescheduleMonitor();
            }
        }
    }
    
    /**
     * Remove a Lamport timestamp event from monitoring
     */
    public void removeLamportEvent(VSTimestampTriggeredEvent event) {
        lamportEvents.remove(event);
    }
    
    /**
     * Schedule the next monitoring check
     */
    private void rescheduleMonitor() {
        if (process instanceof VSInternalProcess) {
            VSInternalProcess internalProcess = (VSInternalProcess) process;
            
            // Create a new monitor task for the next interval
            VSTimestampMonitorEvent nextMonitor = new VSTimestampMonitorEvent(monitorInterval);
            nextMonitor.lamportEvents = new ArrayList<>(this.lamportEvents);
            nextMonitor.isActive = this.isActive;
            
            // Schedule as local timed task
            long nextTime = internalProcess.getTime() + monitorInterval;
            VSTask monitorTask = new VSTask(nextTime, internalProcess, nextMonitor, VSTask.LOCAL);
            
            internalProcess.getSimulatorCanvas().getTaskManager().addTask(monitorTask);
        }
    }
    
    /**
     * Stop monitoring
     */
    public void stopMonitoring() {
        isActive = false;
        lamportEvents.clear();
    }
    
    /**
     * Get count of Lamport events being monitored
     */
    public int getLamportEventCount() {
        return lamportEvents.size();
    }
    
    /**
     * Check if monitoring is active
     */
    public boolean isActive() {
        return isActive && !lamportEvents.isEmpty();
    }
    
    @Override
    public void initCopy(VSAbstractEvent copy) {
        if (copy instanceof VSTimestampMonitorEvent) {
            VSTimestampMonitorEvent copyEvent = (VSTimestampMonitorEvent) copy;
            copyEvent.monitorInterval = this.monitorInterval;
            copyEvent.isActive = this.isActive;
            copyEvent.lamportEvents = new ArrayList<>(this.lamportEvents);
        }
    }
    
    @Override
    public String toString() {
        return String.format(" [LamportMonitor: %d events, interval=%d]", 
            lamportEvents.size(), monitorInterval);
    }
    
    // Getters and setters
    public long getMonitorInterval() {
        return monitorInterval;
    }
    
    public void setMonitorInterval(long interval) {
        this.monitorInterval = interval;
    }
    
    public List<VSTimestampTriggeredEvent> getLamportEvents() {
        return new ArrayList<>(lamportEvents);
    }
    
    @Override
    protected String createShortname(String savedShortname) {
        return "TimestampMonitor";
    }
}