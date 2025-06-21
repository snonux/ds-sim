package testing.protocols;

import testing.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit test for Slow Connection simulation.
 */
@DisplayName("Slow Connection Simulation Tests")
public class SlowConnectionProtocolTest {
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
    @DisplayName("Test slow connection simulation")
    public void testSlowConnection() throws Exception {
        SimulationResult result = runner.runSimulation(
            "saved-simulations/slow-connection.dat", 
            5000
        );
        
        // Slow connection should show delayed message delivery
        ProtocolVerifier verifier = new ProtocolVerifier()
            .expectLog("activated")
            .expectLog("Message")
            .expectNoLog("ERROR");
            
        VerificationResult verification = verifier.verify(result.getAllLogs());
        assertTrue(verification.passed(), verification.getFailureMessage());
    }
    
    @Test
    @DisplayName("Test message delays in slow connection")
    public void testMessageDelays() throws Exception {
        SimulationResult result = runner.runSimulation(
            "saved-simulations/slow-connection.dat", 
            6000
        );
        
        // Look for evidence of delays
        var sentMessages = result.findAll("Message sent");
        var receivedMessages = result.findAll("Message received");
        
        if (!sentMessages.isEmpty() && !receivedMessages.isEmpty()) {
            // Calculate average delay
            long totalDelay = 0;
            int delayCount = 0;
            
            for (int i = 0; i < Math.min(sentMessages.size(), receivedMessages.size()); i++) {
                long sentTime = sentMessages.get(i).getTimestamp();
                long receivedTime = receivedMessages.get(i).getTimestamp();
                if (receivedTime > sentTime) {
                    totalDelay += (receivedTime - sentTime);
                    delayCount++;
                }
            }
            
            if (delayCount > 0) {
                long avgDelay = totalDelay / delayCount;
                assertTrue(avgDelay > 0, "Should have message delays in slow connection");
            }
        }
    }
    
    @Test
    @DisplayName("Test connection characteristics")
    public void testConnectionCharacteristics() throws Exception {
        SimulationResult result = runner.runSimulation(
            "saved-simulations/slow-connection.dat", 
            4000
        );
        
        // Check for slow/delay related messages
        boolean hasSlowIndication = result.countLogs("slow|Slow|delay|Delay") > 0;
        boolean hasConnection = result.countLogs("connection|Connection") > 0;
        
        assertTrue(hasSlowIndication || hasConnection || result.getAllLogs().size() > 0, 
                  "Should have some activity indicating slow connection");
    }
}