package simulator.engine;

import simulator.*;
import core.*;
import prefs.VSPrefs;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Adapter that allows VSSimulatorVisualization to work with the new SimulationEngine interface.
 * This provides backward compatibility during the refactoring process.
 */
public class VisualizationAdapter {
    
    /**
     * Inject a simulation engine into an existing VSSimulatorVisualization.
     * This replaces direct simulation logic with delegated calls to the engine.
     */
    public static void injectEngine(VSSimulatorVisualization viz, SimulationEngine engine) 
            throws Exception {
        
        // Replace sendMessage method behavior using a proxy
        installSendMessageProxy(viz, engine);
        
        // Sync process list
        syncProcessList(viz, engine);
        
        // Sync task manager
        syncTaskManager(viz, engine);
    }
    
    /**
     * Install a proxy for the sendMessage method that delegates to the engine.
     */
    private static void installSendMessageProxy(VSSimulatorVisualization viz, 
                                                SimulationEngine engine) throws Exception {
        // This is complex with standard Java, so we'll use a different approach
        // We'll override the behavior by setting a flag that the paint method checks
        
        // Set a flag indicating headless mode
        Field headlessField = findOrCreateField(viz, "isHeadlessMode");
        headlessField.setAccessible(true);
        headlessField.set(viz, true);
        
        // Store engine reference
        Field engineField = findOrCreateField(viz, "simulationEngine");
        engineField.setAccessible(true);
        engineField.set(viz, engine);
    }
    
    /**
     * Find a field or create it dynamically if possible.
     */
    private static Field findOrCreateField(Object obj, String fieldName) {
        try {
            return obj.getClass().getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            // In real implementation, we'd need to use bytecode manipulation
            // For now, we'll work with existing fields
            return null;
        }
    }
    
    /**
     * Sync the process list between visualization and engine.
     */
    private static void syncProcessList(VSSimulatorVisualization viz, 
                                       SimulationEngine engine) throws Exception {
        Field processesField = VSSimulatorVisualization.class.getDeclaredField("processes");
        processesField.setAccessible(true);
        
        // Get current processes from viz
        java.util.ArrayList<VSInternalProcess> vizProcesses = 
            (java.util.ArrayList<VSInternalProcess>) processesField.get(viz);
        
        // Add all to engine
        for (VSInternalProcess process : vizProcesses) {
            engine.addProcess(process);
        }
    }
    
    /**
     * Sync the task manager between visualization and engine.
     */
    private static void syncTaskManager(VSSimulatorVisualization viz, 
                                       SimulationEngine engine) throws Exception {
        Field taskManagerField = VSSimulatorVisualization.class.getDeclaredField("taskManager");
        taskManagerField.setAccessible(true);
        
        VSTaskManager vizTaskManager = (VSTaskManager) taskManagerField.get(viz);
        VSTaskManager engineTaskManager = engine.getTaskManager();
        
        // Copy tasks if needed
        // This would require access to internal task manager state
    }
    
    /**
     * Create a headless wrapper for VSSimulatorVisualization that prevents paint operations.
     */
    public static VSSimulatorVisualization createHeadlessWrapper(
            final VSSimulatorVisualization original, 
            final SimulationEngine engine) {
        
        try {
            // Get prefs, simulator, and loging via reflection
            Field prefsField = VSSimulatorVisualization.class.getDeclaredField("prefs");
            prefsField.setAccessible(true);
            VSPrefs prefs = (VSPrefs) prefsField.get(original);
            
            Field simulatorField = VSSimulatorVisualization.class.getDeclaredField("simulator");
            simulatorField.setAccessible(true);
            VSSimulator simulator = (VSSimulator) simulatorField.get(original);
            
            Field logingField = VSSimulatorVisualization.class.getDeclaredField("loging");
            logingField.setAccessible(true);
            VSLogging loging = (VSLogging) logingField.get(original);
            
            // Create a wrapper that intercepts paint calls
            return new VSSimulatorVisualization(prefs, simulator, loging) {
            
            @Override
            public void paint() {
                // Do nothing - no painting in headless mode
            }
            
            @Override
            public void paint(java.awt.Graphics g) {
                // Do nothing
            }
            
            @Override
            public void sendMessage(VSMessage message) {
                // Delegate to engine instead of creating visual elements
                engine.sendMessage(message);
            }
            
            @Override
            public void repaint() {
                // Do nothing
            }
            
            @Override
            public java.awt.image.BufferStrategy getBufferStrategy() {
                return null; // Prevent buffer strategy creation
            }
            
            @Override
            public void createBufferStrategy(int numBuffers) {
                // Do nothing
            }
        };
        } catch (Exception e) {
            throw new RuntimeException("Failed to create headless wrapper", e);
        }
    }
}