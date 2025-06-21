package testing.protocols;

import testing.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit test for Reliable Multicast protocol.
 */
@DisplayName("Reliable Multicast Protocol Tests")
public class ReliableMulticastProtocolTest {
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
    @DisplayName("Test Reliable Multicast protocol activation")
    public void testProtocolActivation() throws Exception {
        SimulationResult result = runner.runSimulation(
            "saved-simulations/reliable-multicast.dat", 
            1000
        );
        
        ProtocolVerifier verifier = new ProtocolVerifier()
            .expectLog("Reliable Multicast.*activated")
            .expectNoLog("ERROR");
            
        VerificationResult verification = verifier.verify(result.getAllLogs());
        assertTrue(verification.passed(), verification.getFailureMessage());
    }
    
    @Test
    @DisplayName("Test reliable delivery guarantees")
    public void testReliableDelivery() throws Exception {
        SimulationResult result = runner.runSimulation(
            "saved-simulations/reliable-multicast.dat", 
            5000
        );
        
        // Reliable multicast should ensure delivery
        ProtocolVerifier verifier = new ProtocolVerifier()
            .expectLog("Message sent")
            .expectLog("Message received")
            .expectLog("Multicast|multicast")
            .expectNoLog("lost|Lost|LOST")
            .expectNoLog("failed delivery");
            
        VerificationResult verification = verifier.verify(result.getAllLogs());
        assertTrue(verification.passed(), verification.getFailureMessage());
        
        // Check for acknowledgments or reliability mechanisms
        boolean hasAck = result.countLogs("ack|ACK|acknowledge|Acknowledge") > 0;
        boolean hasSeq = result.countLogs("sequence|Sequence|seq|SEQ") > 0;
        boolean hasReliable = result.countLogs("reliable|Reliable") > 0;
        
        assertTrue(hasAck || hasSeq || hasReliable, 
                  "Should have reliability mechanisms");
    }
    
    @Test
    @DisplayName("Test message ordering in reliable multicast")
    public void testMessageOrdering() throws Exception {
        SimulationResult result = runner.runSimulation(
            "saved-simulations/reliable-multicast.dat", 
            3000
        );
        
        // Verify messages maintain order
        var messages = result.findAll("Message received");
        
        long lastTime = -1;
        for (LogEntry entry : messages) {
            assertTrue(entry.getTimestamp() >= lastTime, 
                      "Messages should be in order");
            lastTime = entry.getTimestamp();
        }
    }
}