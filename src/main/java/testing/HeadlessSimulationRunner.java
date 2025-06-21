package testing;

import simulator.*;
import simulator.engine.*;
import simulator.messaging.*;
import core.*;
import prefs.*;
import events.*;
import serialize.VSSerialize;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Runs DS-Sim simulations in headless mode without GUI dependencies.
 * Captures logs and provides verification capabilities for automated testing.
 */
public class HeadlessSimulationRunner {
    private final VSDefaultPrefs prefs;
    private VSSimulator simulator;
    private VSSimulatorVisualization viz;
    private LogCapture logCapture;
    private final ExecutorService executor;
    private boolean printLogs = false;
    
    public HeadlessSimulationRunner() {
        this.prefs = new VSDefaultPrefs();
        this.prefs.fillWithDefaults();
        VSRegisteredEvents.init(prefs);
        this.executor = Executors.newSingleThreadExecutor();
    }
    
    /**
     * Run a simulation from a saved file for a specified duration.
     * 
     * @param simulationFile Path to the saved simulation .dat file
     * @param maxTime Maximum simulation time in milliseconds
     * @return SimulationResult containing logs and metrics
     */
    public SimulationResult runSimulation(String simulationFile, long maxTime) 
            throws Exception {
        return runSimulation(simulationFile, maxTime, null);
    }
    
    /**
     * Run a simulation with an optional log listener.
     */
    public SimulationResult runSimulation(String simulationFile, long maxTime, LogListener listener) 
            throws Exception {
        System.out.println("Loading simulation: " + simulationFile);
        
        try {
            // Use HeadlessLoader to avoid any GUI initialization
            HeadlessLoader.LoadedSimulation loaded = HeadlessLoader.load(simulationFile, prefs);
            simulator = loaded.getSimulator();
            viz = loaded.getVisualization();
            
            if (simulator == null || viz == null) {
                throw new IllegalStateException("Failed to load simulation");
            }
            
            // Set up headless message handlers for all processes
            setupHeadlessMessageHandlers(viz);
            
            // Install log capture
            logCapture = new LogCapture();
            logCapture.setPrintLogs(printLogs);
            if (listener != null) {
                logCapture.addListener(listener);
            }
            installLogCapture();
            
            System.out.println("Running simulation for " + maxTime + "ms...");
            
            // Run simulation
            Future<Void> runFuture = executor.submit(() -> {
                try {
                    runSimulationSteps(maxTime);
                } catch (Exception e) {
                    System.err.println("Error during simulation: " + e.getMessage());
                    e.printStackTrace();
                }
                return null;
            });
            
            // Wait for completion or timeout
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
            System.err.println("Failed to load simulation: " + e.getMessage());
            throw e;
        }
    }
    
    private void runSimulationSteps(long maxTime) throws Exception {
        VSTaskManager taskManager = viz.getTaskManager();
        
        // Get necessary fields via reflection
        Field timeField = VSSimulatorVisualization.class
            .getDeclaredField("time");
        timeField.setAccessible(true);
        
        // Find runTasks method with correct signature
        Method runTasksMethod = VSTaskManager.class
            .getDeclaredMethod("runTasks", long.class, long.class, long.class);
        runTasksMethod.setAccessible(true);
        
        long startTime = timeField.getLong(viz);
        long currentTime = startTime;
        
        while (currentTime - startTime < maxTime) {
            // Update time
            timeField.setLong(viz, currentTime);
            
            // Sync process times
            for (int i = 0; i < viz.getNumProcesses(); i++) {
                viz.getProcess(i).syncTime(currentTime);
            }
            
            // Run tasks (step, offset, lastGlobalTime)
            runTasksMethod.invoke(taskManager, currentTime, 0L, currentTime - 1);
            
            // Advance time by 1ms
            currentTime++;
            
            // Small delay to prevent CPU spinning
            Thread.sleep(1);
        }
    }
    
    private void installLogCapture() throws Exception {
        // Set simulatorVisualization reference in logCapture
        logCapture.setSimulatorCanvas(viz);
        
        // Install on visualization
        Field logingField = VSSimulatorVisualization.class
            .getDeclaredField("loging");
        logingField.setAccessible(true);
        logingField.set(viz, logCapture);
        
        // Install on all processes
        for (int i = 0; i < viz.getNumProcesses(); i++) {
            VSInternalProcess process = viz.getProcess(i);
            if (process != null) {
                Field processLogingField = VSAbstractProcess.class
                    .getDeclaredField("loging");
                processLogingField.setAccessible(true);
                processLogingField.set(process, logCapture);
            }
        }
    }
    
    private SimulationMetrics getSimulationMetrics() {
        return new SimulationMetrics(
            viz.getNumProcesses(),
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
    
    public void addLogListener(LogListener listener) {
        if (logCapture != null) {
            logCapture.addListener(listener);
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
    
    /**
     * Sets up headless message handlers for all processes to avoid GUI dependencies.
     */
    private void setupHeadlessMessageHandlers(VSSimulatorVisualization viz) {
        // Create a headless simulation engine
        HeadlessSimulationEngine engine = new HeadlessSimulationEngine(prefs, logCapture);
        
        // Copy processes to engine
        for (int i = 0; i < viz.getNumProcesses(); i++) {
            VSInternalProcess process = viz.getProcess(i);
            if (process != null) {
                engine.addProcess(process);
                
                // Create and set headless message handler
                MessageHandler handler = new HeadlessMessageHandler(engine);
                process.setMessageHandler(handler);
            }
        }
        
        // Copy task manager state
        try {
            VSTaskManager vizTaskManager = viz.getTaskManager();
            VSTaskManager engineTaskManager = engine.getTaskManager();
            
            // Use reflection to copy task queues
            Field globalTasksField = VSTaskManager.class.getDeclaredField("globalTasks");
            globalTasksField.setAccessible(true);
            Field localTasksField = VSTaskManager.class.getDeclaredField("localTasks");
            localTasksField.setAccessible(true);
            
            Object globalTasks = globalTasksField.get(vizTaskManager);
            Object localTasks = localTasksField.get(vizTaskManager);
            
            globalTasksField.set(engineTaskManager, globalTasks);
            localTasksField.set(engineTaskManager, localTasks);
        } catch (Exception e) {
            // Log but don't fail - task manager state might not be critical
            System.err.println("Warning: Could not copy task manager state: " + e.getMessage());
        }
    }
}