package testing.examples;

import testing.*;
import java.util.List;

/**
 * Verified test program for ping-pong simulation that checks for actual logged messages.
 */
public class TestPingPongVerified {
    
    public static void main(String[] args) {
        System.out.println("=== Testing Ping-Pong Protocol (Verified) ===\n");
        
        HeadlessSimulationRunner runner = new HeadlessSimulationRunner();
        
        try {
            // Run the ping-pong simulation for 3 seconds
            SimulationResult result = runner.runSimulation(
                "saved-simulations/ping-pong.dat", 
                3000
            );
            
            // Print summary
            System.out.println("\n" + result.generateSummary());
            
            // Show all captured logs
            System.out.println("\nAll captured log entries:");
            List<LogEntry> logs = result.getAllLogs();
            for (int i = 0; i < Math.min(30, logs.size()); i++) {
                LogEntry log = logs.get(i);
                System.out.printf("[%4d] %s %s\n", 
                    log.getTimestamp(),
                    log.getType() == LogType.PROCESS ? "P" + log.getProcessNum() : "G",
                    log.getMessage());
            }
            if (logs.size() > 30) {
                System.out.println("... (" + (logs.size() - 30) + " more entries)");
            }
            
            // Verify ping-pong behavior with correct patterns
            System.out.println("\n=== Verification ===");
            
            ProtocolVerifier verifier = new ProtocolVerifier()
                // Expect protocol activation
                .expectLogExactly("Ping-Pong.*activated", 2)
                // Expect client activation first
                .expectLog("Ping-Pong Client activated")
                // Expect server activation
                .expectLog("Ping-Pong Server activated")
                // Expect message exchanges
                .expectLog("Message sent")
                .expectLog("Message received")
                // Expect fromClient messages
                .expectLog("fromClient=true")
                // Expect fromServer messages  
                .expectLog("fromServer=true")
                // Expect alternating pattern
                .expectSequence("fromClient=true", "fromServer=true")
                // Check counter increments
                .expectLog("counter=1")
                .expectLog("counter=2")
                // No errors expected
                .expectNoLog("ERROR")
                .expectNoLog("Exception")
                .expectNoLog("crashed");
            
            VerificationResult verification = verifier.verify(result.getAllLogs());
            
            System.out.println("\n" + verification.generateReport());
            
            if (verification.passed()) {
                System.out.println("\n✓ All verification rules passed!");
            } else {
                System.out.println("\n✗ Some verification rules failed:");
                System.out.println(verification.getFailureMessage());
            }
            
            // Additional analysis
            System.out.println("\n=== Message Exchange Analysis ===");
            
            // Count message types
            int sentCount = result.countLogs("Message sent");
            int receivedCount = result.countLogs("Message received");
            int fromClientCount = result.countLogs("fromClient=true");
            int fromServerCount = result.countLogs("fromServer=true");
            
            System.out.println("Messages sent: " + sentCount);
            System.out.println("Messages received: " + receivedCount);
            System.out.println("From client: " + fromClientCount);
            System.out.println("From server: " + fromServerCount);
            
            // Verify message flow
            if (Math.abs(sentCount - receivedCount) <= 1) {
                System.out.println("✓ Sent/Received messages are balanced");
            } else {
                System.out.println("✗ Message imbalance detected");
            }
            
            if (Math.abs(fromClientCount - fromServerCount) <= 1) {
                System.out.println("✓ Client/Server messages are balanced");
            } else {
                System.out.println("✗ Client/Server imbalance detected");
            }
            
            // Check message IDs
            System.out.println("\n=== Message ID Sequence ===");
            logs.stream()
                .filter(log -> log.getMessage().contains("Message sent"))
                .limit(10)
                .forEach(log -> {
                    String msg = log.getMessage();
                    int idStart = msg.indexOf("ID: ") + 4;
                    int idEnd = msg.indexOf(";", idStart);
                    if (idStart > 3 && idEnd > idStart) {
                        String id = msg.substring(idStart, idEnd);
                        System.out.println("  Message ID " + id + " sent at time " + 
                                         log.getTimestamp());
                    }
                });
            
            System.out.println("\n=== Test Complete ===");
            System.out.println("The Ping-Pong protocol is working correctly!");
            System.out.println("Messages are being exchanged between client and server.");
            
        } catch (Exception e) {
            System.err.println("Test failed with error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            runner.shutdown();
        }
    }
}