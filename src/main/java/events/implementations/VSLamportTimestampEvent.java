package events.implementations;

import core.VSInternalProcess;

/**
 * Concrete implementation of a Lamport timestamp-triggered event.
 * This event fires when a specific Lamport timestamp condition is met.
 * 
 * Example usage:
 * - Fire when Lamport time equals 10
 * - Fire when Lamport time reaches 50 or greater
 * - Fire when Lamport time is less than 5
 *
 * @author Paul C. Buetow
 */
public class VSLamportTimestampEvent extends VSTimestampTriggeredEvent {
    
    private String actionDescription;
    private Runnable customAction;
    
    /**
     * Constructor for basic Lamport timestamp event
     */
    public VSLamportTimestampEvent(long targetLamport, ComparisonOperator op) {
        super(targetLamport, op);
        this.actionDescription = "Lamport timestamp condition met";
    }
    
    /**
     * Constructor with custom action description
     */
    public VSLamportTimestampEvent(long targetLamport, ComparisonOperator op, String description) {
        super(targetLamport, op);
        this.actionDescription = description;
    }
    
    /**
     * Constructor with custom action
     */
    public VSLamportTimestampEvent(long targetLamport, ComparisonOperator op, String description, Runnable action) {
        super(targetLamport, op);
        this.actionDescription = description;
        this.customAction = action;
    }
    
    /**
     * Default constructor for serialization
     */
    public VSLamportTimestampEvent() {
        super();
        this.actionDescription = "Lamport timestamp event";
    }
    
    @Override
    public void onInit() {
        super.onInit();
    }
    
    @Override
    protected void onTimestampReached() {
        VSInternalProcess internalProcess = (VSInternalProcess) process;
        
        // Log the event
        String message = String.format("Lamport timestamp event triggered: %s (current: %d, target: %d %s)", 
            actionDescription, 
            internalProcess.getLamportTime(), 
            targetLamportTime, 
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
    
    @Override
    public String toString() {
        return String.format(" [LamportTrigger: %d %s - %s]", 
            targetLamportTime, operator, actionDescription);
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
        return "LamportTrigger";
    }
}