package testing.protocols;

import org.junit.platform.suite.api.*;

/**
 * Test suite that runs all protocol tests.
 * This ensures all protocol simulations are tested when running unit tests.
 */
@Suite
@SuiteDisplayName("DS-Sim Protocol Test Suite")
@SelectClasses({
    // Basic protocols
    PingPongProtocolTest.class,
    PingPongSturmProtocolTest.class,
    BroadcastProtocolTest.class,
    BasicMulticastProtocolTest.class,
    ReliableMulticastProtocolTest.class,
    
    // Time synchronization protocols
    BerkeleyProtocolTest.class,
    TimeSynchronizationProtocolTest.class,
    
    // Commit protocols
    CommitProtocolTest.class,
    
    // Network simulation
    SlowConnectionProtocolTest.class
    
    // Note: Raft tests are excluded as requested
})
@IncludeClassNamePatterns(".*Test")
public class AllProtocolsTestSuite {
    // This class remains empty. It is used only as a holder for the above annotations
}