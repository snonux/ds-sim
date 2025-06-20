package core.time;

/**
 * Interface for time representations in the distributed simulator.
 * This interface provides a common contract for different time implementations
 * including Lamport logical time and vector clocks.
 * 
 * <p>All time implementations must track the global simulation time
 * and provide a string representation for display purposes.</p>
 *
 * @see VSLamportTime
 * @see VSVectorTime
 * @author Paul C. Buetow
 */
public interface VSTime {
    /**
     * Gets the global simulation time when this time value was recorded.
     * This allows correlation between logical time and simulation time.
     *
     * @return the global simulation time in milliseconds
     */
    public long getGlobalTime();

    /**
     * Returns a string representation of this time value.
     * The format depends on the specific time implementation.
     *
     * @return string representation suitable for display or logging
     */
    public String toString();
}
