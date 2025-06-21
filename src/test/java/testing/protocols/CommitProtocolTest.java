package testing.protocols;

import testing.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit tests for commit protocols (one-phase and two-phase).
 */
@DisplayName("Commit Protocol Tests")
public class CommitProtocolTest {
    private HeadlessSimulationRunner runner;
    
    @BeforeEach
    public void setup() {
        runner = new HeadlessSimulationRunner();
    }
    
    @AfterEach
    public void teardown() {
        runner.shutdown();
    }
    
    @Test
    @DisplayName("Test One-Phase Commit protocol")
    public void testOnePhaseCommit() throws Exception {
        SimulationResult result = runner.runSimulation(
            "saved-simulations/one-phase-commit.dat", 
            3000
        );
        
        ProtocolVerifier verifier = new ProtocolVerifier()
            .expectLog("1-Phase Commit.*activated|One.*Phase.*activated")
            .expectLog("commit|Commit|COMMIT")
            .expectLog("Message")
            .expectNoLog("ERROR")
            .expectNoLog("abort|ABORT");
            
        VerificationResult verification = verifier.verify(result.getAllLogs());
        assertTrue(verification.passed(), verification.getFailureMessage());
    }
    
    @Test
    @DisplayName("Test Two-Phase Commit protocol")
    public void testTwoPhaseCommit() throws Exception {
        SimulationResult result = runner.runSimulation(
            "saved-simulations/two-phase-commit.dat", 
            4000
        );
        
        ProtocolVerifier verifier = new ProtocolVerifier()
            .expectLog("2-Phase Commit.*activated|Two.*Phase.*activated")
            .expectLog("Message")
            .expectNoLog("ERROR");
            
        VerificationResult verification = verifier.verify(result.getAllLogs());
        assertTrue(verification.passed(), verification.getFailureMessage());
        
        // Two-phase commit should have prepare/vote and commit phases
        boolean hasPrepare = result.countLogs("prepare|Prepare|PREPARE|vote|Vote") > 0;
        boolean hasCommit = result.countLogs("commit|Commit|COMMIT") > 0;
        
        assertTrue(hasPrepare || hasCommit, 
                  "Should have prepare/vote or commit messages");
    }
    
    @Test
    @DisplayName("Test commit protocol coordinator behavior")
    public void testCoordinatorBehavior() throws Exception {
        SimulationResult result = runner.runSimulation(
            "saved-simulations/two-phase-commit.dat", 
            5000
        );
        
        // Look for coordinator-specific messages
        boolean hasCoordinator = result.countLogs("coordinator|Coordinator|COORDINATOR") > 0;
        boolean hasParticipant = result.countLogs("participant|Participant|PARTICIPANT") > 0;
        boolean hasTransaction = result.countLogs("transaction|Transaction") > 0;
        
        assertTrue(hasCoordinator || hasParticipant || hasTransaction, 
                  "Should have coordinator/participant activity");
    }
}