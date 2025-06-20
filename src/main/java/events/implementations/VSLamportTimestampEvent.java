package events.implementations;

import core.VSInternalProcess;

/**
 * Concrete implementation of a Lamport timestamp-triggered event.
 * This event fires when a specific Lamport timestamp condition is met.
 * 
 * <p>This class allows you to create events that trigger based on Lamport logical time.
 * You can specify conditions such as:</p>
 * <ul>
 *   <li>Fire when Lamport time equals 10</li>
 *   <li>Fire when Lamport time reaches 50 or greater</li>
 *   <li>Fire when Lamport time is less than 5</li>
 * </ul>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * // Create event that fires when Lamport time reaches 100
 * VSLamportTimestampEvent event = new VSLamportTimestampEvent(
 *     100, ComparisonOperator.GREATER_EQUAL, "Checkpoint reached");
 * 
 * // Add custom action
 * event.setCustomAction(() -> {
 *     System.out.println("Lamport time 100 reached!");
 * });
 * }</pre>
 *
 * @see VSTimestampTriggeredEvent
 * @see VSTimestampMonitorEvent
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
            } catch (RuntimeException e) {
                // Log the error but don't let it stop the event processing
                internalProcess.log("Error executing custom action: " + e.getMessage());
                exceptions.VSErrorHandler.warning("Lamport timestamp event custom action failed", e);
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