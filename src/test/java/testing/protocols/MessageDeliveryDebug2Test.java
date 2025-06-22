package testing.protocols;

import testing.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import core.VSTask;
import core.VSTaskManager;
import java.util.*;

/**
 * Extended debug test to understand why protocols are running twice.
 */
public class MessageDeliveryDebug2Test {
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
    @DisplayName("Debug why PingPong client sends two messages")
    public void debugDuplicateMessages() throws Exception {
        System.out.println("\n=== Starting Duplicate Message Debug Test ===");
        
        // Custom log listener to trace task execution
        LogListener listener = new LogListener() {
            private int messagesSent = 0;
            
            @Override
            public void onLogEntry(LogEntry entry) {
                String msg = entry.getMessage();
                
                // Track all events
                if (msg.contains("activated") || msg.contains("Message sent") || 
                    msg.contains("onClientStart") || msg.contains("counter=")) {
                    System.out.println(String.format("[TRACE %5d] P%d: %s", 
                        entry.getTimestamp(), entry.getProcessNum(), msg));
                    
                    if (msg.contains("Message sent")) {
                        messagesSent++;
                        if (messagesSent == 2) {
                            System.out.println("!!! ISSUE: Second message sent immediately at time " + 
                                entry.getTimestamp() + " - protocol likely executed twice!");
                        }
                    }
                }
                
                // Look for protocol event execution
                if (msg.contains("VSProtocolEvent") || msg.contains("VSPingPongProtocol")) {
                    System.out.println(String.format("[EVENT %5d] P%d: %s", 
                        entry.getTimestamp(), entry.getProcessNum(), msg));
                }
            }
        };
        
        SimulationResult result = runner.runSimulation(
            "saved-simulations/ping-pong.dat", 
            1000,  // Just 1 second is enough to see the issue
            listener
        );
        
        System.out.println("\n=== Analysis ===");
        
        // Count messages sent at time 0
        int messagesAtTimeZero = 0;
        for (LogEntry entry : result.getAllLogs()) {
            if (entry.getTimestamp() == 0 && entry.getMessage().contains("Message sent")) {
                messagesAtTimeZero++;
                System.out.println("Message at time 0: " + entry.getMessage());
            }
        }
        
        System.out.println("\nMessages sent at time 0: " + messagesAtTimeZero);
        
        if (messagesAtTimeZero > 1) {
            System.out.println("ERROR: Multiple messages sent at time 0 indicates duplicate protocol execution!");
        }
        
        System.out.println("\n=== Test Complete ===");
    }
}