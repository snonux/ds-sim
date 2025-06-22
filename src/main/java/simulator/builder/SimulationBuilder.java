package simulator.builder;

import simulator.*;
import core.*;
import prefs.*;
import events.*;
import events.internal.*;
import serialize.VSSerialize;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.ArrayList;

/**
 * Builder for creating DS-Sim simulations programmatically without GUI.
 * 
 * Example usage:
 * <pre>
 * SimulationBuilder builder = new SimulationBuilder()
 *     .withProcesses(5)
 *     .withProtocol("protocols.implementations.VSRaftProtocol")
 *     .activateServers(0, 1, 2)
 *     .activateClients(3, 4)
 *     .addCrashEvent(0, 2000)
 *     .addRecoveryEvent(0, 3000)
 *     .save("saved-simulations/my-raft.dat");
 * </pre>
 */
public class SimulationBuilder {
    
    private VSDefaultPrefs prefs;
    private VSSimulator simulator;
    private VSSimulatorVisualization visualization;
    private VSTaskManager taskManager;
    private String protocolClass;
    private int numProcesses = 3; // default
    private List<ScheduledTask> scheduledTasks = new ArrayList<>();
    private int simulationDuration = 10000; // default 10 seconds
    
    /**
     * Internal class to hold task scheduling information
     */
    private static class ScheduledTask {
        long time;
        int processId;
        VSAbstractEvent event;
        boolean isGlobalTimed;
        
        ScheduledTask(long time, int processId, VSAbstractEvent event, boolean isGlobalTimed) {
            this.time = time;
            this.processId = processId;
            this.event = event;
            this.isGlobalTimed = isGlobalTimed;
        }
    }
    
    /**
     * Initialize the builder with default preferences
     */
    public SimulationBuilder() throws Exception {
        // Initialize preferences
        prefs = new VSDefaultPrefs();
        prefs.fillWithDefaults();
        
        // Initialize registered events
        VSRegisteredEvents.init(prefs);
    }
    
    /**
     * Set the simulation duration in milliseconds
     */
    public SimulationBuilder withDuration(int durationMs) {
        this.simulationDuration = durationMs;
        return this;
    }
    
    /**
     * Set the number of processes in the simulation
     */
    public SimulationBuilder withProcesses(int count) {
        this.numProcesses = count;
        return this;
    }
    
    /**
     * Set the protocol class to use
     */
    public SimulationBuilder withProtocol(String protocolClassName) {
        this.protocolClass = protocolClassName;
        return this;
    }
    
    /**
     * Activate protocol as server on specified processes
     */
    public SimulationBuilder activateServers(int... processIds) {
        for (int pid : processIds) {
            VSProtocolEvent event = new VSProtocolEvent();
            event.onInit(); // Initialize the event first
            setProtocolClassname(event, protocolClass);
            setIsServer(event, true);
            
            scheduledTasks.add(new ScheduledTask(0, pid, event, true));
        }
        return this;
    }
    
    /**
     * Activate protocol as client on specified processes
     */
    public SimulationBuilder activateClients(int... processIds) {
        return activateClients(500, processIds); // default delay
    }
    
    /**
     * Activate protocol as client on specified processes with custom start time
     */
    public SimulationBuilder activateClients(long startTime, int... processIds) {
        for (int i = 0; i < processIds.length; i++) {
            VSProtocolEvent event = new VSProtocolEvent();
            event.onInit(); // Initialize the event first
            setProtocolClassname(event, protocolClass);
            setIsServer(event, false);
            
            // Stagger client starts
            long time = startTime + (i * 200);
            scheduledTasks.add(new ScheduledTask(time, processIds[i], event, true));
        }
        return this;
    }
    
