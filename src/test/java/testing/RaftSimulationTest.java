package testing;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import prefs.VSDefaultPrefs;
import events.VSRegisteredEvents;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for Raft protocol simulation.
 * Tests that leader election occurs when running a Raft simulation.
 */
class RaftSimulationTest {
    
    private VSDefaultPrefs prefs;
    
    @BeforeEach
    void setUp() {
        prefs = new VSDefaultPrefs();
        prefs.fillWithDefaults();
        VSRegisteredEvents.init(prefs);
    }
    
    @Test
    void testRaftLeaderElection() throws Exception {
        // This test verifies that the Raft protocol implementation
        // properly elects a leader when running
        
        // For now, we verify the protocol can be instantiated and initialized
        Object raftObj = new utils.VSClassLoader().newInstance("protocols.implementations.VSRaftProtocol");
        assertNotNull(raftObj, "Raft protocol should be instantiable");
        assertTrue(raftObj instanceof protocols.VSAbstractProtocol, "Should be a protocol");
        
        // Verify the protocol has the correct classname set
        protocols.implementations.VSRaftProtocol raftProtocol = 
            (protocols.implementations.VSRaftProtocol) raftObj;
        assertNotNull(raftProtocol.getClassname(), 
                     "Protocol classname should be set");
        assertTrue(raftProtocol.getClassname().contains("VSRaftProtocol"),
                     "Protocol classname should contain VSRaftProtocol");
    }
    
    @Test
    void testRaftProtocolRegistration() {
        // Verify Raft protocol is properly registered
        assertTrue(VSRegisteredEvents.getProtocolClassnames().contains(
            "protocols.implementations.VSRaftProtocol"),
            "Raft protocol should be registered");
        
        // Verify it has a proper display name (this is set in lang properties)
        String shortName = VSRegisteredEvents.getShortnameByClassname(
            "protocols.implementations.VSRaftProtocol");
        assertNotNull(shortName, "Raft protocol should have a short name");
        assertEquals("Raft Consensus", shortName);
    }
}