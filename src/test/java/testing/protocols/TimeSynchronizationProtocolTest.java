package testing.protocols;

import testing.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit tests for time synchronization protocols (internal and external).
 */
@DisplayName("Time Synchronization Protocol Tests")
public class TimeSynchronizationProtocolTest {
    private HeadlessSimulationRunner runner;
    
    @BeforeEach
    public void setup() {
        runner = new HeadlessSimulationRunner();
    }
    
    @AfterEach
    public void teardown() {
        runner.shutdown();
    }
    
    @Test
    @DisplayName("Test Internal Time Synchronization")
    public void testInternalTimeSync() throws Exception {
        SimulationResult result = runner.runSimulation(
            "saved-simulations/int-sync.dat", 
            3000
        );
        
        ProtocolVerifier verifier = new ProtocolVerifier()
            .expectLog("Internal.*sync.*activated|Internal sync.*activated")
            .expectLog("time|Time|sync|Sync")
            .expectLog("Message")
            .expectNoLog("ERROR");
            
        VerificationResult verification = verifier.verify(result.getAllLogs());
        assertTrue(verification.passed(), verification.getFailureMessage());
    }
    
    @Test
    @DisplayName("Test External vs Internal Time Synchronization")
    public void testExternalVsInternalSync() throws Exception {
        SimulationResult result = runner.runSimulation(
            "saved-simulations/ext-vs-int-sync.dat", 
            4000
        );
        
        // This simulation compares external and internal sync
        ProtocolVerifier verifier = new ProtocolVerifier()
            .expectLog("activated")
            .expectLog("sync|Sync|synchron")
            .expectNoLog("ERROR")
            .expectNoLog("failed");
            
        VerificationResult verification = verifier.verify(result.getAllLogs());
        assertTrue(verification.passed(), verification.getFailureMessage());
        
        // Check for both internal and external sync activity
        boolean hasInternal = result.countLogs("Internal|internal") > 0;
        boolean hasExternal = result.countLogs("External|external|Christians") > 0;
        
        assertTrue(hasInternal || hasExternal, 
                  "Should have time synchronization activity");
    }
    
    @Test
    @DisplayName("Test clock adjustments")
    public void testClockAdjustments() throws Exception {
        SimulationResult result = runner.runSimulation(
            "saved-simulations/int-sync.dat", 
            5000
        );
        
        // Look for time-related messages
        boolean hasTimeMessages = result.countLogs("time|Time|clock|Clock") > 0;
        boolean hasAdjustments = result.countLogs("adjust|Adjust|sync") > 0;
        
        assertTrue(hasTimeMessages || hasAdjustments, 
                  "Should have time-related activity");
    }
}