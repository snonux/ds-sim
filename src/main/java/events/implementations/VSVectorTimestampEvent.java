package events.implementations;

import core.VSInternalProcess;
import core.time.VSVectorTime;

/**
 * Concrete implementation of a Vector timestamp-triggered event.
 * This event fires when a specific Vector timestamp condition is met.
 * 
 * Example usage:
 * - Fire when vector clock equals [3,2,1]
 * - Fire when vector clock is greater than or equal to [5,0,2]
 * - Fire when any element in vector clock reaches a threshold
 *
 * @author Paul C. Buetow
 */
public class VSVectorTimestampEvent extends VSTimestampTriggeredEvent {
    
    private String actionDescription;
    private Runnable customAction;
    
    /**
     * Constructor for basic Vector timestamp event
     */
    public VSVectorTimestampEvent(VSVectorTime targetVector, ComparisonOperator op) {
        super(targetVector, op);
        this.actionDescription = "Vector timestamp condition met";
    }
    
    /**
     * Constructor with custom action description
     */
    public VSVectorTimestampEvent(VSVectorTime targetVector, ComparisonOperator op, String description) {
        super(targetVector, op);
        this.actionDescription = description;
    }
    
    /**
     * Constructor with custom action
     */
    public VSVectorTimestampEvent(VSVectorTime targetVector, ComparisonOperator op, String description, Runnable action) {
        super(targetVector, op);
        this.actionDescription = description;
        this.customAction = action;
    }
    
    /**
     * Default constructor for serialization
     */
    public VSVectorTimestampEvent() {
        super();
        this.actionDescription = "Vector timestamp event";
    }
    
    @Override
    public void onInit() {
        super.onInit();
    }
    
    @Override
    protected void onTimestampReached() {
        VSInternalProcess internalProcess = (VSInternalProcess) process;
        
        // Log the event
        String message = String.format("Vector timestamp event triggered: %s (current: %s, target: %s %s)", 
            actionDescription, 
            internalProcess.getVectorTime(), 
            targetVectorTime, 
            operator);
        
        internalProcess.log(message);
        
        // Execute custom action if provided
        if (customAction != null) {
            try {
                customAction.run();
            } catch (Exception e) {
                internalProcess.log("Error executing custom action: " + e.getMessage());
            }
        }
        
        // Default behavior: change process color to indicate trigger
        changeProcessColor();
    }
    
    /**
     * Change process color to indicate the timestamp event has been triggered
     */
    protected void changeProcessColor() {
        if (process instanceof VSInternalProcess) {
            VSInternalProcess internalProcess = (VSInternalProcess) process;
            // Change to highlight color temporarily
            internalProcess.highlightOn();
        }
    }
    
    /**
     * Convenience method to create event that triggers when any vector element reaches threshold
     */
    public static VSVectorTimestampEvent createThresholdEvent(int processCount, long threshold, String description) {
        VSVectorTime targetVector = new VSVectorTime(0);
        for (int i = 0; i < processCount; i++) {
            targetVector.add(threshold);
        }
        return new VSVectorTimestampEvent(targetVector, ComparisonOperator.GREATER_EQUAL, description);
    }
    
    /**
     * Convenience method to create event that triggers when specific process element reaches value
     */
    public static VSVectorTimestampEvent createProcessThresholdEvent(int processCount, int processIndex, long threshold, String description) {
        VSVectorTime targetVector = new VSVectorTime(0);
        for (int i = 0; i < processCount; i++) {
            targetVector.add(i == processIndex ? threshold : 0L);
        }
        return new VSVectorTimestampEvent(targetVector, ComparisonOperator.GREATER_EQUAL, description);
    }
    
    @Override
    public String toString() {
        return String.format(" [VectorTrigger: %s %s - %s]", 
            targetVectorTime, operator, actionDescription);
    }
    
    // Getters and setters
    public String getActionDescription() {
        return actionDescription;
    }
    
    public void setActionDescription(String description) {
        this.actionDescription = description;
    }
    
    public void setCustomAction(Runnable action) {
        this.customAction = action;
    }
    
    @Override
    protected String createShortname(String savedShortname) {
        return "VectorTrigger";
    }
}