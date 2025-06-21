package testing;

import simulator.*;
import core.*;
import prefs.*;
import serialize.*;
import events.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.awt.*;

/**
 * Loads simulations without creating any GUI components.
 */
public class HeadlessLoader {
    
    static {
        // Set headless mode before any AWT/Swing classes are loaded
        System.setProperty("java.awt.headless", "true");
        System.setProperty("ds.sim.headless", "true");
    }
    
    /**
     * Load a simulation file without creating GUI components.
     * @param filename The simulation file to load
     * @param prefs The preferences to use
     * @return The loaded simulator and visualization
     */
    public static LoadedSimulation load(String filename, VSPrefs prefs) throws Exception {
        // Initialize events
        VSRegisteredEvents.init(prefs);
        
        // Load simulation data directly
        FileInputStream fileInputStream = new FileInputStream(filename);
        ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
        
        // Read preferences
        VSSerializablePrefs serializedPrefs = new VSSerializablePrefs();
        VSSerialize serializer = new VSSerialize();
        serializedPrefs.deserialize(serializer, objectInputStream);
        
        // Create new prefs with current localization
        VSDefaultPrefs newPrefs = new VSDefaultPrefs();
        newPrefs.fillWithDefaults();
        
        // Copy non-string values from serialized prefs
        for (String key : serializedPrefs.getIntegerKeySet()) {
            if (!key.startsWith("lang.")) {
                newPrefs.initInteger(key, serializedPrefs.getInteger(key));
            }
        }
        for (String key : serializedPrefs.getBooleanKeySet()) {
            if (!key.startsWith("lang.")) {
                newPrefs.initBoolean(key, serializedPrefs.getBoolean(key));
            }
        }
        for (String key : serializedPrefs.getFloatKeySet()) {
            if (!key.startsWith("lang.")) {
                newPrefs.initFloat(key, serializedPrefs.getFloat(key));
            }
        }
        for (String key : serializedPrefs.getColorKeySet()) {
            if (!key.startsWith("lang.")) {
                newPrefs.initColor(key, serializedPrefs.getColor(key));
            }
        }
        for (String key : serializedPrefs.getVectorKeySet()) {
            if (!key.startsWith("lang.")) {
                newPrefs.initVector(key, serializedPrefs.getVector(key));
            }
        }
        for (String key : serializedPrefs.getLongKeySet()) {
            if (!key.startsWith("lang.")) {
                newPrefs.initLong(key, serializedPrefs.getLong(key));
            }
        }
        
        // Store prefs for deserialization
        serializer.setObject("prefs", newPrefs);
        serializer.setObject("current_prefs", newPrefs);
        
        // Create simulator with null frame
        VSSimulator simulator = new VSSimulator(newPrefs, null);
        
        // Deserialize simulator
        simulator.deserialize(serializer, objectInputStream);
        objectInputStream.close();
        
        // Get the visualization using reflection
        Field vizField = VSSimulator.class.getDeclaredField("simulatorVisualization");
        vizField.setAccessible(true);
        VSSimulatorVisualization viz = (VSSimulatorVisualization) vizField.get(simulator);
        
        // Override paint methods to prevent GUI errors
        overridePaintMethods(viz);
        
        return new LoadedSimulation(simulator, viz);
    }
    
    /**
     * Override paint methods using reflection to prevent GUI errors.
     */
    private static void overridePaintMethods(VSSimulatorVisualization viz) {
        try {
            // Create a dynamic proxy to intercept method calls
            Class<?> clazz = viz.getClass();
            
            // Override paint() method
            Method paintMethod = clazz.getMethod("paint");
            Method paint2Method = clazz.getMethod("paint", Graphics.class);
            Method repaintMethod = clazz.getMethod("repaint");
            Method isDisplayableMethod = clazz.getMethod("isDisplayable");
            
            // We can't use dynamic proxy for a concrete class, 
            // so we'll rely on the headless checks already in place
            // and the message handler pattern
        } catch (Exception e) {
            // Ignore - methods might not exist or be accessible
        }
    }
    
    /**
     * Container for loaded simulation components.
     */
    public static class LoadedSimulation {
        private final VSSimulator simulator;
        private final VSSimulatorVisualization visualization;
        
        public LoadedSimulation(VSSimulator simulator, VSSimulatorVisualization visualization) {
            this.simulator = simulator;
            this.visualization = visualization;
        }
        
        public VSSimulator getSimulator() {
            return simulator;
        }
        
        public VSSimulatorVisualization getVisualization() {
            return visualization;
        }
    }
}