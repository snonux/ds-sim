package events;

/**
 * The interface VSCopyableEvent, all events which implement this class
 * are copyable.
 *
 * @author Paul C. Buetow
 */
public interface VSCopyableEvent {
    /**
     * Fills a copy of this event with its values
     *
     * @param copy The copy
     */
    public void initCopy(VSAbstractEvent copy);
}