    /**
     * Add a process crash event
     */
    public SimulationBuilder addCrashEvent(int processId, long time) {
        try {
            // Use reflection to create crash event
            Class<?> crashClass = Class.forName("events.implementations.VSProcessCrashEvent");
            VSAbstractEvent crashEvent = (VSAbstractEvent) crashClass.getDeclaredConstructor().newInstance();
            scheduledTasks.add(new ScheduledTask(time, processId, crashEvent, true));
        } catch (Exception e) {
            throw new RuntimeException("Failed to create crash event", e);
        }
        return this;
    }
    
    /**
     * Add a process recovery event
     */
    public SimulationBuilder addRecoveryEvent(int processId, long time) {
        try {
            // Use reflection to create recovery event
            Class<?> recoverClass = Class.forName("events.implementations.VSProcessRecoverEvent");
            VSAbstractEvent recoverEvent = (VSAbstractEvent) recoverClass.getDeclaredConstructor().newInstance();
            scheduledTasks.add(new ScheduledTask(time, processId, recoverEvent, true));
        } catch (Exception e) {
            throw new RuntimeException("Failed to create recovery event", e);
        }
        return this;
    }
    
    /**
     * Add a custom event
     */
    public SimulationBuilder addEvent(String eventClassName, int processId, long time) {
        try {
            Class<?> eventClass = Class.forName(eventClassName);
            VSAbstractEvent event = (VSAbstractEvent) eventClass.getDeclaredConstructor().newInstance();
            scheduledTasks.add(new ScheduledTask(time, processId, event, true));
        } catch (Exception e) {
            throw new RuntimeException("Failed to create event: " + eventClassName, e);
        }
        return this;
    }
    
    /**
     * Set protocol classname using reflection (since field is private)
     */
    private void setProtocolClassname(VSProtocolEvent event, String classname) {
        try {
            Field field = VSProtocolEvent.class.getDeclaredField("protocolClassname");
            field.setAccessible(true);
            field.set(event, classname);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set protocol classname", e);
        }
    }
    
    /**
     * Set isServer flag using reflection (field is called isClientProtocol)
     */
    private void setIsServer(VSProtocolEvent event, boolean isServer) {
        try {
            // The field is actually called isClientProtocol, and server = !client
            Field field = VSProtocolEvent.class.getDeclaredField("isClientProtocol");
            field.setAccessible(true);
            field.set(event, !isServer); // Invert: server means NOT client
            
            // Also set protocol activation to true
            Field activationField = VSProtocolEvent.class.getDeclaredField("isProtocolActivation");
            activationField.setAccessible(true);
            activationField.set(event, true);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set protocol flags", e);
        }
    }
    
    /**
     * Build the simulation (must be called before save)
     */
    private void build() throws Exception {
        if (simulator != null) {
            return; // Already built
        }
        
        // Set simulation duration
        prefs.setInteger("sim.seconds", simulationDuration / 1000);
        
        // Set network delay parameters for message delivery
        prefs.setInteger("process.msg.delay.min", 10);  // 10ms minimum delay
        prefs.setInteger("process.msg.delay.max", 50);  // 50ms maximum delay
        
        // Create simulator without frame for headless
        simulator = new VSSimulator(prefs, null);
        
        // Create visualization without GUI
        VSLogging logging = new VSLogging();
        visualization = new VSSimulatorVisualization(prefs, simulator, logging);
        
        // Set visualization in simulator using reflection
        Field vizField = VSSimulator.class.getDeclaredField("simulatorVisualization");
        vizField.setAccessible(true);
        vizField.set(simulator, visualization);
        
        // Add processes if needed (default is 3)
        Method addProcessMethod = VSSimulatorVisualization.class.getDeclaredMethod("addProcess");
        addProcessMethod.setAccessible(true);
        
        // Remove default processes if we want fewer
        if (numProcesses < 3) {
            Field processesField = VSSimulatorVisualization.class.getDeclaredField("processes");
            processesField.setAccessible(true);
            ArrayList<VSInternalProcess> processes = (ArrayList<VSInternalProcess>) processesField.get(visualization);
            while (processes.size() > numProcesses) {
                processes.remove(processes.size() - 1);
            }
        }
        
        // Add more processes if needed
        for (int i = 3; i < numProcesses; i++) {
            addProcessMethod.invoke(visualization);
        }
        
        // Get task manager
        taskManager = visualization.getTaskManager();
        
        // Initialize all events with their processes
        for (ScheduledTask st : scheduledTasks) {
            VSInternalProcess process = visualization.getProcess(st.processId);
            st.event.init(process);
            
            // For protocol events, update the shortname after init
            if (st.event instanceof VSProtocolEvent) {
                VSProtocolEvent protocolEvent = (VSProtocolEvent) st.event;
                // Force shortname update by calling the method via reflection
                try {
                    Method createShortname = VSProtocolEvent.class.getDeclaredMethod("createShortname", String.class);
                    createShortname.setAccessible(true);
                    String shortname = (String) createShortname.invoke(protocolEvent, (String)null);
                    protocolEvent.setShortname(shortname);
                } catch (Exception e) {
                    // Ignore
                }
            }
            
            // Create task
            VSTask task = new VSTask(st.time, process, st.event, 
                st.isGlobalTimed ? VSTask.GLOBAL : VSTask.LOCAL);
            taskManager.addTask(task);
        }
    }
    
