package core;

/**
 * Configuration record for simulation parameters using Java 21 records.
 * This immutable configuration object encapsulates all simulation settings.
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * var config = VSSimulationConfig.defaultConfig();
 * var customConfig = new VSSimulationConfig(
 *     5,        // processes
 *     100,      // timestep
 *     10000,    // duration
 *     true,     // logging enabled
 *     0.1f      // clock variance
 * );
 * }</pre>
 * 
 * @param numProcesses number of processes in the simulation (1-6)
 * @param timestepMs simulation timestep in milliseconds
 * @param maxDurationMs maximum simulation duration in milliseconds
 * @param loggingEnabled whether logging is enabled
 * @param defaultClockVariance default clock variance for processes
 * 
 * @author Paul C. Buetow
 */
public record VSSimulationConfig(
    int numProcesses,
    long timestepMs,
    long maxDurationMs,
    boolean loggingEnabled,
    float defaultClockVariance
) {
    
    /**
     * Validates the configuration parameters.
     * Called automatically by the record's canonical constructor.
     */
    public VSSimulationConfig {
        if (numProcesses < constants.VSConstants.MIN_PROCESSES || 
            numProcesses > constants.VSConstants.MAX_PROCESSES) {
            throw new IllegalArgumentException(
                "Number of processes must be between %d and %d"
                    .formatted(constants.VSConstants.MIN_PROCESSES, 
                              constants.VSConstants.MAX_PROCESSES)
            );
        }
        if (timestepMs <= 0) {
            throw new IllegalArgumentException("Timestep must be positive");
        }
        if (maxDurationMs <= 0) {
            throw new IllegalArgumentException("Max duration must be positive");
        }
        if (defaultClockVariance < -1.0f || defaultClockVariance > 1.0f) {
            throw new IllegalArgumentException("Clock variance must be between -1.0 and 1.0");
        }
    }
    
    /**
     * Creates a default configuration suitable for most simulations.
     * 
     * @return default simulation configuration
     */
    public static VSSimulationConfig defaultConfig() {
        return new VSSimulationConfig(
            constants.VSConstants.DEFAULT_PROCESSES,
            100,      // 100ms timestep
            60000,    // 60 second max duration
            true,     // logging enabled
            0.0f      // no clock variance
        );
    }
    
    /**
     * Creates a new configuration with a different number of processes.
     * 
     * @param newNumProcesses the new number of processes
     * @return new configuration with updated process count
     */
    public VSSimulationConfig withProcesses(int newNumProcesses) {
        return new VSSimulationConfig(
            newNumProcesses,
            timestepMs,
            maxDurationMs,
            loggingEnabled,
            defaultClockVariance
        );
    }
    
    /**
     * Creates a new configuration with different timing parameters.
     * 
     * @param newTimestep the new timestep in milliseconds
     * @param newMaxDuration the new maximum duration in milliseconds
     * @return new configuration with updated timing
     */
    public VSSimulationConfig withTiming(long newTimestep, long newMaxDuration) {
        return new VSSimulationConfig(
            numProcesses,
            newTimestep,
            newMaxDuration,
            loggingEnabled,
            defaultClockVariance
        );
    }
}