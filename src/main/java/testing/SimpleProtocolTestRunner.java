package testing;

import java.io.File;
import java.util.*;

/**
 * Simple runner for protocol tests that shows detailed output.
 */
public class SimpleProtocolTestRunner {
    
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_BLUE = "\u001B[34m";
    private static final String ANSI_RESET = "\u001B[0m";
    
    public static void main(String[] args) {
        System.out.println("=== DS-Sim Protocol Test Runner ===\n");
        
        // Check if saved simulations exist
        File savedDir = new File("saved-simulations");
        if (!savedDir.exists()) {
            System.err.println("ERROR: saved-simulations directory not found!");
            System.err.println("Please ensure you're running from the project root directory.");
            System.exit(1);
        }
        
        // List of all protocol test simulations
        Map<String, TestInfo> tests = new LinkedHashMap<>();
        tests.put("Ping-Pong Protocol", new TestInfo("saved-simulations/ping-pong.dat", 5000,
            Arrays.asList("Ping", "Pong", "message:", "counter")));
        tests.put("Berkeley Time Protocol", new TestInfo("saved-simulations/berkeley.dat", 5000,
            Arrays.asList("Time synchronization", "Berkeley", "adjustment")));
        tests.put("Broadcast Protocol", new TestInfo("saved-simulations/broadcast.dat", 3000,
            Arrays.asList("broadcast", "received", "message")));
        tests.put("Basic Multicast Protocol", new TestInfo("saved-simulations/basic-multicast.dat", 3000,
            Arrays.asList("multicast", "group", "received")));
        tests.put("Reliable Multicast Protocol", new TestInfo("saved-simulations/reliable-multicast.dat", 5000,
            Arrays.asList("reliable", "multicast", "ACK", "resend")));
        tests.put("External Time Sync", new TestInfo("saved-simulations/external-time-sync.dat", 5000,
            Arrays.asList("external", "time", "sync", "adjustment")));
        tests.put("Internal Time Sync", new TestInfo("saved-simulations/internal-time-sync.dat", 5000,
            Arrays.asList("internal", "time", "sync", "average")));
        tests.put("One-Phase Commit Protocol", new TestInfo("saved-simulations/one-phase-commit.dat", 3000,
            Arrays.asList("commit", "vote", "decision")));
        tests.put("Two-Phase Commit Protocol", new TestInfo("saved-simulations/two-phase-commit.dat", 5000,
            Arrays.asList("prepare", "commit", "vote", "decision")));
        tests.put("Slow Connection Protocol", new TestInfo("saved-simulations/slow-connection.dat", 8000,
            Arrays.asList("slow", "connection", "delay")));
        tests.put("Ping-Pong Sturm Protocol", new TestInfo("saved-simulations/ping-pong-sturm.dat", 5000,
            Arrays.asList("Sturm", "ping", "pong")));
        
        int passed = 0;
        int failed = 0;
        int skipped = 0;
        
        // Create headless runner with logs enabled
        HeadlessSimulationRunner runner = new HeadlessSimulationRunner();
        runner.setPrintLogs(true);
        
        // Run each test
        for (Map.Entry<String, TestInfo> entry : tests.entrySet()) {
            String testName = entry.getKey();
            TestInfo info = entry.getValue();
            
            System.out.println("\n" + ANSI_YELLOW + "Testing: " + testName + ANSI_RESET);
            System.out.println("Simulation file: " + info.simulationFile);
            System.out.println("Duration: " + info.duration + "ms");
            System.out.println("Expected patterns: " + info.expectedPatterns);
            
            // Check if simulation file exists
            File simFile = new File(info.simulationFile);
            if (!simFile.exists()) {
                System.out.println(ANSI_RED + "✗ SKIPPED - Simulation file not found" + ANSI_RESET);
                skipped++;
                continue;
            }
            
            try {
                // Add log listener to capture output
                final List<String> capturedLogs = new ArrayList<>();
                LogListener listener = new LogListener() {
                    @Override
                    public void onLogEntry(LogEntry entry) {
                        String log = entry.getMessage();
                        capturedLogs.add(log);
                        // Print interesting logs in blue
                        for (String pattern : info.expectedPatterns) {
                            if (log.toLowerCase().contains(pattern.toLowerCase())) {
                                System.out.println(ANSI_BLUE + "  [LOG] " + log + ANSI_RESET);
                                break;
                            }
                        }
                    }
                };
                
                System.out.println("\nRunning simulation...");
                SimulationResult result = runner.runSimulation(info.simulationFile, info.duration, listener);
                
                // Verify expected patterns
                System.out.println("\nVerifying patterns:");
                boolean allPatternsFound = true;
                for (String pattern : info.expectedPatterns) {
                    boolean found = capturedLogs.stream()
                        .anyMatch(log -> log.toLowerCase().contains(pattern.toLowerCase()));
                    if (found) {
                        System.out.println(ANSI_GREEN + "  ✓ Found: " + pattern + ANSI_RESET);
                    } else {
                        System.out.println(ANSI_RED + "  ✗ Missing: " + pattern + ANSI_RESET);
                        allPatternsFound = false;
                    }
                }
                
                // Check result
                System.out.println("\nResult summary:");
                System.out.println("  Total logs captured: " + result.getAllLogs().size());
                System.out.println("  Processes: " + result.getMetrics().getNumProcesses());
                System.out.println("  Message counts: " + result.getMetrics().getProcessMessageCounts());
                
                if (allPatternsFound && result.getAllLogs().size() > 0) {
                    System.out.println(ANSI_GREEN + "✓ PASSED" + ANSI_RESET);
                    passed++;
                } else {
                    System.out.println(ANSI_RED + "✗ FAILED - Not all patterns found or no logs captured" + ANSI_RESET);
                    failed++;
                }
                
            } catch (Exception e) {
                System.out.println(ANSI_RED + "✗ FAILED: " + e.getMessage() + ANSI_RESET);
                e.printStackTrace();
                failed++;
            }
        }
        
        // Cleanup
        runner.shutdown();
        
        // Print summary
        System.out.println("\n" + "=".repeat(50));
        System.out.println("Test Summary:");
        System.out.println(ANSI_GREEN + "  Passed: " + passed + ANSI_RESET);
        System.out.println(ANSI_RED + "  Failed: " + failed + ANSI_RESET);
        System.out.println(ANSI_YELLOW + "  Skipped: " + skipped + ANSI_RESET);
        System.out.println("  Total: " + tests.size());
        System.out.println("=".repeat(50));
        
        // Exit with appropriate code
        System.exit(failed > 0 ? 1 : 0);
    }
    
    // Helper class to store test information
    static class TestInfo {
        final String simulationFile;
        final long duration;
        final List<String> expectedPatterns;
        
        TestInfo(String simulationFile, long duration, List<String> expectedPatterns) {
            this.simulationFile = simulationFile;
            this.duration = duration;
            this.expectedPatterns = expectedPatterns;
        }
    }
}