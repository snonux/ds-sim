package examples;

import simulator.*;
import core.*;
import prefs.*;
import events.*;
import events.internal.*;
import events.implementations.*;
import serialize.VSSerialize;
import java.io.*;
import java.lang.reflect.*;

/**
 * Creates a working Raft simulation by properly setting up the event queue
 * and ensuring protocols are activated through the normal event system.
 */
public class CreateWorkingRaftSimulation {
    
    private static final String RAFT_PROTOCOL = "protocols.implementations.VSRaftProtocol";
    
    public static void main(String[] args) throws Exception {
        System.out.println("=== Creating Working Raft Simulation ===\n");
        
        // Initialize
        VSDefaultPrefs prefs = new VSDefaultPrefs();
        prefs.fillWithDefaults();
        VSRegisteredEvents.init(prefs);
        
        // Create simulator with frame
        VSSimulatorFrame frame = new VSSimulatorFrame(prefs, null);
        VSSimulator simulator = new VSSimulator(prefs, frame);
        frame.addSimulator(simulator);
        
        // Access visualization
        Field vizField = VSSimulator.class.getDeclaredField("simulatorVisualization");
        vizField.setAccessible(true);
        VSSimulatorVisualization viz = (VSSimulatorVisualization) vizField.get(simulator);
        
        // Add 5 processes (3 servers + 2 clients)
        Method addProcessMethod = VSSimulatorVisualization.class.getDeclaredMethod("addProcess");
        addProcessMethod.setAccessible(true);
        System.out.println("Adding 5 processes...");
        for (int i = 0; i < 5; i++) {
            addProcessMethod.invoke(viz);
        }
        
        VSTaskManager taskManager = viz.getTaskManager();
        
        // Schedule Raft server activations at time 0
        System.out.println("\nScheduling Raft server activations:");
        for (int i = 0; i < 3; i++) {
            VSProtocolEvent serverEvent = new VSProtocolEvent();
            serverEvent.setProtocolClassname(RAFT_PROTOCOL);
            serverEvent.isClientProtocol(false); // Server mode
            serverEvent.isProtocolActivation(true); // This is an activation
            
            VSTask task = new VSTask(0, viz.getProcess(i), serverEvent, false);
            taskManager.addTask(task);
            System.out.println("  - Server " + i + " activation scheduled at time 0");
        }
        
        // Schedule Raft client activations with slight delay
        System.out.println("\nScheduling Raft client activations:");
        for (int i = 3; i < 5; i++) {
            VSProtocolEvent clientEvent = new VSProtocolEvent();
            clientEvent.setProtocolClassname(RAFT_PROTOCOL);
            clientEvent.isClientProtocol(true); // Client mode
            clientEvent.isProtocolActivation(true); // This is an activation
            
            // Start clients after servers have initialized
            long startTime = 500 + (i - 3) * 200;
            VSTask task = new VSTask(startTime, viz.getProcess(i), clientEvent, false);
            taskManager.addTask(task);
            System.out.println("  - Client " + (i-3) + " activation scheduled at time " + startTime);
        }
        
        // Add some interesting events
        System.out.println("\nAdding crash/recovery events:");
        
        // Process 0 crashes at time 2000 and recovers at 3000
        VSProcessCrashEvent crash1 = new VSProcessCrashEvent();
        taskManager.addTask(new VSTask(2000, viz.getProcess(0), crash1, false));
        System.out.println("  - Server 0 crash scheduled at time 2000");
        
        VSProcessRecoverEvent recover1 = new VSProcessRecoverEvent();
        taskManager.addTask(new VSTask(3000, viz.getProcess(0), recover1, false));
        System.out.println("  - Server 0 recovery scheduled at time 3000");
        
        // Process 1 crashes at time 4000 and recovers at 5000
        VSProcessCrashEvent crash2 = new VSProcessCrashEvent();
        taskManager.addTask(new VSTask(4000, viz.getProcess(1), crash2, false));
        System.out.println("  - Server 1 crash scheduled at time 4000");
        
        VSProcessRecoverEvent recover2 = new VSProcessRecoverEvent();
        taskManager.addTask(new VSTask(5000, viz.getProcess(1), recover2, false));
        System.out.println("  - Server 1 recovery scheduled at time 5000");
        
        // Save simulation
        File outputFile = new File("saved-simulations/raft-working.dat");
        outputFile.getParentFile().mkdirs();
        
        System.out.println("\nSaving simulation...");
        VSSerialize serialize = new VSSerialize();
        serialize.saveSimulator(outputFile.getAbsolutePath(), simulator);
        
        frame.dispose();
        
        System.out.println("\n✓ Simulation saved to: " + outputFile.getAbsolutePath());
        
        // Create instruction file
        File instructionFile = new File("saved-simulations/README-raft.txt");
        try (PrintWriter writer = new PrintWriter(instructionFile)) {
            writer.println("RAFT CONSENSUS SIMULATION");
            writer.println("========================");
            writer.println();
            writer.println("This directory contains Raft consensus protocol simulations:");
            writer.println();
            writer.println("1. raft-working.dat - Full working simulation with:");
            writer.println("   - 3 Raft servers (processes 0-2)");
            writer.println("   - 2 Raft clients (processes 3-4)");
            writer.println("   - Server crash/recovery events");
            writer.println();
            writer.println("To run the simulation:");
            writer.println("1. java -jar target/ds-sim-1.0.1-SNAPSHOT.jar");
            writer.println("2. File → Open → saved-simulations/raft-working.dat");
            writer.println("3. Click Run (▶) button");
            writer.println();
            writer.println("What to look for:");
            writer.println("- Leader election (REQUEST_VOTE messages)");
            writer.println("- Heartbeats from leader (APPEND_ENTRIES)");
            writer.println("- Client requests and responses");
            writer.println("- Re-election when servers crash");
            writer.println();
            writer.println("Timeline:");
            writer.println("- Time 0: Servers start, begin leader election");
            writer.println("- Time 500-700: Clients start");
            writer.println("- Time 2000: Server 0 crashes");
            writer.println("- Time 3000: Server 0 recovers");
            writer.println("- Time 4000: Server 1 crashes");
            writer.println("- Time 5000: Server 1 recovers");
        }
        
        System.out.println("✓ Instructions saved to: " + instructionFile.getAbsolutePath());
        
        System.out.println("\n=== Success! ===");
        System.out.println("\nThe Raft simulation has been created with the following setup:");
        System.out.println("- 3 servers implementing Raft consensus");
        System.out.println("- 2 clients that will send requests");
        System.out.println("- Crash/recovery events to test fault tolerance");
        System.out.println("\nRun the simulator and load the file to see it in action!");
    }
}