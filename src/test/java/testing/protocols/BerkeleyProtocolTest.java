package testing.protocols;

import testing.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit test for Berkeley time synchronization protocol.
 */
@DisplayName("Berkeley Time Synchronization Protocol Tests")
public class BerkeleyProtocolTest {
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
    @DisplayName("Test Berkeley protocol activation")
    public void testProtocolActivation() throws Exception {
        SimulationResult result = runner.runSimulation(
            "saved-simulations/berkeley.dat", 
            1000
        );
        
        ProtocolVerifier verifier = new ProtocolVerifier()
            .expectLog("Berkley.*activated|Berkeley.*activated")
            .expectNoLog("ERROR");
            
        VerificationResult verification = verifier.verify(result.getAllLogs());
        
        assertTrue(verification.passed(), verification.getFailureMessage());
        assertTrue(result.getMetrics().getTotalLogCount() > 0, 
                  "Should have some log activity");
    }
    
    @Test
    @DisplayName("Test time synchronization messages")
    public void testTimeSynchronization() throws Exception {
        SimulationResult result = runner.runSimulation(
            "saved-simulations/berkeley.dat", 
            5000
        );
        
        // Berkeley protocol involves time requests and adjustments
        ProtocolVerifier verifier = new ProtocolVerifier()
            .expectLog("time|Time|TIME")
            .expectLog("sync|Sync|SYNC")
            .expectLog("Message sent")
            .expectLog("Message received");
            
        VerificationResult verification = verifier.verify(result.getAllLogs());
        assertTrue(verification.passed(), verification.getFailureMessage());
    }
    
    @Test
    @DisplayName("Test master-slave communication")
    public void testMasterSlaveCommunication() throws Exception {
        SimulationResult result = runner.runSimulation(
            "saved-simulations/berkeley.dat", 
            3000
        );
        
        // Berkeley has master and slave nodes
        boolean hasMasterActivity = result.countLogs("master|Master|MASTER") > 0;
        boolean hasSlaveActivity = result.countLogs("slave|Slave|SLAVE") > 0;
        boolean hasTimeExchange = result.countLogs("time|Time") > 0;
        
        assertTrue(hasMasterActivity || hasSlaveActivity || hasTimeExchange, 
                  "Should have master/slave or time-related activity");
    }
}