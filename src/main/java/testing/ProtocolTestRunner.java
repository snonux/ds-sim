package testing;

import java.util.*;

/**
 * Runs all protocol tests and reports results.
 * This is a standalone test runner that doesn't require JUnit.
 */
public class ProtocolTestRunner {
    
    private static class TestCase {
        final String name;
        final String simulationFile;
        final long duration;
        final ProtocolVerifier verifier;
        
        TestCase(String name, String simulationFile, long duration, ProtocolVerifier verifier) {
            this.name = name;
            this.simulationFile = simulationFile;
            this.duration = duration;
            this.verifier = verifier;
        }
    }
    
    public static void main(String[] args) {
        System.out.println("=== DS-Sim Protocol Test Runner ===\n");
        
        // Check for verbose flag
        boolean verbose = args.length > 0 && 
            (args[0].equals("-v") || args[0].equals("--verbose"));
        
        List<TestCase> tests = createTestCases();
        int passed = 0;
        int failed = 0;
        
        HeadlessSimulationRunner runner = new HeadlessSimulationRunner();
        runner.setPrintLogs(verbose);
        
        for (TestCase test : tests) {
            System.out.println("\n" + "=".repeat(60));
            System.out.println("Testing " + test.name);
            System.out.println("Simulation: " + test.simulationFile);
            System.out.println("=".repeat(60));
            
            try {
                SimulationResult result = runner.runSimulation(
                    test.simulationFile, 
                    test.duration
                );
                
                if (!verbose) {
                    System.out.println("\nCaptured " + result.getAllLogs().size() + " log entries");
                }
                
                VerificationResult verification = test.verifier.verify(result.getAllLogs());
                
                if (verification.passed()) {
                    System.out.println("\n✓ PASSED");
                    passed++;
                } else {
                    System.out.println("\n✗ FAILED");
                    System.out.println("  " + verification.getFailureMessage());
                    if (!verbose && result.getAllLogs().size() > 0) {
                        System.out.println("\n  First few logs:");
                        result.getAllLogs().stream()
                            .limit(5)
                            .forEach(log -> System.out.println("    " + log));
                    }
                    failed++;
                }
                
            } catch (Exception e) {
                System.out.println("\n✗ ERROR: " + e.getMessage());
                if (verbose) {
                    e.printStackTrace();
                }
                failed++;
            }
        }
        
        runner.shutdown();
        
        System.out.println("\n" + "=".repeat(60));
        System.out.println("=== Summary ===");
        System.out.println("Total tests: " + tests.size());
        System.out.println("Passed: " + passed);
        System.out.println("Failed: " + failed);
        
        if (failed == 0) {
            System.out.println("\n✓ All tests passed!");
            System.exit(0);
        } else {
            System.out.println("\n✗ Some tests failed!");
            System.out.println("\nRun with -v or --verbose to see detailed logs");
            System.exit(1);
        }
    }
    
    private static List<TestCase> createTestCases() {
        List<TestCase> tests = new ArrayList<>();
        
        // Ping-Pong
        tests.add(new TestCase(
            "Ping-Pong",
            "saved-simulations/ping-pong.dat",
            2000,
            new ProtocolVerifier()
                .expectLog("Ping-Pong.*activated")
                .expectLog("Message sent")
                .expectLog("Message received")
                .expectNoLog("ERROR")
        ));
        
        // Ping-Pong Sturm
        tests.add(new TestCase(
            "Ping-Pong Sturm",
            "saved-simulations/ping-pong-sturm.dat",
            2000,
            new ProtocolVerifier()
                .expectLog("Ping-Pong.*activated")
                .expectLog("Message")
                .expectNoLog("ERROR")
        ));
        
        // Broadcast
        tests.add(new TestCase(
            "Broadcast",
            "saved-simulations/broadcast.dat",
            2000,
            new ProtocolVerifier()
                .expectLog("Broadcast.*activated")
                .expectLog("Message")
                .expectNoLog("ERROR")
        ));
        
        // Basic Multicast
        tests.add(new TestCase(
            "Basic Multicast",
            "saved-simulations/basic-multicast.dat",
            2000,
            new ProtocolVerifier()
                .expectLog("Basic Multicast.*activated|Multicast.*activated")
                .expectLog("Message")
                .expectNoLog("ERROR")
        ));
        
        // Reliable Multicast
        tests.add(new TestCase(
            "Reliable Multicast",
            "saved-simulations/reliable-multicast.dat",
            2000,
            new ProtocolVerifier()
                .expectLog("Reliable Multicast.*activated")
                .expectLog("Message")
                .expectNoLog("ERROR")
        ));
        
        // Berkeley Time Sync
        tests.add(new TestCase(
            "Berkeley Time Sync",
            "saved-simulations/berkeley.dat",
            2000,
            new ProtocolVerifier()
                .expectLog("Berkley.*activated|Berkeley.*activated")
                .expectNoLog("ERROR")
        ));
        
        // Internal Time Sync
        tests.add(new TestCase(
            "Internal Time Sync",
            "saved-simulations/int-sync.dat",
            2000,
            new ProtocolVerifier()
                .expectLog("Internal.*sync.*activated")
                .expectNoLog("ERROR")
        ));
        
        // External vs Internal Sync
        tests.add(new TestCase(
            "External vs Internal Sync",
            "saved-simulations/ext-vs-int-sync.dat",
            2000,
            new ProtocolVerifier()
                .expectLog("activated")
                .expectNoLog("ERROR")
        ));
        
        // One-Phase Commit
        tests.add(new TestCase(
            "One-Phase Commit",
            "saved-simulations/one-phase-commit.dat",
            2000,
            new ProtocolVerifier()
                .expectLog("1-Phase Commit.*activated")
                .expectNoLog("ERROR")
        ));
        
        // Two-Phase Commit
        tests.add(new TestCase(
            "Two-Phase Commit",
            "saved-simulations/two-phase-commit.dat",
            2000,
            new ProtocolVerifier()
                .expectLog("2-Phase Commit.*activated")
                .expectNoLog("ERROR")
        ));
        
        // Slow Connection
        tests.add(new TestCase(
            "Slow Connection",
            "saved-simulations/slow-connection.dat",
            2000,
            new ProtocolVerifier()
                .expectLog("activated")
                .expectNoLog("ERROR")
        ));
        
        return tests;
    }
}