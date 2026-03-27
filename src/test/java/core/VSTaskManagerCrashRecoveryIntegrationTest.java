package core;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.PriorityQueue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import events.implementations.VSProcessCrashEvent;
import events.implementations.VSProcessRecoverEvent;
import prefs.VSSerializablePrefs;
import serialize.VSSerialize;
import simulator.VSSimulator;
import simulator.VSSimulatorVisualization;
import simulator.builder.SimulationBuilder;
import testing.HeadlessLoader;

class VSTaskManagerCrashRecoveryIntegrationTest {
    private static final long ADVANCE_STEP_MS = 1L;
    private static final String RAFT_REPLAY = "saved-simulations/raft.dat";

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
        assertFalse(visualization.getProcess(0).isCrashed(),
                    "process 0 should start alive after replay load");
        assertFalse(visualization.getProcess(1).isCrashed(),
                    "process 1 should start alive after replay load");

        runUntil(visualization, 4);
        assertFalse(visualization.getProcess(0).isCrashed(),
                    "process 0 should stay alive before its crash point");
        assertFalse(visualization.getProcess(1).isCrashed(),
                    "process 1 should stay alive before process 0 crashes");

        runUntil(visualization, 6);
        assertTrue(visualization.getProcess(0).isCrashed(),
                   "process 0 should crash immediately after its scheduled crash point");
        assertFalse(visualization.getProcess(1).isCrashed(),
                    "process 1 should still be alive while process 0 is crashed");

        runUntil(visualization, 10);
        assertTrue(visualization.getProcess(0).isCrashed(),
                   "process 0 should remain crashed before its recover point");
        assertFalse(visualization.getProcess(1).isCrashed(),
                    "process 1 should still be alive before its later crash");

        runUntil(visualization, 11);
        assertFalse(visualization.getProcess(0).isCrashed(),
                    "process 0 should recover immediately after its scheduled recover point");
        assertFalse(visualization.getProcess(1).isCrashed(),
                    "process 1 should still be alive before its crash point");

        runUntil(visualization, 15);
        assertFalse(visualization.getProcess(0).isCrashed(),
                    "process 0 should stay recovered before process 1 crashes");
        assertFalse(visualization.getProcess(1).isCrashed(),
                    "process 1 should stay alive before its later crash point");

