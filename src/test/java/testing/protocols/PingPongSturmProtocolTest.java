package testing.protocols;

import testing.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit test for Ping-Pong Sturm variant.
 */
@DisplayName("Ping-Pong Sturm Protocol Tests")
public class PingPongSturmProtocolTest {
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
    @DisplayName("Test Ping-Pong Sturm protocol activation")
    public void testProtocolActivation() throws Exception {
        SimulationResult result = runner.runSimulation(
            "saved-simulations/ping-pong-sturm.dat", 
            1000
        );
        
        ProtocolVerifier verifier = new ProtocolVerifier()
            .expectLog("Ping-Pong.*activated")
            .expectNoLog("ERROR");
            
        VerificationResult verification = verifier.verify(result.getAllLogs());
        assertTrue(verification.passed(), verification.getFailureMessage());
    }
    
    @Test
    @DisplayName("Test Sturm variant message exchange")
    public void testSturmMessageExchange() throws Exception {
        SimulationResult result = runner.runSimulation(
            "saved-simulations/ping-pong-sturm.dat", 
            3000
        );
        
        // Similar to regular ping-pong but may have different patterns
        ProtocolVerifier verifier = new ProtocolVerifier()
            .expectLog("Message sent")
            .expectLog("Message received")
            .expectLog("fromClient=true|fromServer=true");
            
        VerificationResult verification = verifier.verify(result.getAllLogs());
        assertTrue(verification.passed(), verification.getFailureMessage());
        
        // Check for balanced communication
        int sent = result.countLogs("Message sent");
        int received = result.countLogs("Message received");
        
        assertTrue(Math.abs(sent - received) <= 2, 
                  "Messages should be roughly balanced");
    }
}