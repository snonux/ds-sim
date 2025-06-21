package testing.examples;

import testing.*;
import java.util.List;

/**
 * Test program to verify the headless testing framework with ping-pong simulation.
 * This demonstrates how to use the framework to verify protocol behavior.
 */
public class TestPingPongSimulation {
    
    public static void main(String[] args) {
        System.out.println("=== Testing Ping-Pong Protocol ===\n");
        
        HeadlessSimulationRunner runner = new HeadlessSimulationRunner();
        
        try {
            // Run the ping-pong simulation for 2 seconds
            SimulationResult result = runner.runSimulation(
                "saved-simulations/ping-pong.dat", 
                2000
            );
            
            // Print summary
            System.out.println("\n" + result.generateSummary());
            
            // Show first 20 logs
            System.out.println("\nFirst 20 log entries:");
            List<LogEntry> logs = result.getAllLogs();
            for (int i = 0; i < Math.min(20, logs.size()); i++) {
                System.out.println("  " + logs.get(i));
            }
            
            // Verify ping-pong behavior
            System.out.println("\n=== Verification ===");
            
            ProtocolVerifier verifier = new ProtocolVerifier()
                // Expect protocol activation
                .expectLog("Ping-Pong.*activated")
                // Expect ping messages
                .expectLog("ping")
                // Expect pong responses
                .expectLog("pong")
                // Expect alternating sequence
                .expectSequence("ping", "pong")
                // No errors expected
                .expectNoLog("ERROR")
                .expectNoLog("Exception");
            
            VerificationResult verification = verifier.verify(result.getAllLogs());
            
            System.out.println("\n" + verification.generateReport());
            
            if (verification.passed()) {
                System.out.println("\n✓ All verification rules passed!");
            } else {
                System.out.println("\n✗ Some verification rules failed:");
                System.out.println(verification.getFailureMessage());
            }
            
            // Additional analysis
            System.out.println("\n=== Protocol Analysis ===");
            
            // Count ping and pong messages
            int pingCount = result.countLogs("ping");
            int pongCount = result.countLogs("pong");
            
            System.out.println("Ping messages: " + pingCount);
            System.out.println("Pong messages: " + pongCount);
            
            // Check balance
            if (Math.abs(pingCount - pongCount) <= 1) {
                System.out.println("✓ Ping/Pong messages are balanced");
            } else {
                System.out.println("✗ Ping/Pong imbalance detected");
            }
            
            // Check for message patterns by process
            System.out.println("\n=== Per-Process Analysis ===");
            for (int i = 0; i < result.getMetrics().getNumProcesses(); i++) {
                List<LogEntry> processLogs = result.getLogsForProcess(i);
                if (!processLogs.isEmpty()) {
                    System.out.println("Process " + i + ":");
                    System.out.println("  Total messages: " + processLogs.size());
                    
                    long pings = processLogs.stream()
                        .filter(log -> log.getMessage().contains("ping"))
                        .count();
                    long pongs = processLogs.stream()
                        .filter(log -> log.getMessage().contains("pong"))
                        .count();
                    
                    System.out.println("  Pings sent: " + pings);
                    System.out.println("  Pongs sent: " + pongs);
                }
            }
            
            // Test real-time monitoring
            System.out.println("\n=== Testing Real-time Monitoring ===");
            
            HeadlessSimulationRunner runner2 = new HeadlessSimulationRunner();
            
            // Add a listener that prints ping/pong messages in real-time
            class PingPongMonitor implements LogListener {
                private int pingCount = 0;
                private int pongCount = 0;
                
                @Override
                public void onLogEntry(LogEntry entry) {
                    if (entry.getMessage().contains("ping")) {
                        pingCount++;
                        if (pingCount <= 5) {
                            System.out.println("  [MONITOR] Ping #" + pingCount + 
                                             " at time " + entry.getTimestamp());
                        }
                    } else if (entry.getMessage().contains("pong")) {
                        pongCount++;
                        if (pongCount <= 5) {
                            System.out.println("  [MONITOR] Pong #" + pongCount + 
                                             " at time " + entry.getTimestamp());
                        }
                    }
                }
            }
            
            // Note: We'd need to modify HeadlessSimulationRunner to expose
            // the LogCapture to add listeners, but this shows the concept
            
            System.out.println("\n=== Test Complete ===");
            
        } catch (Exception e) {
            System.err.println("Test failed with error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            runner.shutdown();
        }
    }
}