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
        
        // Set reasonable message delays for testing (10-50ms instead of 500-2000ms)
        this.prefs.initLong("message.sendingtime.min", 10);
        this.prefs.initLong("message.sendingtime.max", 50);
        
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
            
            // Update message delays on all processes after loading
            for (int i = 0; i < viz.getNumProcesses(); i++) {
                VSInternalProcess process = viz.getProcess(i);
                if (process != null) {
                    process.initLong("message.sendingtime.min", 10);
                    process.initLong("message.sendingtime.max", 50);
                }
            }
            
            if (simulator == null || viz == null) {
                throw new IllegalStateException("Failed to load simulation");
            }
            
            // Install log capture first
            logCapture = new LogCapture();
            logCapture.setPrintLogs(printLogs);
            if (listener != null) {
                logCapture.addListener(listener);
            }
            installLogCapture();
            
            // Set up headless message handlers for all processes (after log capture is ready)
            setupHeadlessMessageHandlers(viz);
            
            // Get the simulation's configured end time
            long untilTime = viz.getUntilTime();
            long prefsUntilTime = simulator.getPrefs().getInteger("sim.seconds") * 1000L;
            long actualUntilTime = Math.max(untilTime, prefsUntilTime);
            long actualMaxTime = Math.min(maxTime, actualUntilTime);
            
            System.out.println("Running simulation for up to " + actualMaxTime + "ms (until time: " + untilTime + "ms)...");
            
            // Run simulation
            Future<Void> runFuture = executor.submit(() -> {
                try {
                    runSimulationSteps(actualMaxTime);
                } catch (Exception e) {
                    System.err.println("Error during simulation: " + e.getMessage());
                    e.printStackTrace();
                }
                return null;
            });
            
            // Wait for completion or timeout
            try {
                runFuture.get(actualMaxTime * 2, TimeUnit.MILLISECONDS);
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
        
        // Get simulatorTime field for accurate time tracking
        Field simulatorTimeField = VSSimulatorVisualization.class
            .getDeclaredField("simulatorTime");
        simulatorTimeField.setAccessible(true);
        
        // Get isPaused and hasFinished fields
        Field isPausedField = VSSimulatorVisualization.class
            .getDeclaredField("isPaused");
        isPausedField.setAccessible(true);
        Field hasFinishedField = VSSimulatorVisualization.class
            .getDeclaredField("hasFinished");
        hasFinishedField.setAccessible(true);
        
        // Get clockSpeed field and ensure it's set
        Field clockSpeedField = VSSimulatorVisualization.class
            .getDeclaredField("clockSpeed");
        clockSpeedField.setAccessible(true);
        double clockSpeed = clockSpeedField.getDouble(viz);
        if (clockSpeed == 0) {
            // Set default clock speed if not initialized
            clockSpeed = 1.0;
            clockSpeedField.setDouble(viz, clockSpeed);
        }
        
        // Get task queue fields for checking if tasks remain
        Field globalTasksField = VSTaskManager.class.getDeclaredField("globalTasks");
        globalTasksField.setAccessible(true);
        
        // Set isPaused to false to allow simulation to run
        isPausedField.setBoolean(viz, false);
        hasFinishedField.setBoolean(viz, false);
        
        long startTime = timeField.getLong(viz);
        long currentTime = startTime;
        long endTime = startTime + maxTime;
        int noActivityCount = 0;
        int lastLogCount = 0;
        long lastActiveTime = 0;
        
        // Call updateSimulator method to advance simulation
        Method updateSimulatorMethod = VSSimulatorVisualization.class
            .getDeclaredMethod("updateSimulator", long.class, long.class);
        updateSimulatorMethod.setAccessible(true);
        
        System.out.println("Starting simulation at time " + startTime + ", running until " + endTime);
        
        while (currentTime < endTime) {
            // Sync all process times BEFORE running tasks
            for (int i = 0; i < viz.getNumProcesses(); i++) {
                VSInternalProcess process = viz.getProcess(i);
                if (process != null) {
                    process.syncTime(currentTime);
                }
            }
            
            // Update simulation time - this also runs tasks internally
            updateSimulatorMethod.invoke(viz, currentTime, currentTime - 1);
            long simulatorTime = simulatorTimeField.getLong(viz);
            
            // Check if there's been any activity
            int currentLogCount = logCapture.getTotalLogCount();
            boolean hasActivity = currentLogCount > lastLogCount || 
                                 hasPendingActivity(taskManager, globalTasksField, simulatorTime);
            
            if (hasActivity) {
                noActivityCount = 0;
                lastLogCount = currentLogCount;
                lastActiveTime = currentTime;
            } else {
                noActivityCount++;
                // If no activity for 5000ms (5 seconds) of simulation time, stop
                // This accounts for message delivery times of 500-2000ms plus extra buffer
                // to ensure all messages are delivered
                if (noActivityCount > 5000 && (currentTime - lastActiveTime) > 5000) {
                    System.out.println("No activity detected for 5 seconds - simulation complete at time " + simulatorTime);
                    break;
                }
            }
            
            // Advance time by 1ms
            currentTime++;
            timeField.setLong(viz, currentTime);
            
            // No delay needed - let simulation run at full speed
        }
        
        // Set isPaused back to true when done
        isPausedField.setBoolean(viz, true);
    }
    
    private boolean hasPendingActivity(VSTaskManager taskManager, Field globalTasksField, long currentTime) {
        try {
            // Check global tasks - but also check if any are scheduled for future times
            Queue<?> globalTasks = (Queue<?>) globalTasksField.get(taskManager);
            if (globalTasks != null && !globalTasks.isEmpty()) {
                // Check if any tasks are scheduled for the future
                for (Object obj : globalTasks) {
                    VSTask task = (VSTask) obj;
                    if (task.getTaskTime() > currentTime) {
                        // There's a future task scheduled, keep running
                        return true;
                    }
                }
                // If all tasks are in the past or present, they should execute now
                return true;
            }
            
            // Check process-specific tasks
            for (int i = 0; i < viz.getNumProcesses(); i++) {
                VSInternalProcess process = viz.getProcess(i);
                if (process != null) {
                    Queue<VSTask> tasks = process.getTasks();
                    if (tasks != null && !tasks.isEmpty()) {
                        // Check if any tasks are scheduled for the future
                        for (VSTask task : tasks) {
                            if (task.getTaskTime() > process.getTime()) {
                                return true;
                            }
                        }
                        // If all tasks are ready to run, keep going
                        return true;
                    }
                }
            }
            
            // Check for messages in transit (visualization lines)
            Field messageLinesField = VSSimulatorVisualization.class.getDeclaredField("messageLines");
            messageLinesField.setAccessible(true);
            LinkedList<?> messageLines = (LinkedList<?>) messageLinesField.get(viz);
            if (messageLines != null && !messageLines.isEmpty()) {
                return true; // Messages are still being delivered
            }
            
            return false;
        } catch (Exception e) {
            // If we can't check, assume there might be activity
            return true;
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
        
        // Set the task manager from the visualization
        engine.setTaskManager(viz.getTaskManager());
        
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
    }
}
