package examples;

import simulator.*;
import core.*;
import prefs.*;
import events.*;
import events.internal.*;
import events.implementations.*;
import serialize.VSSerialize;
import java.io.*;

/**
 * Creates a Raft simulation and verifies it can be loaded properly.
 */
public class CreateAndVerifyRaftSimulation {
    
    private static final String RAFT_PROTOCOL = "protocols.implementations.VSRaftProtocol";
    
    public static void main(String[] args) throws Exception {
        System.out.println("=== Creating and Verifying Raft Simulation ===\n");
        
        // Initialize
        VSDefaultPrefs prefs = new VSDefaultPrefs();
        prefs.fillWithDefaults();
        VSRegisteredEvents.init(prefs);
        
        // Step 1: Create the simulation
        System.out.println("Step 1: Creating Raft simulation...");
        
        VSSimulatorFrame frame = new VSSimulatorFrame(prefs, null);
        VSSimulator simulator = new VSSimulator(prefs, frame);
        frame.addSimulator(simulator);
        
        // Access visualization
        java.lang.reflect.Field vizField = VSSimulator.class.getDeclaredField("simulatorVisualization");
        vizField.setAccessible(true);
        VSSimulatorVisualization viz = (VSSimulatorVisualization) vizField.get(simulator);
        
        // Add processes (5 total: 3 servers + 2 clients)
        while (viz.getNumProcesses() < 5) {
            java.lang.reflect.Method addProcessMethod = VSSimulatorVisualization.class.getDeclaredMethod("addProcess");
            addProcessMethod.setAccessible(true);
            addProcessMethod.invoke(viz);
        }
        
        VSTaskManager taskManager = viz.getTaskManager();
        
        // Add Raft server activations
        System.out.println("  - Adding 3 Raft servers");
        for (int i = 0; i < 3; i++) {
            VSProtocolEvent serverEvent = new VSProtocolEvent();
            serverEvent.setProtocolClassname(RAFT_PROTOCOL);
            serverEvent.isClientProtocol(false);
            serverEvent.isProtocolActivation(true);
            
            VSTask task = new VSTask(0, viz.getProcess(i), serverEvent, false);
            taskManager.addTask(task);
        }
        
        // Add Raft client activations
        System.out.println("  - Adding 2 Raft clients");
        for (int i = 3; i < 5; i++) {
            VSProtocolEvent clientEvent = new VSProtocolEvent();
            clientEvent.setProtocolClassname(RAFT_PROTOCOL);
            clientEvent.isClientProtocol(true);
            clientEvent.isProtocolActivation(true);
            
            // Stagger client starts
            VSTask task = new VSTask(200 + (i-3)*100, viz.getProcess(i), clientEvent, false);
            taskManager.addTask(task);
        }
        
        // Add some events
        System.out.println("  - Adding crash/recovery events");
        
        // Server 0 crashes at 1000, recovers at 1500
        VSProcessCrashEvent crash = new VSProcessCrashEvent();
        taskManager.addTask(new VSTask(1000, viz.getProcess(0), crash, false));
        
        VSProcessRecoverEvent recover = new VSProcessRecoverEvent();
        taskManager.addTask(new VSTask(1500, viz.getProcess(0), recover, false));
        
        // Save simulation
        File outputFile = new File("saved-simulations/raft-verified.dat");
        outputFile.getParentFile().mkdirs();
        
        VSSerialize serialize = new VSSerialize();
        serialize.saveSimulator(outputFile.getAbsolutePath(), simulator);
        
        frame.dispose();
        
        System.out.println("  ✓ Simulation saved to: " + outputFile.getName());
        
        // Step 2: Verify the simulation can be loaded
        System.out.println("\nStep 2: Loading and verifying simulation...");
        
        VSSimulatorFrame frame2 = new VSSimulatorFrame(prefs, null);
        VSSimulator loadedSim = serialize.openSimulator(outputFile.getAbsolutePath(), frame2);
        
        if (loadedSim == null) {
            System.err.println("  ✗ Failed to load simulation!");
            System.exit(1);
        }
        
        // Verify contents
        vizField = VSSimulator.class.getDeclaredField("simulatorVisualization");
        vizField.setAccessible(true);
        VSSimulatorVisualization loadedViz = (VSSimulatorVisualization) vizField.get(loadedSim);
        
        System.out.println("  ✓ Simulation loaded successfully");
        System.out.println("  - Processes: " + loadedViz.getNumProcesses());
        
        // Check tasks
        VSTaskManager loadedTaskManager = loadedViz.getTaskManager();
        java.lang.reflect.Field tasksField = VSTaskManager.class.getDeclaredField("tasks");
        tasksField.setAccessible(true);
        Object taskQueue = tasksField.get(loadedTaskManager);
        java.lang.reflect.Method sizeMethod = taskQueue.getClass().getMethod("size");
        int taskCount = (Integer) sizeMethod.invoke(taskQueue);
        
        System.out.println("  - Scheduled tasks: " + taskCount);
        
        frame2.dispose();
        
        // Step 3: Provide instructions
        System.out.println("\n=== Success! ===");
        System.out.println("\nTo run the Raft simulation:");
        System.out.println("1. Start the simulator:");
        System.out.println("   java -jar target/ds-sim-1.0.1-SNAPSHOT.jar");
        System.out.println("\n2. Load the simulation:");
        System.out.println("   File → Open → saved-simulations/raft-verified.dat");
        System.out.println("\n3. Run the simulation:");
        System.out.println("   Click the 'Run' button (▶)");
        System.out.println("\n4. What to look for:");
        System.out.println("   - Leader election messages (REQUEST_VOTE, VOTE_RESPONSE)");
        System.out.println("   - Heartbeats from leader (APPEND_ENTRIES)");
        System.out.println("   - Client requests and responses");
        System.out.println("   - Re-election when server 0 crashes at time 1000");
        
        System.exit(0);
    }
}