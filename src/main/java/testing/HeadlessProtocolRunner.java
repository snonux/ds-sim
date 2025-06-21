package testing;

import java.io.File;
import java.util.*;

/**
 * Runs protocol tests in headless mode without GUI errors.
 * This replaces the old test runners that had GUI dependency issues.
 */
public class HeadlessProtocolRunner {
    
    public static void main(String[] args) throws Exception {
        System.out.println("=== DS-Sim Headless Protocol Test Runner ===\n");
        
        // Check for verbose mode
        boolean verbose = Boolean.getBoolean("ds.sim.verbose");
        
        if (args.length > 0) {
            // Run specific simulation
            runSingleSimulation(args[0], verbose);
        } else {
            // Run all simulations
            runAllSimulations(verbose);
        }
    }
    
    private static void runSingleSimulation(String simFile, boolean verbose) throws Exception {
        System.out.println("Running simulation: " + simFile);
        System.out.println("-".repeat(50));
        
        HeadlessSimulationRunner runner = new HeadlessSimulationRunner();
        runner.setPrintLogs(verbose);
        
        try {
            long startTime = System.currentTimeMillis();
            SimulationResult result = runner.runSimulation(simFile, 5000); // 5 second timeout
            long duration = System.currentTimeMillis() - startTime;
            
            System.out.println("✓ Completed in " + duration + "ms");
            System.out.println("  Processes: " + result.getMetrics().getNumProcesses());
            System.out.println("  Log entries: " + result.getMetrics().getTotalLogCount());
            System.out.println("  Messages per process: " + result.getMetrics().getProcessMessageCounts());
            
            if (verbose) {
                System.out.println("\n--- Log Output ---");
                for (LogEntry log : result.getAllLogs()) {
                    System.out.println(log.toString());
                }
            }
            
            System.out.println();
        } catch (Exception e) {
            System.err.println("✗ FAILED: " + e.getMessage());
            if (verbose) {
                e.printStackTrace();
            }
        } finally {
            runner.shutdown();
        }
    }
    
    private static void runAllSimulations(boolean verbose) throws Exception {
        File simDir = new File("saved-simulations");
        File[] simFiles = simDir.listFiles((dir, name) -> name.endsWith(".dat"));
        
        if (simFiles == null || simFiles.length == 0) {
            System.out.println("No simulation files found in saved-simulations/");
            return;
        }
        
        Arrays.sort(simFiles);
        
        System.out.println("Found " + simFiles.length + " simulations to test\n");
        
        int passed = 0;
        int failed = 0;
        List<String> failures = new ArrayList<>();
        
        for (File simFile : simFiles) {
            System.out.println("Testing: " + simFile.getName());
            System.out.println("-".repeat(50));
            
            HeadlessSimulationRunner runner = new HeadlessSimulationRunner();
            runner.setPrintLogs(false); // Don't print logs when running all tests
            
            try {
                long startTime = System.currentTimeMillis();
                SimulationResult result = runner.runSimulation(simFile.getPath(), 3000); // 3 second timeout
                long duration = System.currentTimeMillis() - startTime;
                
                System.out.println("✓ PASSED in " + duration + "ms");
                System.out.println("  Logs: " + result.getMetrics().getTotalLogCount());
                passed++;
                
            } catch (Exception e) {
                System.err.println("✗ FAILED: " + e.getMessage());
                failed++;
                failures.add(simFile.getName() + " - " + e.getMessage());
            } finally {
                runner.shutdown();
            }
            
            System.out.println();
        }
        
        // Summary
        System.out.println("=".repeat(60));
        System.out.println("Test Summary:");
        System.out.println("  Total: " + simFiles.length);
        System.out.println("  Passed: " + passed);
        System.out.println("  Failed: " + failed);
        
        if (!failures.isEmpty()) {
            System.out.println("\nFailures:");
            for (String failure : failures) {
                System.out.println("  - " + failure);
            }
        }
        
        System.out.println();
        System.exit(failed > 0 ? 1 : 0);
    }
}