package testing.protocols;

import testing.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit test for Basic Multicast protocol using the headless testing framework.
 */
@DisplayName("Basic Multicast Protocol Tests")
public class BasicMulticastProtocolTest {
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
    @DisplayName("Test Basic Multicast protocol activation")
    public void testProtocolActivation() throws Exception {
        SimulationResult result = runner.runSimulation(
            "saved-simulations/basic-multicast.dat", 
            1000
        );
        
        ProtocolVerifier verifier = new ProtocolVerifier()
            .expectLog("Basic Multicast.*activated")
            .expectLog("Client activated")
            .expectLog("Server activated");
            
        VerificationResult verification = verifier.verify(result.getAllLogs());
        
        assertTrue(verification.passed(), verification.getFailureMessage());
        assertTrue(result.getMetrics().getNumProcesses() >= 3, 
                  "Should have at least 3 processes for multicast");
    }
    
    @Test
    @DisplayName("Test multicast message delivery")
    public void testMulticastDelivery() throws Exception {
        SimulationResult result = runner.runSimulation(
            "saved-simulations/basic-multicast.dat", 
            3000
        );
        
        // Verify multicast behavior
        ProtocolVerifier verifier = new ProtocolVerifier()
            .expectLog("Message sent")
            .expectLog("Message received")
            .expectLog("Multicast")
            .expectNoLog("ERROR")
            .expectNoLog("failed");
            
        VerificationResult verification = verifier.verify(result.getAllLogs());
        assertTrue(verification.passed(), verification.getFailureMessage());
        
        // Check that multiple processes receive messages
        int sentCount = result.countLogs("Message sent");
        int receivedCount = result.countLogs("Message received");
        
        assertTrue(receivedCount >= sentCount, 
                  "In multicast, received messages should be >= sent messages");
    }
    
    @Test
    @DisplayName("Test message ordering")
    public void testMessageOrdering() throws Exception {
        SimulationResult result = runner.runSimulation(
            "saved-simulations/basic-multicast.dat", 
            2000
        );
        
        // Verify messages are delivered in order
        var receivedMessages = result.findAll("Message received");
        
        long lastTimestamp = -1;
        for (LogEntry entry : receivedMessages) {
            assertTrue(entry.getTimestamp() >= lastTimestamp, 
                      "Messages should be received in chronological order");
            lastTimestamp = entry.getTimestamp();
        }
    }
}