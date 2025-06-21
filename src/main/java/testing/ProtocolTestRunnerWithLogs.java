package testing;

import java.util.*;

/**
 * Protocol test runner that shows logs during execution for better visibility.
 */
public class ProtocolTestRunnerWithLogs {
    
    public static void main(String[] args) {
        System.out.println("=== DS-Sim Protocol Test Runner (with logs) ===\n");
        
        // Simple test configuration
        String[][] tests = {
            {"Ping-Pong", "saved-simulations/ping-pong.dat"},
            {"Broadcast", "saved-simulations/broadcast.dat"},
            {"Basic Multicast", "saved-simulations/basic-multicast.dat"},
            {"Berkeley Time Sync", "saved-simulations/berkeley.dat"},
            {"One-Phase Commit", "saved-simulations/one-phase-commit.dat"},
            {"Two-Phase Commit", "saved-simulations/two-phase-commit.dat"}
        };
        
        int passed = 0;
        int failed = 0;
        
        for (String[] test : tests) {
            String name = test[0];
            String file = test[1];
            
            System.out.println("\n" + "=".repeat(70));
            System.out.println("TEST: " + name);
            System.out.println("FILE: " + file);
            System.out.println("=".repeat(70));
            
            HeadlessSimulationRunner runner = new HeadlessSimulationRunner();
            
            try {
                // Create a custom log listener to format output nicely
                final int[] logCount = {0};
                final int maxLogs = 20; // Show first 20 logs
                
                LogListener listener = new LogListener() {
                    @Override
                    public void onLogEntry(LogEntry entry) {
                        if (logCount[0]++ < maxLogs) {
                            String timestamp = String.format("[%4dms]", entry.getTimestamp());
                            String process = entry.getType() == LogType.PROCESS ? 
                                "P" + entry.getProcessNum() : "SYS";
                            System.out.printf("%s %3s: %s\n", 
                                timestamp, process, entry.getMessage());
                        } else if (logCount[0] == maxLogs) {
                            System.out.println("... (more logs hidden)");
                        }
                    }
                };
                
                // Run simulation with listener
                System.out.println("\nRunning simulation for 2 seconds...\n");
                SimulationResult result = runner.runSimulation(file, 2000, listener);
                
                // If no logs were printed in real-time, show them now
                if (logCount[0] == 0 && result.getAllLogs().size() > 0) {
                    System.out.println("Captured logs:");
                    result.getAllLogs().stream()
                        .limit(maxLogs)
                        .forEach(log -> {
                            String timestamp = String.format("[%4dms]", log.getTimestamp());
                            String process = log.getType() == LogType.PROCESS ? 
                                "P" + log.getProcessNum() : "SYS";
                            System.out.printf("%s %3s: %s\n", 
                                timestamp, process, log.getMessage());
                        });
                }
                
                // Simple verification
                boolean hasActivation = result.countLogs("activated") > 0;
                boolean hasMessages = result.countLogs("Message") > 0;
                boolean hasErrors = result.countLogs("ERROR") > 0 || 
                                   result.countLogs("Exception") > 0;
                
                System.out.println("\nVerification:");
                System.out.println("  Protocol activated: " + (hasActivation ? "✓" : "✗"));
                System.out.println("  Messages exchanged: " + (hasMessages ? "✓" : "✗"));
                System.out.println("  No errors: " + (!hasErrors ? "✓" : "✗"));
                System.out.println("  Total logs: " + result.getAllLogs().size());
                
                if (hasActivation && !hasErrors) {
                    System.out.println("\n✓ PASSED");
                    passed++;
                } else {
                    System.out.println("\n✗ FAILED");
                    failed++;
                }
                
            } catch (Exception e) {
                System.out.println("\n✗ ERROR: " + e.getMessage());
                failed++;
            } finally {
                runner.shutdown();
            }
        }
        
        // Summary
        System.out.println("\n" + "=".repeat(70));
        System.out.println("SUMMARY");
        System.out.println("=".repeat(70));
        System.out.println("Total tests: " + tests.length);
        System.out.println("Passed: " + passed);
        System.out.println("Failed: " + failed);
        System.out.println();
        
        System.exit(failed == 0 ? 0 : 1);
    }
}