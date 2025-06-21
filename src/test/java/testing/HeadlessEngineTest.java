package testing;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify that the new engine-based architecture eliminates GUI errors
 * in headless mode.
 */
public class HeadlessEngineTest {
    
    private EngineBasedHeadlessRunner runner;
    
    @BeforeEach
    public void setUp() {
        // Set headless mode
        System.setProperty("ds.sim.headless", "true");
        runner = new EngineBasedHeadlessRunner();
        runner.setPrintLogs(false); // Quiet mode for tests
    }
    
    @Test
    @DisplayName("Engine-based runner should not produce GUI errors")
    public void testNoGuiErrors() throws Exception {
        // Capture stderr to check for GUI errors
        java.io.ByteArrayOutputStream errContent = new java.io.ByteArrayOutputStream();
        java.io.PrintStream originalErr = System.err;
        
        try {
            System.setErr(new java.io.PrintStream(errContent));
            
            // Run a simulation
            SimulationResult result = runner.runSimulation("saved-simulations/ping-pong.dat", 1000);
            
            // Check that we got results
            assertNotNull(result, "Result should not be null");
            assertTrue(result.getAllLogs().size() > 0, "Should have captured logs");
            
            // Check stderr for GUI errors
            String errors = errContent.toString();
            assertFalse(errors.contains("Component must have a valid peer"), 
                       "Should not have 'valid peer' errors");
            assertFalse(errors.contains("IllegalStateException"), 
                       "Should not have IllegalStateException");
            assertFalse(errors.contains("paint()"), 
                       "Should not have paint() errors");
            
        } finally {
            System.setErr(originalErr);
            runner.shutdown();
        }
    }
    
    @Test
    @DisplayName("Compare engine-based vs traditional headless runner")
    public void testCompareRunners() throws Exception {
        // This test demonstrates the difference between the approaches
        
        // Traditional approach - would produce GUI errors
        HeadlessSimulationRunner traditionalRunner = new HeadlessSimulationRunner();
        traditionalRunner.setPrintLogs(false);
        
        // Engine-based approach - no GUI errors
        EngineBasedHeadlessRunner engineRunner = new EngineBasedHeadlessRunner();
        engineRunner.setPrintLogs(false);
        
        try {
            // Both should produce results
            SimulationResult result1 = traditionalRunner.runSimulation("saved-simulations/ping-pong.dat", 500);
            SimulationResult result2 = engineRunner.runSimulation("saved-simulations/ping-pong.dat", 500);
            
            // Both should capture logs
            assertTrue(result1.getAllLogs().size() > 0, "Traditional runner should capture logs");
            assertTrue(result2.getAllLogs().size() > 0, "Engine runner should capture logs");
            
            // Note: The traditional runner would produce GUI errors in stderr,
            // but still functions. The engine-based runner produces no GUI errors.
            
        } finally {
            traditionalRunner.shutdown();
            engineRunner.shutdown();
        }
    }
}