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
 * Creates a simple working Raft simulation.
 * The key insight: Raft protocol uses HAS_ON_SERVER_START, so servers
 * automatically start when activated. We just need to activate them!
 */
public class CreateSimpleRaftSimulation {
    
    private static final String RAFT_PROTOCOL = "protocols.implementations.VSRaftProtocol";
    
    public static void main(String[] args) throws Exception {
        // Initialize
        VSDefaultPrefs prefs = new VSDefaultPrefs();
        prefs.fillWithDefaults();
        VSRegisteredEvents.init(prefs);
        
        // Create frame and simulator
        VSSimulatorFrame frame = new VSSimulatorFrame(prefs, null);
        VSSimulator simulator = new VSSimulator(prefs, frame);
        frame.addSimulator(simulator);
        
        // Access visualization via reflection
        java.lang.reflect.Field vizField = VSSimulator.class.getDeclaredField("simulatorVisualization");
        vizField.setAccessible(true);
        VSSimulatorVisualization viz = (VSSimulatorVisualization) vizField.get(simulator);
        
        // Add more processes - we want 5 total (3 servers, 2 clients)
        while (viz.getNumProcesses() < 5) {
            java.lang.reflect.Method addProcessMethod = VSSimulatorVisualization.class.getDeclaredMethod("addProcess");
            addProcessMethod.setAccessible(true);
            addProcessMethod.invoke(viz);
        }
        
        VSTaskManager taskManager = viz.getTaskManager();
        
        // Activate Raft SERVERS on processes 0, 1, 2
        // Since Raft uses HAS_ON_SERVER_START, onServerStart() will be called automatically!
        System.out.println("Creating Raft server activations...");
        for (int i = 0; i < 3; i++) {
            VSProtocolEvent serverEvent = new VSProtocolEvent();
            serverEvent.setProtocolClassname(RAFT_PROTOCOL);
            serverEvent.isClientProtocol(false); // Server mode
            serverEvent.isProtocolActivation(true); // Activation
            
            // Activate at time 0
            VSTask task = new VSTask(0, viz.getProcess(i), serverEvent, false);
            taskManager.addTask(task);
            System.out.println("  - Server " + i + " will activate at time 0");
        }
        
        // Activate Raft CLIENTS on processes 3, 4
        // Clients will react to server heartbeats and start sending requests
        System.out.println("\nCreating Raft client activations...");
        for (int i = 3; i < 5; i++) {
            VSProtocolEvent clientEvent = new VSProtocolEvent();
            clientEvent.setProtocolClassname(RAFT_PROTOCOL);
            clientEvent.isClientProtocol(true); // Client mode
            clientEvent.isProtocolActivation(true); // Activation
            
            // Activate clients a bit later so servers have time to elect leader
            VSTask task = new VSTask(300 + (i-3)*100, viz.getProcess(i), clientEvent, false);
            taskManager.addTask(task);
            System.out.println("  - Client " + (i-3) + " will activate at time " + (300 + (i-3)*100));
        }
        
        // Add crash/recovery to demonstrate leader re-election
        System.out.println("\nAdding failure scenarios...");
        
        // Crash server 0 at time 1000
        VSProcessCrashEvent crash = new VSProcessCrashEvent();
        VSTask crashTask = new VSTask(1000, viz.getProcess(0), crash, false);
        taskManager.addTask(crashTask);
        System.out.println("  - Server 0 will crash at time 1000");
        
        // Recover server 0 at time 1500
        VSProcessRecoverEvent recover = new VSProcessRecoverEvent();
        VSTask recoverTask = new VSTask(1500, viz.getProcess(0), recover, false);
        taskManager.addTask(recoverTask);
        System.out.println("  - Server 0 will recover at time 1500");
        
        // Save simulation
        File outputFile = new File("saved-simulations/raft-simple.dat");
        outputFile.getParentFile().mkdirs();
        
        VSSerialize serialize = new VSSerialize();
        serialize.saveSimulator(outputFile.getAbsolutePath(), simulator);
        
        frame.dispose();
        
        System.out.println("\n===========================================");
        System.out.println("Simple Raft simulation saved successfully!");
        System.out.println("===========================================");
        System.out.println("\nFile: " + outputFile.getAbsolutePath());
        System.out.println("\nWhat happens in this simulation:");
        System.out.println("1. Time 0: Three Raft servers start and begin leader election");
        System.out.println("2. Time ~150-300: One server becomes leader (watch for election messages)");
        System.out.println("3. Time 300: First client activates and starts sending requests");
        System.out.println("4. Time 400: Second client activates and starts sending requests");
        System.out.println("5. Time 1000: Server 0 crashes, triggering new leader election");
        System.out.println("6. Time 1500: Server 0 recovers and rejoins as follower");
        System.out.println("\nTo run the simulation:");
        System.out.println("1. java -jar target/ds-sim-1.0.1-SNAPSHOT.jar");
        System.out.println("2. File -> Open -> saved-simulations/raft-simple.dat");
        System.out.println("3. Click 'Run' and watch the Raft consensus in action!");
        System.out.println("\nLook for:");
        System.out.println("- REQUEST_VOTE and VOTE_RESPONSE messages during elections");
        System.out.println("- APPEND_ENTRIES messages (heartbeats) from leader");
        System.out.println("- CLIENT_REQUEST messages and their processing");
        
        System.exit(0);
    }
}