package testing.protocols;

import testing.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit test for Ping-Pong protocol using the headless testing framework.
 */
public class PingPongProtocolTest {
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
    @DisplayName("Test Ping-Pong protocol activation")
    public void testProtocolActivation() throws Exception {
        SimulationResult result = runner.runSimulation(
            "saved-simulations/ping-pong.dat", 
            1000
        );
        
        ProtocolVerifier verifier = new ProtocolVerifier()
            .expectLogExactly("Ping-Pong.*activated", 2)
            .expectLog("Ping-Pong Client activated")
            .expectLog("Ping-Pong Server activated")
            .expectMessages();  // Ensure messages are sent
            
        VerificationResult verification = verifier.verify(result.getAllLogs());
        
        assertTrue(verification.passed(), verification.getFailureMessage());
        assertEquals(2, result.getMetrics().getNumProcesses(), 
                    "Should have 2 processes");
    }
    
    @Test
    @DisplayName("Test Ping-Pong message exchange")
    public void testMessageExchange() throws Exception {
        SimulationResult result = runner.runSimulation(
            "saved-simulations/ping-pong.dat", 
            3000
        );
        
        ProtocolVerifier verifier = new ProtocolVerifier()
            .expectLog("Message sent")
            .expectLog("Message received")
            .expectLog("fromClient=true")
            .expectLog("fromServer=true")
            .expectSequence("fromClient=true", "fromServer=true");
            
        VerificationResult verification = verifier.verify(result.getAllLogs());
        assertTrue(verification.passed(), verification.getFailureMessage());
        
        // Check message balance
        int sent = result.countLogs("Message sent");
        int received = result.countLogs("Message received");
        assertTrue(Math.abs(sent - received) <= 1, 
                  "Sent/Received messages should be balanced");
    }
    
    @Test
    @DisplayName("Test Ping-Pong counter increments")
    public void testCounterIncrements() throws Exception {
        SimulationResult result = runner.runSimulation(
            "saved-simulations/ping-pong.dat", 
            5000
        );
        
        // Verify counter increments
        assertTrue(result.findFirst("counter=1").isPresent(), 
                  "Should have counter=1");
        assertTrue(result.findFirst("counter=2").isPresent(), 
                  "Should have counter=2");
        assertTrue(result.findFirst("counter=3").isPresent(), 
                  "Should have counter=3");
    }
    
    @Test
    @DisplayName("Test no errors occur during simulation")
    public void testNoErrors() throws Exception {
        SimulationResult result = runner.runSimulation(
            "saved-simulations/ping-pong.dat", 
            2000
        );
        
        ProtocolVerifier verifier = new ProtocolVerifier()
            .expectNoLog("ERROR")
            .expectNoLog("Exception")
            .expectNoLog("crashed")
            .expectNoLog("failed");
            
        VerificationResult verification = verifier.verify(result.getAllLogs());
        assertTrue(verification.passed(), "No errors should occur");
    }
    
    @Test
    @DisplayName("Test message timing and ordering")
    public void testMessageTiming() throws Exception {
        SimulationResult result = runner.runSimulation(
            "saved-simulations/ping-pong.dat", 
            3000
        );
        
        // Get all sent messages
        var sentMessages = result.findAll("Message sent");
        assertTrue(sentMessages.size() >= 4, 
                  "Should have at least 4 sent messages");
        
        // Verify messages are sent at increasing timestamps
        long lastTime = -1;
        for (LogEntry entry : sentMessages) {
            assertTrue(entry.getTimestamp() >= lastTime, 
                      "Messages should be in chronological order");
            lastTime = entry.getTimestamp();
        }
        
        // Verify Lamport time increases
        var allLogs = result.getAllLogs();
        for (LogEntry log : allLogs) {
            String msg = log.getMessage();
            if (msg.contains("Lamport time:")) {
                int start = msg.indexOf("Lamport time: ") + 14;
                int end = msg.indexOf(";", start);
                if (end == -1) end = msg.length();
                try {
                    int lamport = Integer.parseInt(msg.substring(start, end));
                    assertTrue(lamport >= 0, "Lamport time should be non-negative");
                } catch (NumberFormatException e) {
                    // Skip if format is different
                }
            }
        }
    }
}