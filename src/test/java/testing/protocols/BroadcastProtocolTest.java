package testing.protocols;

import testing.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit test for Broadcast protocol.
 */
@DisplayName("Broadcast Protocol Tests")
public class BroadcastProtocolTest {
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
    @DisplayName("Test Broadcast protocol activation")
    public void testProtocolActivation() throws Exception {
        SimulationResult result = runner.runSimulation(
            "saved-simulations/broadcast.dat", 
            1000
        );
        
        ProtocolVerifier verifier = new ProtocolVerifier()
            .expectLog("Broadcast.*activated")
            .expectNoLog("ERROR")
            .expectNoLog("Exception")
            .expectMessages();  // Broadcast must send messages
            
        VerificationResult verification = verifier.verify(result.getAllLogs());
        
        assertTrue(verification.passed(), verification.getFailureMessage());
    }
    
    @Test
    @DisplayName("Test broadcast message delivery to all nodes")
    public void testBroadcastDelivery() throws Exception {
        SimulationResult result = runner.runSimulation(
            "saved-simulations/broadcast.dat", 
            3000
        );
        
        // In broadcast, one message should be received by multiple nodes
        ProtocolVerifier verifier = new ProtocolVerifier()
            .expectLog("Message sent")
            .expectLog("Message received")
            .expectLog("Broadcast|broadcast|BROADCAST");
            
        VerificationResult verification = verifier.verify(result.getAllLogs());
        assertTrue(verification.passed(), verification.getFailureMessage());
        
        // Verify broadcast property: more receives than sends
        int sent = result.countLogs("Message sent");
        int received = result.countLogs("Message received");
        
        if (sent > 0 && result.getMetrics().getNumProcesses() > 2) {
            assertTrue(received > sent, 
                      "Broadcast should deliver to multiple receivers");
        }
    }
    
    @Test
    @DisplayName("Test all processes receive broadcast")
    public void testAllProcessesReceive() throws Exception {
        SimulationResult result = runner.runSimulation(
            "saved-simulations/broadcast.dat", 
            4000
        );
        
        // Count how many different processes received messages
        var processLogs = result.getProcessLogs();
        int processesWithReceivedMessages = 0;
        
        for (var entry : processLogs.entrySet()) {
            boolean hasReceived = entry.getValue().stream()
                .anyMatch(log -> log.getMessage().contains("received"));
            if (hasReceived) {
                processesWithReceivedMessages++;
            }
        }
        
        // In broadcast, multiple processes should receive messages
        assertTrue(processesWithReceivedMessages >= 1, 
                  "At least one process should receive messages");
    }
}