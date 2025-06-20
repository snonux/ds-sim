package events;

/**
 * Interface for events that support copying.
 * Events that implement this interface can be duplicated, which is useful
 * for creating multiple instances of the same event or for event scheduling.
 * 
 * <p>To make an event copyable:</p>
 * <ol>
 *   <li>Implement this interface</li>
 *   <li>Override initCopy() to copy all event-specific state</li>
 *   <li>The framework will handle creating the new instance</li>
 * </ol>
 * 
 * <p>Events that don't implement this interface will throw
 * {@link exceptions.VSEventNotCopyableException} when copy is attempted.</p>
 *
 * @see VSAbstractEvent#getCopy()
 * @see exceptions.VSEventNotCopyableException
 * @author Paul C. Buetow
 */
public interface VSCopyableEvent {
    /**
     * Initializes a copy of this event with all necessary state.
     * This method should copy all event-specific fields to the provided copy.
     * The copy will already be initialized with the same process and basic properties.
     *
     * @param copy the event instance to initialize with this event's state
     */
    public void initCopy(VSAbstractEvent copy);
}