    /**
     * Save the simulation to a file
     */
    public SimulationBuilder save(String filename) throws Exception {
        build();
        
        File outputFile = new File(filename);
        outputFile.getParentFile().mkdirs();
        
        VSSerialize serialize = new VSSerialize();
        
        // Save using the serializer
        try {
            FileOutputStream fos = new FileOutputStream(outputFile);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            
            // Create serializable prefs from our prefs
            VSSerializablePrefs serializablePrefs = new VSSerializablePrefs();
            
            // Copy all preferences
            for (String key : prefs.getIntegerKeySet()) {
                serializablePrefs.initInteger(key, prefs.getInteger(key));
            }
            for (String key : prefs.getBooleanKeySet()) {
                serializablePrefs.initBoolean(key, prefs.getBoolean(key));
            }
            for (String key : prefs.getStringKeySet()) {
                serializablePrefs.initString(key, prefs.getString(key));
            }
            for (String key : prefs.getFloatKeySet()) {
                serializablePrefs.initFloat(key, prefs.getFloat(key));
            }
            for (String key : prefs.getColorKeySet()) {
                serializablePrefs.initColor(key, prefs.getColor(key));
            }
            for (String key : prefs.getVectorKeySet()) {
                serializablePrefs.initVector(key, prefs.getVector(key));
            }
            for (String key : prefs.getLongKeySet()) {
                serializablePrefs.initLong(key, prefs.getLong(key));
            }
            
            // Serialize preferences first
            serializablePrefs.serialize(serialize, oos);
            
            // Then serialize simulator
            simulator.serialize(serialize, oos);
            
            oos.close();
            fos.close();
            
            System.out.println("Simulation saved to: " + outputFile.getAbsolutePath());
        } catch (Exception e) {
            throw new RuntimeException("Failed to save simulation", e);
        }
        
        return this;
    }
    
    /**
     * Get the built simulator (for testing/verification)
     */
    public VSSimulator getSimulator() throws Exception {
        build();
        return simulator;
    }
    
    /**
     * Fluent API for common protocol setups
     */
    public static class Protocols {
        public static final String PING_PONG = "protocols.implementations.VSPingPongProtocol";
        public static final String BERKLEY_TIME = "protocols.implementations.VSBerkelyTimeProtocol";
        public static final String BROADCAST = "protocols.implementations.VSBroadcastProtocol";
        public static final String ONE_PHASE_COMMIT = "protocols.implementations.VSOnePhaseCommitProtocol";
        public static final String TWO_PHASE_COMMIT = "protocols.implementations.VSTwoPhaseCommitProtocol";
        public static final String RELIABLE_MULTICAST = "protocols.implementations.VSReliableMulticastProtocol";
    }
}