package core.time;

/**
 * Immutable record representing a Lamport timestamp in the distributed system.
 * Lamport timestamps provide a partial ordering of events in a distributed system.
 * 
 * <p>Each timestamp contains:</p>
 * <ul>
 *   <li>Global time - the simulation time when this timestamp was created</li>
 *   <li>Lamport time - the logical clock value following Lamport's algorithm</li>
 * </ul>
 * 
 * <p>This record automatically provides {@code equals()}, {@code hashCode()}, 
 * and {@code toString()} implementations.</p>
 *
 * @param globalTime the global simulation time when this timestamp was recorded
 * @param lamportTime the Lamport logical clock value
 * 
 * @author Paul C. Buetow
 */
public record VSLamportTime(long globalTime, long lamportTime) implements VSTime {
    
    /**
     * Gets the global time (implements VSTime interface).
     *
     * @return the global simulation time
     */
    @Override
    public long getGlobalTime() {
        return globalTime;
    }
    
    /**
     * Gets the Lamport time value.
     *
     * @return the Lamport logical clock value
     * @deprecated Use {@link #lamportTime()} instead
     */
    @Deprecated
    public long getLamportTime() {
        return lamportTime;
    }
    
    /**
     * Returns a string representation of this Lamport timestamp.
     * Format: "(lamportTime)"
     *
     * @return string representation of the Lamport time
     */
    @Override
    public String toString() {
        return "(" + lamportTime + ")";
    }
}