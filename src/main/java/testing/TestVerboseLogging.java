package testing;

/**
 * Simple test to demonstrate verbose logging during simulation.
 */
public class TestVerboseLogging {
    
    public static void main(String[] args) throws Exception {
        System.out.println("=== Testing Verbose Logging ===\n");
        
        // Run a short simulation with verbose logging
        HeadlessSimulationRunner runner = new HeadlessSimulationRunner();
        runner.setPrintLogs(true); // Enable real-time log output
        
        System.out.println("Starting simulation with real-time log output...\n");
        
        try {
            SimulationResult result = runner.runSimulation("saved-simulations/ping-pong.dat", 1000);
            
            System.out.println("\n=== Simulation Complete ===");
            System.out.println("Total logs captured: " + result.getAllLogs().size());
            System.out.println("Processes: " + result.getMetrics().getNumProcesses());
        } finally {
            runner.shutdown();
        }
    }
}