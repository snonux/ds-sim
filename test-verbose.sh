#!/bin/bash
#
# Test verbose logging for DS-Sim protocols
#

echo "DS-Sim Verbose Logging Test"
echo "=========================="
echo
echo "This demonstrates real-time logging during protocol simulation."
echo

# Compile if needed
if [ ! -d "target/classes" ]; then
    echo "Building project..."
    mvn compile -q || { echo "Build failed!"; exit 1; }
fi

# Run ping-pong for just 2 seconds with verbose output
echo "Running ping-pong protocol for 2 seconds with verbose output..."
echo

# Create a simple test that runs for a limited time
cat > /tmp/TestVerbose.java << 'EOF'
import testing.*;

public class TestVerbose {
    public static void main(String[] args) throws Exception {
        String simFile = args.length > 0 ? args[0] : "saved-simulations/ping-pong.dat";
        int duration = args.length > 1 ? Integer.parseInt(args[1]) : 2000;
        
        System.out.println("Loading: " + simFile);
        System.out.println("Duration: " + duration + "ms");
        System.out.println("\n--- Real-Time Log Output ---\n");
        
        HeadlessSimulationRunner runner = new HeadlessSimulationRunner();
        runner.setPrintLogs(true);
        
        try {
            SimulationResult result = runner.runSimulation(simFile, duration);
            System.out.println("\n--- Simulation Complete ---");
            System.out.println("Total events: " + result.getAllLogs().size());
        } finally {
            runner.shutdown();
        }
    }
}
EOF

# Compile and run the test
javac -cp target/classes /tmp/TestVerbose.java -d /tmp
java -cp /tmp:target/classes:target/test-classes -Djava.awt.headless=true TestVerbose "$@"

# Clean up
rm -f /tmp/TestVerbose.java /tmp/TestVerbose.class