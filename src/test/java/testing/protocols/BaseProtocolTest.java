package testing.protocols;

import testing.*;
import org.junit.jupiter.api.*;

/**
 * Base class for protocol tests providing common setup and utilities.
 */
public abstract class BaseProtocolTest {
    protected HeadlessSimulationRunner runner;
    
    @BeforeEach
    public void baseSetup() {
        runner = new HeadlessSimulationRunner();
    }
    
    @AfterEach
    public void baseTeardown() {
        if (runner != null) {
            runner.shutdown();
        }
    }
    
    /**
     * Run a simulation and get the result with error handling.
     */
    protected SimulationResult runSimulation(String file, long duration) {
        try {
            SimulationResult result = runner.runSimulation(file, duration);
            
            // Check if any messages were sent
            int totalMessages = result.getMetrics().getTotalMessageCount();
            if (totalMessages == 0) {
                throw new AssertionError("Protocol test failed: No messages were sent during simulation of " + file);
            }
            
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Failed to run simulation: " + file, e);
        }
    }
    
    /**
     * Create a basic verifier that checks for no errors.
     */
    protected ProtocolVerifier createBasicVerifier() {
        return new ProtocolVerifier()
            .expectNoLog("ERROR")
            .expectNoLog("Exception")
            .expectNoLog("null")
            .expectNoLog("NullPointer");
    }
}