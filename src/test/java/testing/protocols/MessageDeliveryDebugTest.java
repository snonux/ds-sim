package testing.protocols;

import testing.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Debug test to understand message delivery issues in headless mode.
 */
public class MessageDeliveryDebugTest {
    private HeadlessSimulationRunner runner;
    
    @BeforeEach
    public void setup() {
        runner = new HeadlessSimulationRunner();
        runner.setPrintLogs(true); // Enable log printing for debugging
    }
    
    @AfterEach
    public void teardown() {
        runner.shutdown();
    }
    
    @Test
    @DisplayName("Debug message delivery in Ping-Pong protocol")
    public void debugMessageDelivery() throws Exception {
        System.out.println("\n=== Starting Message Delivery Debug Test ===");
        
        // Run simulation with log listener to see what's happening
        LogListener listener = new LogListener() {
            @Override
            public void onLogEntry(LogEntry entry) {
                String msg = entry.getMessage();
                if (msg.contains("Message sent") || msg.contains("Message received") || 
                    msg.contains("scheduled for delivery") || msg.contains("activated")) {
                    System.out.println(String.format("[DEBUG %5d] P%d: %s", 
                        entry.getTimestamp(), entry.getProcessNum(), msg));
                }
            }
        };
        
        SimulationResult result = runner.runSimulation(
            "saved-simulations/ping-pong.dat", 
            5000,
            listener
        );
        
        System.out.println("\n=== Simulation Complete ===");
        System.out.println("Total logs captured: " + result.getAllLogs().size());
        
        // Count specific log types
        int sentCount = result.countLogs("Message sent");
        int receivedCount = result.countLogs("Message received");
        int activatedCount = result.countLogs("activated");
        
        System.out.println("Messages sent: " + sentCount);
        System.out.println("Messages received: " + receivedCount);
        System.out.println("Protocols activated: " + activatedCount);
        
        // Print all logs for analysis
        System.out.println("\n=== All Logs ===");
        for (LogEntry entry : result.getAllLogs()) {
            System.out.println(String.format("[%5d] P%d: %s", 
                entry.getTimestamp(), entry.getProcessNum(), entry.getMessage()));
        }
        
        // Basic assertions
        assertTrue(activatedCount > 0, "At least one protocol should be activated");
        System.out.println("\n=== Test Complete ===");
    }
}