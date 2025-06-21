package testing;

import simulator.*;
import simulator.engine.*;
import core.*;
import prefs.*;
import events.*;
import serialize.*;
import java.lang.reflect.*;
import java.util.concurrent.*;
import java.util.ArrayList;

/**
 * A headless runner that uses the new decoupled simulation engine.
 * This demonstrates how the new architecture eliminates GUI errors.
 */
public class EngineBasedHeadlessRunner {
    private final VSDefaultPrefs prefs;
    private SimulationEngine engine;
    private VSSimulator simulator;
    private LogCapture logCapture;
    private final ExecutorService executor;
    private boolean printLogs = false;
    
    public EngineBasedHeadlessRunner() {
        this.prefs = new VSDefaultPrefs();
        this.prefs.fillWithDefaults();
        VSRegisteredEvents.init(prefs);
        this.executor = Executors.newSingleThreadExecutor();
    }
    
    public SimulationResult runSimulation(String simulationFile, long maxTime) throws Exception {
        return runSimulation(simulationFile, maxTime, null);
    }
    
    public SimulationResult runSimulation(String simulationFile, long maxTime, LogListener listener) 
            throws Exception {
        System.out.println("Loading simulation: " + simulationFile);
        
        try {
            // Create log capture first
            logCapture = new LogCapture();
            logCapture.setPrintLogs(printLogs);
            if (listener != null) {
                logCapture.addListener(listener);
            }
            
            // Create headless engine
            engine = new HeadlessSimulationEngine(prefs, logCapture);
            
            // Load simulation data
            loadSimulation(simulationFile);
            
            System.out.println("Running simulation for " + maxTime + "ms...");
            
            // Run simulation in executor
            Future<Void> runFuture = executor.submit(() -> {
                try {
                    runSimulation(maxTime);
                } catch (Exception e) {
                    System.err.println("Error during simulation: " + e.getMessage());
                    e.printStackTrace();
                }
                return null;
            });
            
            // Wait for completion
            try {
                runFuture.get(maxTime * 2, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                System.out.println("Simulation timeout - stopping...");
                runFuture.cancel(true);
            }
            
            System.out.println("Simulation complete. Captured " + 
                             logCapture.getTotalLogCount() + " log entries.");
            
            return new SimulationResult(
                logCapture.getCapturedLogs(),
                logCapture.getProcessLogs(),
                getSimulationMetrics()
            );
            
        } catch (Exception e) {
            System.err.println("Failed to run simulation: " + e.getMessage());
            throw e;
        }
    }
    
    private void loadSimulation(String simulationFile) throws Exception {
        // We need a custom loader that works with the engine
        // For now, we'll use the existing loader and extract data
        
        DummySimulatorFrame dummyFrame = null;
        try {
            // Create minimal frame for loading
            dummyFrame = new DummySimulatorFrame(prefs);
            
            // Load simulation
            VSSerialize serialize = new VSSerialize();
            simulator = serialize.openSimulator(simulationFile, dummyFrame);
            
            if (simulator == null) {
                throw new IllegalStateException("Failed to load simulation");
            }
            
            // Extract visualization
            Field vizField = VSSimulator.class.getDeclaredField("simulatorVisualization");
            vizField.setAccessible(true);
            VSSimulatorVisualization viz = (VSSimulatorVisualization) vizField.get(simulator);
            
            // Extract processes and add to engine
            for (int i = 0; i < viz.getNumProcesses(); i++) {
                VSInternalProcess process = viz.getProcess(i);
                if (process != null) {
                    engine.addProcess(process);
                    // Update process to use engine for message sending
                    injectEngineIntoProcess(process);
                }
            }
            
            // Extract tasks from task manager
            VSTaskManager vizTaskManager = viz.getTaskManager();
            copyTasksToEngine(vizTaskManager, engine.getTaskManager());
            
        } finally {
            if (dummyFrame != null) {
                dummyFrame.dispose();
            }
        }
    }
    
    private void injectEngineIntoProcess(VSInternalProcess process) throws Exception {
        // This is where we'd modify the process to use the engine for sending messages
        // For now, we'll set up the logging
        Field logingField = VSAbstractProcess.class.getDeclaredField("loging");
        logingField.setAccessible(true);
        logingField.set(process, logCapture);
    }
    
    private void copyTasksToEngine(VSTaskManager source, VSTaskManager dest) throws Exception {
        // Copy tasks from source to destination
        // This requires accessing internal task manager state
        Field globalTasksField = VSTaskManager.class.getDeclaredField("globalTasks");
        globalTasksField.setAccessible(true);
        
        Field localTasksField = VSTaskManager.class.getDeclaredField("localTasks");
        localTasksField.setAccessible(true);
        
        // Copy global tasks
        Object globalTasks = globalTasksField.get(source);
        globalTasksField.set(dest, globalTasks);
        
        // Copy local tasks
        Object localTasks = localTasksField.get(source);
        localTasksField.set(dest, localTasks);
    }
    
    private void runSimulation(long maxTime) {
        if (engine instanceof HeadlessSimulationEngine) {
            HeadlessSimulationEngine headlessEngine = (HeadlessSimulationEngine) engine;
            
            // Reset and start
            engine.reset();
            engine.play();
            
            // Run for specified duration
            headlessEngine.runFor(maxTime);
        }
    }
    
    private SimulationMetrics getSimulationMetrics() {
        return new SimulationMetrics(
            engine.getNumProcesses(),
            logCapture.getTotalLogCount(),
            logCapture.getProcessMessageCounts()
        );
    }
    
    public void setPrintLogs(boolean printLogs) {
        this.printLogs = printLogs;
        if (logCapture != null) {
            logCapture.setPrintLogs(printLogs);
        }
    }
    
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }
}