package testing.protocols;

import testing.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Deep debug test to understand why messages aren't being received.
 */
public class MessageDeliveryDebug3Test {
    private HeadlessSimulationRunner runner;
    
    @BeforeEach
    public void setup() {
        runner = new HeadlessSimulationRunner();
        runner.setPrintLogs(true);
    }
    
    @AfterEach
    public void teardown() {
        runner.shutdown();
    }
    
    @Test
    @DisplayName("Debug message delivery with detailed logging")
    public void debugMessageDelivery() throws Exception {
        System.out.println("\n=== Starting Message Delivery Deep Debug ===");
        
        // Run for longer to see if messages eventually get delivered
        SimulationResult result = runner.runSimulation(
            "saved-simulations/ping-pong.dat", 
            10000  // 10 seconds
        );
        
        System.out.println("\n=== Analysis ===");
        
        // Count message types
        int sentCount = 0;
        int receivedCount = 0;
        int scheduledCount = 0;
        
        for (LogEntry entry : result.getAllLogs()) {
            String msg = entry.getMessage();
            if (msg.contains("Message sent")) {
                sentCount++;
                System.out.println("SENT at " + entry.getTimestamp() + ": " + msg);
            } else if (msg.contains("Message received")) {
                receivedCount++;
                System.out.println("RECEIVED at " + entry.getTimestamp() + ": " + msg);
            } else if (msg.contains("scheduled for delivery")) {
                scheduledCount++;
                System.out.println("SCHEDULED: " + msg);
            }
        }
        
        System.out.println("\nTotal messages sent: " + sentCount);
        System.out.println("Total messages received: " + receivedCount);
        System.out.println("Total messages scheduled: " + scheduledCount);
        
        // Check if we're getting any server/client activity
        boolean hasServerActivity = false;
        boolean hasClientActivity = false;
        
        for (LogEntry entry : result.getAllLogs()) {
            String msg = entry.getMessage();
            if (msg.contains("Server") && msg.contains("activated")) {
                hasServerActivity = true;
            }
            if (msg.contains("Client") && msg.contains("activated")) {
                hasClientActivity = true;
            }
        }
        
        System.out.println("\nServer activated: " + hasServerActivity);
        System.out.println("Client activated: " + hasClientActivity);
        
        // Print all logs for full context
        System.out.println("\n=== All Logs ===");
        for (LogEntry entry : result.getAllLogs()) {
            System.out.println(String.format("[%5d] P%d: %s", 
                entry.getTimestamp(), entry.getProcessNum(), entry.getMessage()));
        }
        
        assertTrue(sentCount > 0, "Should have sent at least one message");
        assertTrue(receivedCount > 0, "Should have received at least one message");
    }
}