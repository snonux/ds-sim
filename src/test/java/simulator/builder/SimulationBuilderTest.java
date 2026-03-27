package simulator.builder;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.io.File;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;

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

        SimulationFactory.createRaftSimulation()
            .save(filename);

        File file = new File(filename);
        assertTrue(file.exists(), "Simulation file should be created");
        assertTrue(file.length() > 10000, "Raft simulation should be larger");

        String content = new String(Files.readAllBytes(file.toPath()),
                                    StandardCharsets.ISO_8859_1);
        assertTrue(content.contains("VSRaftProtocol"), "Should contain Raft protocol");
        assertTrue(content.contains("VSProcessCrashEvent"), "Should contain crash event");
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
}
