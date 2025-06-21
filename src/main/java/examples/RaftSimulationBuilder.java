package examples;

import simulator.*;
import core.*;
import prefs.*;
import events.*;
import events.internal.*;
import serialize.VSSerialize;
import java.io.*;

/**
 * Builder for creating Raft simulations programmatically.
 * Uses reflection to access private simulator fields when necessary.
 */
public class RaftSimulationBuilder {
    
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
        
        // Access private field via reflection
        java.lang.reflect.Field vizField = VSSimulator.class.getDeclaredField("simulatorVisualization");
        vizField.setAccessible(true);
        VSSimulatorVisualization viz = (VSSimulatorVisualization) vizField.get(simulator);
        
        // Build Raft simulation
        VSTaskManager taskManager = viz.getTaskManager();
        
        // Add server activations (processes 0,1)
        for (int i = 0; i < 2; i++) {
            VSProtocolEvent serverEvent = new VSProtocolEvent();
            serverEvent.setProtocolClassname(RAFT_PROTOCOL);
            serverEvent.isClientProtocol(false);
            serverEvent.isProtocolActivation(true);
            
            VSTask task = new VSTask(0, viz.getProcess(i), serverEvent, false);
            taskManager.addTask(task);
        }
        
        // Add client activation (process 2)
        VSProtocolEvent clientEvent = new VSProtocolEvent();
        clientEvent.setProtocolClassname(RAFT_PROTOCOL);
        clientEvent.isClientProtocol(true);
        clientEvent.isProtocolActivation(true);
        
        VSTask clientTask = new VSTask(100, viz.getProcess(2), clientEvent, false);
        taskManager.addTask(clientTask);
        
        // Save
        File outputFile = new File("saved-simulations/raft-consensus.dat");
        outputFile.getParentFile().mkdirs();
        
        VSSerialize serialize = new VSSerialize();
        serialize.saveSimulator(outputFile.getAbsolutePath(), simulator);
        
        frame.dispose();
        
        System.out.println("Raft simulation created: " + outputFile.getAbsolutePath());
        System.out.println("\nContains:");
        System.out.println("- 2 Raft servers (processes 0-1)");
        System.out.println("- 1 Raft client (process 2)");
        System.out.println("\nRun with: java -jar target/ds-sim-1.0.1-SNAPSHOT.jar");
        System.out.println("Then open: " + outputFile.getName());
        
        System.exit(0);
    }
}