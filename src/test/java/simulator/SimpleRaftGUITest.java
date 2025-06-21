package simulator;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import core.*;
import prefs.*;
import events.*;
import serialize.VSSerialize;

import java.io.File;
import java.lang.reflect.*;

/**
 * Simple GUI test for Raft simulation to verify it loads and runs.
 */
public class SimpleRaftGUITest {
    
    @Test
    @DisplayName("Test loading Raft simulation file")
    public void testLoadRaftSimulation() throws Exception {
        // Initialize
        VSDefaultPrefs prefs = new VSDefaultPrefs();
        prefs.fillWithDefaults();
        VSRegisteredEvents.init(prefs);
        
        // Check if simulation file exists
        File simFile = new File("saved-simulations/raft-working.dat");
        assertTrue(simFile.exists(), "Raft simulation file should exist");
        
        // Load simulation
        VSSimulatorFrame frame = new VSSimulatorFrame(prefs, null);
        VSSerialize serialize = new VSSerialize();
        VSSimulator simulator = serialize.openSimulator(simFile.getAbsolutePath(), frame);
        
        assertNotNull(simulator, "Simulator should be loaded");
        
        // Access visualization
        Field vizField = VSSimulator.class.getDeclaredField("simulatorVisualization");
        vizField.setAccessible(true);
        VSSimulatorVisualization viz = (VSSimulatorVisualization) vizField.get(simulator);
        
        // Verify basic properties
        assertTrue(viz.getNumProcesses() >= 5, "Should have at least 5 processes");
        
        // Check task manager
        VSTaskManager taskManager = viz.getTaskManager();
        assertNotNull(taskManager, "Task manager should exist");
        
        // Get task count using reflection
        Field tasksField = VSTaskManager.class.getDeclaredField("tasks");
        tasksField.setAccessible(true);
        Object taskQueue = tasksField.get(taskManager);
        Method sizeMethod = taskQueue.getClass().getMethod("size");
        int taskCount = (Integer) sizeMethod.invoke(taskQueue);
        
        assertTrue(taskCount > 0, "Should have scheduled tasks");
        
        frame.dispose();
        
        System.out.println("\n=== Test Results ===");
        System.out.println("✓ Raft simulation loads successfully");
        System.out.println("✓ Processes: " + viz.getNumProcesses());
        System.out.println("✓ Scheduled tasks: " + taskCount);
    }
}