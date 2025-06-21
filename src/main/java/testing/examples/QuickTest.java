package testing.examples;

import testing.*;

public class QuickTest {
    public static void main(String[] args) throws Exception {
        // Use command line arg or default
        String simulationFile = args.length > 0 ? args[0] : "saved-simulations/ping-pong.dat";
        long duration = args.length > 1 ? Long.parseLong(args[1]) : 1000;
        
        if (args.length == 0) {
            System.out.println("=== Quick Headless Test ===\n");
        }
        
        HeadlessSimulationRunner runner = new HeadlessSimulationRunner();
        
        try {
            SimulationResult result = runner.runSimulation(
                simulationFile, 
                duration
            );
            
            System.out.println("Captured " + result.getAllLogs().size() + " logs");
            System.out.println("\nFirst 5 logs:");
            result.getAllLogs().stream()
                .limit(5)
                .forEach(log -> System.out.println("  " + log));
            
            // Simple verification
            boolean hasActivation = result.countLogs("activated") > 0;
            boolean hasMessages = result.countLogs("Message") > 0;
            
            System.out.println("\n✓ Protocol activated: " + hasActivation);
            System.out.println("✓ Messages exchanged: " + hasMessages);
            
        } finally {
            runner.shutdown();
        }
    }
}