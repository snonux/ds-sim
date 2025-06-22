package testing.protocols;

import testing.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test with longer timeout to ensure all messages are delivered.
 */
public class MessageDeliveryDebug4Test {
    private HeadlessSimulationRunner runner;
    
    @BeforeEach
    public void setup() {
        runner = new HeadlessSimulationRunner();
    }
    
    @Test
    @DisplayName("Test counter values with extended timeout")
    public void testCounterValues() throws Exception {
        System.out.println("\n=== Testing Counter Values ===");
        
        // Run for 10 seconds to ensure all messages are exchanged
        SimulationResult result = runner.runSimulation(
            "saved-simulations/ping-pong.dat", 
            10000
        );
        
        System.out.println("\n=== Counter Analysis ===");
        
        // Look for all counter values
        int maxClientCounter = 0;
        int maxServerCounter = 0;
        
        for (LogEntry entry : result.getAllLogs()) {
            String msg = entry.getMessage();
            if (msg.contains("counter=")) {
                int start = msg.indexOf("counter=") + 8;
                int end = msg.indexOf(";", start);
                if (end == -1) end = msg.indexOf(" ", start);
                if (end == -1) end = msg.length();
                
                try {
                    int counter = Integer.parseInt(msg.substring(start, end));
                    System.out.println("Found counter=" + counter + " at time " + entry.getTimestamp());
                    
                    if (msg.contains("fromClient=true")) {
                        maxClientCounter = Math.max(maxClientCounter, counter);
                    } else if (msg.contains("fromServer=true")) {
                        maxServerCounter = Math.max(maxServerCounter, counter);
                    }
                } catch (NumberFormatException e) {
                    // Ignore
                }
            }
        }
        
        System.out.println("\nMax client counter: " + maxClientCounter);
        System.out.println("Max server counter: " + maxServerCounter);
        
        // Count messages
        int sentCount = result.countLogs("Message sent");
        int receivedCount = result.countLogs("Message received");
        
        System.out.println("\nTotal messages sent: " + sentCount);
        System.out.println("Total messages received: " + receivedCount);
        
        // Print timeline
        System.out.println("\n=== Message Timeline ===");
        for (LogEntry entry : result.getAllLogs()) {
            String msg = entry.getMessage();
            if (msg.contains("Message sent") || msg.contains("Message received") || 
                msg.contains("activated")) {
                System.out.println(String.format("[%5d] P%d: %s", 
                    entry.getTimestamp(), entry.getProcessNum(), 
                    msg.substring(msg.indexOf(";") + 2)));  // Skip PID part
            }
        }
        
        assertTrue(maxClientCounter >= 2, "Client should reach at least counter=2");
        assertTrue(maxServerCounter >= 1, "Server should reach at least counter=1");
    }
}