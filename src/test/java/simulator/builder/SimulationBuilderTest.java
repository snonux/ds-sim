package simulator.builder;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.io.File;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;

import core.VSInternalProcess;
import simulator.VSSimulator;
import simulator.VSSimulatorVisualization;
import testing.HeadlessLoader;

/**
 * Tests for the SimulationBuilder framework
 */
class SimulationBuilderTest {
    
    private static final String TEST_DIR = "target/test-simulations/";
    
    @BeforeEach
    void setUp() throws Exception {
        // Create test directory
        Files.createDirectories(Paths.get(TEST_DIR));
    }
    
    @AfterEach
    void tearDown() throws Exception {
        // Clean up test files
        File dir = new File(TEST_DIR);
        if (dir.exists()) {
            for (File file : dir.listFiles()) {
                file.delete();
            }
        }
    }
    
    
    @Test
    void testCreatePingPongSimulation() throws Exception {
        String filename = TEST_DIR + "test-pingpong.dat";
        
        SimulationFactory.createPingPongSimulation(2)
            .save(filename);
        
        File file = new File(filename);
        assertTrue(file.exists(), "Simulation file should be created");
        
        String content = new String(Files.readAllBytes(file.toPath()),
                                    StandardCharsets.ISO_8859_1);
        assertTrue(content.contains("VSPingPongProtocol"), "Should contain PingPong protocol");
    }
    
    @Test
    void testCreateComplexSimulation() throws Exception {
        String filename = TEST_DIR + "test-complex.dat";
        
        // Create a complex simulation with events
        new SimulationBuilder()
            .withProcesses(5)
            .withProtocol(SimulationBuilder.Protocols.TWO_PHASE_COMMIT)
            .withDuration(30000)
            .activateServers(0, 1, 2)
            .activateClientsAt(1000, 3, 4)
            .addCrashEvent(0, 5000)
            .addRecoveryEvent(0, 10000)
            .save(filename);
        
        File file = new File(filename);
        assertTrue(file.exists(), "Simulation file should be created");
        assertTrue(file.length() > 5000, "Complex simulation should be larger");
        
        String content = new String(Files.readAllBytes(file.toPath()),
                                    StandardCharsets.ISO_8859_1);
        assertTrue(content.contains("VSProcessCrashEvent"), "Should contain crash event");
        assertTrue(content.contains("VSProcessRecoverEvent"), "Should contain recovery event");
    }

    @Test
    void testCreateRaftSimulation() throws Exception {
        String filename = TEST_DIR + "test-raft.dat";

        SimulationBuilder builder = SimulationFactory.createRaftSimulation();
        VSSimulator simulator = builder.save(filename).getSimulator();

        File file = new File(filename);
        assertTrue(file.exists(), "Simulation file should be created");
        assertTrue(file.length() > 10000, "Raft simulation should be larger");
        assertEquals(60, builder.getSimulator().getPrefs().getInteger("sim.seconds"),
                     "Raft simulation should replay for 60 seconds");

        String content = new String(Files.readAllBytes(file.toPath()),
                                    StandardCharsets.ISO_8859_1);
        assertTrue(content.contains("VSRaftProtocol"), "Should contain Raft protocol");
        assertTrue(content.contains("VSProcessCrashEvent"), "Should contain crash event");
        assertTrue(content.contains("VSProcessRecoverEvent"), "Should contain recovery event");
        assertTrue(countOccurrences(content, "VSProcessCrashEvent") >= 2,
                   "Should contain two crash events for different processes");

        HeadlessLoader.LoadedSimulation loaded =
            HeadlessLoader.load(filename, simulator.getPrefs());
        VSSimulator loadedSimulator = loaded.getSimulator();
        VSSimulatorVisualization visualization = loaded.getVisualization();
        try {
            VSInternalProcess process0 = visualization.getProcess(0);
            VSInternalProcess process2 = visualization.getProcess(2);

            runUntil(visualization, 3499);
            assertFalse(process0.isCrashed(), "leader should stay alive before 3500ms");
            assertFalse(process2.isCrashed(), "process 2 should stay alive before 3500ms");

            runUntil(visualization, 3501);
            assertTrue(process0.isCrashed(), "leader should crash immediately after 3500ms");
            assertFalse(process2.isCrashed(), "process 2 should still be alive after leader crash");

            runUntil(visualization, 12000);
            assertTrue(process0.isCrashed(), "leader should remain crashed before 12000ms recovery executes");
            assertFalse(process2.isCrashed(), "process 2 should stay alive before 20000ms");

            runUntil(visualization, 12001);
            assertFalse(process0.isCrashed(), "leader should recover immediately after 12000ms");
            assertFalse(process2.isCrashed(), "process 2 should still be alive after leader recovery");

            runUntil(visualization, 20000);
            assertFalse(process0.isCrashed(), "leader should remain recovered before 20000ms crash executes");
            assertFalse(process2.isCrashed(), "process 2 should stay alive before its crash point");

            runUntil(visualization, 20001);
            assertFalse(process0.isCrashed(), "leader should remain recovered at 20000ms");
            assertTrue(process2.isCrashed(), "process 2 should crash immediately after 20000ms");
        } finally {
            loadedSimulator.getSimulatorCanvas().stopThread();
            simulator.getSimulatorCanvas().stopThread();
        }
    }

    @Test
    void testAllProtocolTypes() throws Exception {
        // Test that all protocol constants work
        String[] protocols = {
            SimulationBuilder.Protocols.PING_PONG,
            SimulationBuilder.Protocols.BERKLEY_TIME,
            SimulationBuilder.Protocols.BROADCAST,
            SimulationBuilder.Protocols.ONE_PHASE_COMMIT,
            SimulationBuilder.Protocols.TWO_PHASE_COMMIT,
            SimulationBuilder.Protocols.RELIABLE_MULTICAST
        };
        
        for (String protocol : protocols) {
            String filename = TEST_DIR + "test-" + protocol.substring(protocol.lastIndexOf('.') + 1) + ".dat";
            
            new SimulationBuilder()
                .withProcesses(3)
                .withProtocol(protocol)
                .activateServers(0, 1, 2)
                .save(filename);
            
            File file = new File(filename);
            assertTrue(file.exists(), "Should create file for " + protocol);
            assertTrue(file.length() > 1000, "File should have content for " + protocol);
        }
    }
    
    @Test
    void testInvalidConfiguration() {
        // Test that invalid configurations throw exceptions
        assertThrows(IllegalArgumentException.class, () -> {
            SimulationFactory.createBerkeleyTimeSimulation(1); // Too few processes
        });
    }

    private int countOccurrences(String content, String needle) {
        int count = 0;
        int index = 0;

        while ((index = content.indexOf(needle, index)) != -1) {
            count++;
            index += needle.length();
        }

        return count;
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
            long nextWallTime = wallTime + 1L;
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
}
