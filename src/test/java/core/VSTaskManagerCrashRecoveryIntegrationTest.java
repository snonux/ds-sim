package core;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import events.implementations.VSProcessCrashEvent;
import events.implementations.VSProcessRecoverEvent;
import simulator.VSSimulator;
import simulator.VSSimulatorVisualization;
import simulator.builder.SimulationBuilder;
import testing.HeadlessLoader;

class VSTaskManagerCrashRecoveryIntegrationTest {
    private static final long ADVANCE_STEP_MS = 1L;

    private VSSimulator simulatorToStop;
    private VSSimulator loadedSimulatorToStop;

    @AfterEach
    void tearDown() {
        stopSimulatorThread(simulatorToStop);
        stopSimulatorThread(loadedSimulatorToStop);
    }

    @Test
    @DisplayName("Runtime supports recover then later crash of a different process")
    void runtimeSupportsRecoverThenCrashAnotherProcess() throws Exception {
        VSSimulator simulator = new SimulationBuilder()
            .withProcesses(3)
            .withDuration(1000)
            .getSimulator();
        simulatorToStop = simulator;

        VSSimulatorVisualization visualization = simulator.getSimulatorCanvas();
        VSInternalProcess process0 = visualization.getProcess(0);
        VSInternalProcess process1 = visualization.getProcess(1);

        VSTaskManager taskManager = visualization.getTaskManager();
        taskManager.addTask(new VSTask(5, process0, new VSProcessCrashEvent(), VSTask.GLOBAL));
        taskManager.addTask(new VSTask(10, process0, new VSProcessRecoverEvent(), VSTask.GLOBAL));
        taskManager.addTask(new VSTask(15, process1, new VSProcessCrashEvent(), VSTask.GLOBAL));

        runUntil(visualization, 20);

        assertFalse(process0.isCrashed(), "process 0 should have recovered");
        assertTrue(process1.isCrashed(), "process 1 should crash after process 0 recovers");
    }

    @Test
    @DisplayName("Saved replay preserves recover then later crash of another process")
    void savedReplayPreservesRecoverThenCrashAnotherProcess() throws Exception {
        Path datFile = Files.createTempFile("crash-recover-replay", ".dat");

        VSSimulator builtSimulator = new SimulationBuilder()
            .withProcesses(3)
            .withDuration(1000)
            .addCrashEvent(0, 5)
            .addRecoveryEvent(0, 10)
            .addCrashEvent(1, 15)
            .save(datFile.toString())
            .getSimulator();
        simulatorToStop = builtSimulator;

        HeadlessLoader.LoadedSimulation loaded =
            HeadlessLoader.load(datFile.toString(), builtSimulator.getPrefs());
        loadedSimulatorToStop = loaded.getSimulator();

        VSSimulatorVisualization visualization = loaded.getVisualization();
        runUntil(visualization, 20);

        assertFalse(visualization.getProcess(0).isCrashed(),
                    "process 0 should recover after replay load");
        assertTrue(visualization.getProcess(1).isCrashed(),
                   "process 1 should still crash later in the replay");
    }

    @Test
    @DisplayName("Live GUI-style event injection supports recover and later crash")
    void liveEventInjectionSupportsRecoverAndLaterCrash() throws Exception {
        VSSimulator simulator = new SimulationBuilder()
            .withProcesses(3)
            .withDuration(1000)
            .getSimulator();
        simulatorToStop = simulator;

        VSSimulatorVisualization visualization = simulator.getSimulatorCanvas();
        VSInternalProcess process0 = visualization.getProcess(0);
        VSInternalProcess process1 = visualization.getProcess(1);
        VSTaskManager taskManager = visualization.getTaskManager();

        runUntil(visualization, 5);
        taskManager.addTask(new VSTask(process0.getGlobalTime(), process0,
                                       new VSProcessCrashEvent(), VSTask.GLOBAL));
        runUntil(visualization, 6);
        assertTrue(process0.isCrashed(), "process 0 should crash from live event");

        taskManager.addTask(new VSTask(process0.getGlobalTime(), process0,
                                       new VSProcessRecoverEvent(), VSTask.GLOBAL));
        runUntil(visualization, 7);
        assertFalse(process0.isCrashed(), "process 0 should recover from live event");

        taskManager.addTask(new VSTask(process1.getGlobalTime(), process1,
                                       new VSProcessCrashEvent(), VSTask.GLOBAL));
        runUntil(visualization, 8);
        assertTrue(process1.isCrashed(), "process 1 should crash after process 0 recovers");
    }

    private void runUntil(VSSimulatorVisualization visualization, long targetTime)
    throws Exception {
        setBooleanField(visualization, "isPaused", false);
        setBooleanField(visualization, "hasFinished", false);
        setDoubleField(visualization, "clockSpeed", 1.0d);
        Method updateSimulator = VSSimulatorVisualization.class.getDeclaredMethod(
            "updateSimulator", long.class, long.class);
        updateSimulator.setAccessible(true);

        long wallTime = visualization.getTime();
        while (visualization.getTime() < targetTime) {
            long nextWallTime = wallTime + ADVANCE_STEP_MS;
            updateSimulator.invoke(visualization, nextWallTime, wallTime);
            wallTime = nextWallTime;
        }
    }

    private void setBooleanField(Object target, String fieldName, boolean value)
    throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.setBoolean(target, value);
    }

    private void setDoubleField(Object target, String fieldName, double value)
    throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.setDouble(target, value);
    }

    private void stopSimulatorThread(VSSimulator simulator) {
        if (simulator != null) {
            simulator.getSimulatorCanvas().stopThread();
        }
    }
}
