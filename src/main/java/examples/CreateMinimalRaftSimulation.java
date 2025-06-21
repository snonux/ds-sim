package examples;

import simulator.*;
import core.*;
import prefs.*;
import events.*;
import events.internal.*;
import serialize.VSSerialize;
import java.io.*;
import java.lang.reflect.*;

/**
 * Creates a minimal Raft simulation with just protocol activations.
 * This tests if the basic simulation saving/loading works.
 */
public class CreateMinimalRaftSimulation {
    
    public static void main(String[] args) throws Exception {
        System.out.println("=== Creating Minimal Raft Simulation ===\n");
        
        // Initialize
        VSDefaultPrefs prefs = new VSDefaultPrefs();
        prefs.fillWithDefaults();
        VSRegisteredEvents.init(prefs);
        
        // Create simulator without GUI
        VSSimulatorFrame frame = new VSSimulatorFrame(prefs, null);
        VSSimulator simulator = new VSSimulator(prefs, frame);
        frame.addSimulator(simulator);
        
        // Access visualization via reflection
        Field vizField = VSSimulator.class.getDeclaredField("simulatorVisualization");
        vizField.setAccessible(true);
        VSSimulatorVisualization viz = (VSSimulatorVisualization) vizField.get(simulator);
        
        // Add 3 processes
        Method addProcessMethod = VSSimulatorVisualization.class.getDeclaredMethod("addProcess");
        addProcessMethod.setAccessible(true);
        for (int i = 0; i < 3; i++) {
            addProcessMethod.invoke(viz);
        }
        
        VSTaskManager taskManager = viz.getTaskManager();
        
        // Create only one Raft server activation at time 0
        System.out.println("Adding single Raft server activation on process 0...");
        VSProtocolEvent serverEvent = new VSProtocolEvent();
        serverEvent.setProtocolClassname("protocols.implementations.VSRaftProtocol");
        serverEvent.isClientProtocol(false);
        serverEvent.isProtocolActivation(true);
        
        VSTask task = new VSTask(0, viz.getProcess(0), serverEvent, false);
        taskManager.addTask(task);
        
        // Save simulation
        File outputFile = new File("saved-simulations/raft-minimal.dat");
        outputFile.getParentFile().mkdirs();
        
        VSSerialize serialize = new VSSerialize();
        serialize.saveSimulator(outputFile.getAbsolutePath(), simulator);
        
        frame.dispose();
        
        System.out.println("\nSimulation saved to: " + outputFile.getAbsolutePath());
        System.out.println("\nTo test:");
        System.out.println("1. Run: java -jar target/ds-sim-1.0.1-SNAPSHOT.jar");
        System.out.println("2. File → Open → saved-simulations/raft-minimal.dat");
        System.out.println("3. Click Run button and check the logs");
        
        // Try to immediately load it back to verify
        System.out.println("\nVerifying saved file can be loaded...");
        try {
            VSSimulatorFrame frame2 = new VSSimulatorFrame(prefs, null);
            VSSimulator loaded = serialize.openSimulator(outputFile.getAbsolutePath(), frame2);
            if (loaded != null) {
                System.out.println("✓ File loaded successfully!");
                frame2.dispose();
            } else {
                System.out.println("✗ Failed to load file!");
            }
        } catch (Exception e) {
            System.out.println("✗ Error loading file: " + e.getMessage());
            e.printStackTrace();
        }
    }
}