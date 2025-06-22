package testing.protocols;

import testing.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for Raft consensus protocol.
 */
public class RaftProtocolTest extends BaseProtocolTest {
    
    @Test
    @DisplayName("Test Raft protocol activation and message sending")
    public void testRaftActivation() {
        SimulationResult result = runSimulation(
            "saved-simulations/raft.dat", 
            2000  // 2 seconds should be enough for elections
        );
        
        ProtocolVerifier verifier = new ProtocolVerifier()
            .expectLogExactly("Raft Consensus Server activated", 3)
            .expectLog("FOLLOWER.*initialized")
            .expectLog("Starting election")
            .expectLog("CANDIDATE")
            .expectMessages()  // Must have messages
            .expectAtLeastNMessages(10);  // Should have many election messages
            
        VerificationResult verification = verifier.verify(result.getAllLogs());
        
        assertTrue(verification.passed(), verification.getFailureMessage());
        assertEquals(3, result.getMetrics().getNumProcesses(), 
                    "Should have 3 processes");
    }
    
    @Test
    @DisplayName("Test Raft election messages")
    public void testRaftElectionMessages() {
        SimulationResult result = runSimulation(
            "saved-simulations/raft.dat", 
            3000
        );
        
        ProtocolVerifier verifier = new ProtocolVerifier()
            .expectLog("REQUEST_VOTE")
            .expectLog("Message sent.*REQUEST_VOTE")
            .expectAtLeastNMessages(15);  // Multiple election rounds
            
        VerificationResult verification = verifier.verify(result.getAllLogs());
        assertTrue(verification.passed(), verification.getFailureMessage());
        
        // Verify term progression
        assertTrue(result.findFirst("term=1").isPresent(), "Should have term 1");
        assertTrue(result.findFirst("term=2").isPresent(), "Should progress to term 2");
    }
    
    @Test
    @DisplayName("Test Raft with clients")
    public void testRaftWithClients() {
        // Skip if file doesn't exist
        if (!new java.io.File("saved-simulations/raft-with-clients.dat").exists()) {
            return;
        }
        
        SimulationResult result = runSimulation(
            "saved-simulations/raft-with-clients.dat", 
            5000
        );
        
        ProtocolVerifier verifier = new ProtocolVerifier()
            .expectLogExactly("Raft Consensus Server activated", 3)
            .expectLogExactly("Raft Consensus Client activated", 2)
            .expectMessages();  // Must have messages
            
        VerificationResult verification = verifier.verify(result.getAllLogs());
        assertTrue(verification.passed(), verification.getFailureMessage());
    }
}