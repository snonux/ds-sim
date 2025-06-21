package testing;

import java.io.*;

/**
 * Test that verifies the GUI decoupling is working correctly.
 * This should run without any GUI errors in headless mode.
 */
public class TestNoGuiErrors {
    
    public static void main(String[] args) {
        System.out.println("=== Testing GUI Decoupling ===");
        System.out.println("This test should produce NO GUI errors.\n");
        
        // Set headless mode
        System.setProperty("java.awt.headless", "true");
        System.setProperty("ds.sim.headless", "true");
        
        // Capture stderr to check for errors
        ByteArrayOutputStream errStream = new ByteArrayOutputStream();
        PrintStream originalErr = System.err;
        System.setErr(new PrintStream(errStream));
        
        boolean success = true;
        
        try {
            // Test 1: Basic simulation loading and running
            System.out.println("Test 1: Loading and running ping-pong simulation...");
            HeadlessSimulationRunner runner = new HeadlessSimulationRunner();
            runner.setPrintLogs(false); // Quiet mode
            
            SimulationResult result = runner.runSimulation("saved-simulations/ping-pong.dat", 1000);
            
            if (result != null && result.getAllLogs().size() > 0) {
                System.out.println("✓ Simulation ran successfully");
                System.out.println("  Captured " + result.getAllLogs().size() + " log entries");
            } else {
                System.out.println("✗ Simulation failed to produce logs");
                success = false;
            }
            
            runner.shutdown();
            
            // Test 2: Check for GUI errors
            System.out.println("\nTest 2: Checking for GUI errors...");
            String errors = errStream.toString();
            
            if (errors.contains("Component must have a valid peer")) {
                System.out.println("✗ FAILED: Found 'Component must have a valid peer' error");
                success = false;
            } else {
                System.out.println("✓ No 'valid peer' errors");
            }
            
            if (errors.contains("IllegalStateException") && errors.contains("paint")) {
                System.out.println("✗ FAILED: Found paint-related IllegalStateException");
                success = false;
            } else {
                System.out.println("✓ No paint-related exceptions");
            }
            
            if (errors.contains("createBufferStrategy")) {
                System.out.println("✗ FAILED: Found buffer strategy errors");
                success = false;
            } else {
                System.out.println("✓ No buffer strategy errors");
            }
            
            // Test 3: Run multiple simulations
            System.out.println("\nTest 3: Running multiple simulations...");
            String[] simulations = {
                "broadcast.dat",
                "berkeley.dat",
                "basic-multicast.dat"
            };
            
            for (String sim : simulations) {
                try {
                    runner = new HeadlessSimulationRunner();
                    runner.setPrintLogs(false);
                    
                    result = runner.runSimulation("saved-simulations/" + sim, 500);
                    if (result != null && result.getAllLogs().size() > 0) {
                        System.out.println("✓ " + sim + " - OK (" + result.getAllLogs().size() + " logs)");
                    } else {
                        System.out.println("✗ " + sim + " - Failed");
                        success = false;
                    }
                    
                    runner.shutdown();
                } catch (Exception e) {
                    System.out.println("✗ " + sim + " - Exception: " + e.getMessage());
                    success = false;
                }
            }
            
        } catch (Exception e) {
            System.out.println("\n✗ Test failed with exception:");
            e.printStackTrace(System.out);
            success = false;
        } finally {
            System.setErr(originalErr);
        }
        
        // Print captured errors if any
        String capturedErrors = errStream.toString();
        if (!capturedErrors.isEmpty()) {
            System.out.println("\n=== Captured Error Output ===");
            System.out.println(capturedErrors);
            System.out.println("=== End Error Output ===");
        }
        
        // Final result
        System.out.println("\n=== Test Result ===");
        if (success) {
            System.out.println("✅ SUCCESS: GUI decoupling is working correctly!");
            System.out.println("No GUI errors were produced in headless mode.");
        } else {
            System.out.println("❌ FAILED: GUI errors still present.");
            System.out.println("The decoupling is not complete.");
        }
        
        System.exit(success ? 0 : 1);
    }
}