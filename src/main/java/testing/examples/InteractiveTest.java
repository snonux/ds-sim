package testing.examples;

import testing.*;
import java.util.Scanner;

public class InteractiveTest {
    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        HeadlessSimulationRunner runner = new HeadlessSimulationRunner();
        
        System.out.println("=== Interactive Headless Test ===");
        System.out.println("\nAvailable simulations:");
        System.out.println("1. ping-pong.dat");
        System.out.println("2. broadcast.dat");
        System.out.println("3. berkeley.dat");
        System.out.println("4. raft-working.dat");
        
        System.out.print("\nEnter simulation filename (or full path): ");
        String filename = scanner.nextLine();
        
        // Add saved-simulations/ prefix if not present
        if (!filename.contains("/")) {
            filename = "saved-simulations/" + filename;
        }
        
        System.out.print("Run duration in ms (default 2000): ");
        String durationStr = scanner.nextLine();
        long duration = durationStr.isEmpty() ? 2000 : Long.parseLong(durationStr);
        
        System.out.print("Pattern to search for (optional): ");
        String pattern = scanner.nextLine();
        
        try {
            System.out.println("\nRunning simulation...");
            SimulationResult result = runner.runSimulation(filename, duration);
            
            System.out.println("\nResults:");
            System.out.println("- Total logs: " + result.getAllLogs().size());
            System.out.println("- Processes: " + result.getMetrics().getNumProcesses());
            
            if (!pattern.isEmpty()) {
                int count = result.countLogs(pattern);
                System.out.println("- Pattern '" + pattern + "' found: " + count + " times");
                
                if (count > 0) {
                    System.out.println("\nMatching logs:");
                    result.findAll(pattern).stream()
                        .limit(5)
                        .forEach(log -> System.out.println("  " + log));
                }
            }
            
            System.out.println("\nFirst 10 logs:");
            result.getAllLogs().stream()
                .limit(10)
                .forEach(log -> System.out.println("  [" + log.getTimestamp() + "] " + 
                                                  log.getMessage()));
                                                  
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        } finally {
            runner.shutdown();
            scanner.close();
        }
    }
}