package events.implementations;

import java.util.ArrayList;
import java.util.List;

import core.VSInternalProcess;
import core.time.VSVectorTime;

/**
 * A monitor that tracks vector clock changes and triggers events when
 * vector timestamp conditions are met. This monitor is integrated into
 * the process's vector clock update mechanism rather than being time-based.
 *
 * @author Paul C. Buetow
 */
public class VSVectorClockMonitor {
    
    private List<VSTimestampTriggeredEvent> vectorEvents;
    private VSInternalProcess process;
    private VSVectorTime lastCheckedVector;
    
    /**
     * Constructor
     */
    public VSVectorClockMonitor(VSInternalProcess process) {
        this.process = process;
        this.vectorEvents = new ArrayList<>();
        this.lastCheckedVector = null;
    }
    
    /**
     * Add a vector timestamp event to monitor
     */
    public void addVectorEvent(VSTimestampTriggeredEvent event) {
        if (event.getTimestampType() == VSTimestampTriggeredEvent.TimestampType.VECTOR && 
            !vectorEvents.contains(event)) {
            vectorEvents.add(event);
        }
    }
    
    /**
     * Remove a vector timestamp event from monitoring
     */
    public void removeVectorEvent(VSTimestampTriggeredEvent event) {
        vectorEvents.remove(event);
    }
    
    /**
     * Check all vector events when the vector clock changes.
     * This should be called whenever the process's vector clock is updated.
     */
    public void checkVectorEvents() {
        if (vectorEvents.isEmpty()) {
            return;
        }
        
        VSVectorTime currentVector = process.getVectorTime();
        
        // Only check if vector clock actually changed
        if (currentVector == null || vectorClockEquals(currentVector, lastCheckedVector)) {
            return;
        }
        
        List<VSTimestampTriggeredEvent> triggeredEvents = new ArrayList<>();
        
        for (VSTimestampTriggeredEvent event : vectorEvents) {
            if (!event.hasTriggered()) {
                // Initialize event if needed
                if (event.getProcess() == null) {
                    event.init(process);
                }
                
                // Check condition
                if (event.checkCondition(process)) {
                    event.onStart(); // This will trigger the event
                    triggeredEvents.add(event);
                    
                    process.log("Vector timestamp event triggered: " + event.toString());
                }
            }
        }
        
        // Remove triggered events
        vectorEvents.removeAll(triggeredEvents);
        
        // Update last checked vector
        lastCheckedVector = currentVector != null ? currentVector.getCopy() : null;
    }
    
    /**
     * Check if two vector clocks are equal
     */
    private boolean vectorClockEquals(VSVectorTime v1, VSVectorTime v2) {
        if (v1 == null && v2 == null) {
            return true;
        }
        if (v1 == null || v2 == null) {
            return false;
        }
        
        int maxSize = Math.max(v1.size(), v2.size());
        
        for (int i = 0; i < maxSize; i++) {
            long val1 = i < v1.size() ? v1.get(i) : 0;
            long val2 = i < v2.size() ? v2.get(i) : 0;
            
            if (val1 != val2) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Get the number of vector events being monitored
     */
    public int getVectorEventCount() {
        return vectorEvents.size();
    }
    
    /**
     * Clear all vector events
     */
    public void clearVectorEvents() {
        vectorEvents.clear();
        lastCheckedVector = null;
    }
    
    /**
     * Get a copy of the monitored events list
     */
    public List<VSTimestampTriggeredEvent> getVectorEvents() {
        return new ArrayList<>(vectorEvents);
    }
}