        runUntil(visualization, 16);
        assertFalse(visualization.getProcess(0).isCrashed(),
                    "process 0 should remain recovered after replay load");
        assertTrue(visualization.getProcess(1).isCrashed(),
                   "process 1 should crash immediately after its later replay point");
    }

    @Test
    @DisplayName("Loaded raft replay keeps crash and recover events visible in Event view collections")
    void loadedRaftReplayKeepsCrashAndRecoverEventsVisible() throws Exception {
        HeadlessLoader.LoadedSimulation loaded =
            HeadlessLoader.load(RAFT_REPLAY, prefs.VSDefaultPrefs.init());
        loadedSimulatorToStop = loaded.getSimulator();

        VSSimulatorVisualization visualization = loaded.getVisualization();
        VSTaskManager taskManager = visualization.getTaskManager();
        VSInternalProcess process0 = visualization.getProcess(0);
        VSInternalProcess process2 = visualization.getProcess(2);

        assertEquals(6, taskManager.getGlobalTasks().size(),
                     "loaded raft replay should expose all saved global tasks");
        assertEquals(3, taskManager.getProcessGlobalTasks(process0).size(),
                     "process 0 should expose its activation, crash, and recover events");
        assertEquals(2, taskManager.getProcessGlobalTasks(process2).size(),
                     "process 2 should expose its activation and later crash event");

        runUntil(visualization, 12001);
        assertEquals(3, taskManager.getProcessGlobalTasks(process0).size(),
                     "process 0 recover event should remain visible after it executes");

        runUntil(visualization, 20001);
        assertEquals(2, taskManager.getProcessGlobalTasks(process2).size(),
                     "process 2 later crash event should remain visible after it executes");
    }

    @Test
    @DisplayName("Loading preserves programmed replay tasks but not ordinary runtime tasks")
    void loadingPreservesProgrammedReplayTasksWithoutPromotingRuntimeTasks()
    throws Exception {
        Path replayFile = Files.createTempFile("programmed-replay", ".dat");
        Path runtimeFile = Files.createTempFile("runtime-task", ".dat");

        VSSimulator replaySimulator = new SimulationBuilder()
            .withProcesses(3)
            .withDuration(1000)
            .addCrashEvent(0, 5)
            .save(replayFile.toString())
            .getSimulator();
        simulatorToStop = replaySimulator;

        HeadlessLoader.LoadedSimulation replayLoaded =
            HeadlessLoader.load(replayFile.toString(), replaySimulator.getPrefs());
        loadedSimulatorToStop = replayLoaded.getSimulator();

        VSTaskManager replayTaskManager = replayLoaded.getVisualization().getTaskManager();
        assertEquals(1, replayTaskManager.getProcessGlobalTasks(
                         replayLoaded.getVisualization().getProcess(0)).size(),
                     "builder-authored replay task should remain visible after load");

        loadedSimulatorToStop.getSimulatorCanvas().stopThread();
        loadedSimulatorToStop = null;

        VSSimulator runtimeSimulator = new SimulationBuilder()
            .withProcesses(3)
            .withDuration(1000)
            .getSimulator();
        simulatorToStop = runtimeSimulator;

        VSSimulatorVisualization runtimeVisualization = runtimeSimulator.getSimulatorCanvas();
        VSInternalProcess runtimeProcess = runtimeVisualization.getProcess(0);
        VSTask runtimeTask = new VSTask(5, runtimeProcess, new VSProcessCrashEvent(), VSTask.GLOBAL);
        runtimeVisualization.getTaskManager().addTask(runtimeTask);
        assertFalse(runtimeTask.isProgrammed(), "runtime task should remain non-programmed before save");

        saveSimulation(runtimeFile, runtimeSimulator);

        HeadlessLoader.LoadedSimulation runtimeLoaded =
            HeadlessLoader.load(runtimeFile.toString(), runtimeSimulator.getPrefs());
        loadedSimulatorToStop = runtimeLoaded.getSimulator();

        VSSimulatorVisualization loadedRuntimeVisualization = runtimeLoaded.getVisualization();
        VSTaskManager runtimeTaskManager = runtimeLoaded.getVisualization().getTaskManager();
        assertEquals(1, getPendingGlobalTaskCount(runtimeTaskManager),
                     "ordinary non-programmed runtime task should still be restored after load");
        assertEquals(0, runtimeTaskManager.getProcessGlobalTasks(
                         loadedRuntimeVisualization.getProcess(0)).size(),
                     "ordinary non-programmed runtime task must stay hidden after load");

        assertFalse(loadedRuntimeVisualization.getProcess(0).isCrashed(),
                    "restored runtime task should not execute before its scheduled time");
        runUntil(loadedRuntimeVisualization, 6);
        assertTrue(loadedRuntimeVisualization.getProcess(0).isCrashed(),
                   "restored runtime task should still execute after load");
        assertEquals(0, getPendingGlobalTaskCount(runtimeTaskManager),
                     "runtime task should leave the pending queue after execution");
        assertEquals(0, getFulfilledProgrammedTaskCount(runtimeTaskManager),
                     "executed runtime task must not become a programmed Event-view entry");
        assertEquals(0, runtimeTaskManager.getProcessGlobalTasks(
                         loadedRuntimeVisualization.getProcess(0)).size(),
                     "executed runtime task must remain hidden from the Event view");
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

    @SuppressWarnings("unchecked")
    private int getPendingGlobalTaskCount(VSTaskManager taskManager) throws Exception {
        Field field = VSTaskManager.class.getDeclaredField("globalTasks");
        field.setAccessible(true);
        return ((PriorityQueue<VSTask>) field.get(taskManager)).size();
    }

    @SuppressWarnings("unchecked")
    private int getFulfilledProgrammedTaskCount(VSTaskManager taskManager) throws Exception {
        Field field = VSTaskManager.class.getDeclaredField("fullfilledProgrammedTasks");
        field.setAccessible(true);
        return ((LinkedList<VSTask>) field.get(taskManager)).size();
    }

    private void saveSimulation(Path file, VSSimulator simulator) throws Exception {
        VSSerialize serialize = new VSSerialize();
        try (FileOutputStream fos = new FileOutputStream(file.toFile());
             ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            VSSerializablePrefs serializablePrefs = new VSSerializablePrefs();

            for (String key : simulator.getPrefs().getIntegerKeySet()) {
                serializablePrefs.initInteger(key, simulator.getPrefs().getInteger(key));
            }
            for (String key : simulator.getPrefs().getBooleanKeySet()) {
                serializablePrefs.initBoolean(key, simulator.getPrefs().getBoolean(key));
            }
            for (String key : simulator.getPrefs().getStringKeySet()) {
                serializablePrefs.initString(key, simulator.getPrefs().getString(key));
            }
            for (String key : simulator.getPrefs().getFloatKeySet()) {
                serializablePrefs.initFloat(key, simulator.getPrefs().getFloat(key));
            }
            for (String key : simulator.getPrefs().getColorKeySet()) {
                serializablePrefs.initColor(key, simulator.getPrefs().getColor(key));
            }
            for (String key : simulator.getPrefs().getVectorKeySet()) {
                serializablePrefs.initVector(key, simulator.getPrefs().getVector(key));
            }
            for (String key : simulator.getPrefs().getLongKeySet()) {
                serializablePrefs.initLong(key, simulator.getPrefs().getLong(key));
            }

            serializablePrefs.serialize(serialize, oos);
            simulator.serialize(serialize, oos);
        }
    }
}
