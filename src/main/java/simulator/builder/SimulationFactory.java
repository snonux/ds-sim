package simulator.builder;

import java.util.stream.IntStream;

/**
 * Factory for creating common simulation patterns using SimulationBuilder.
 * Provides convenience methods for standard distributed systems scenarios.
 */
public class SimulationFactory {
    
    /**
     * Create a standard Raft consensus simulation
     * @param numServers Number of Raft servers (minimum 3 for consensus)
     * @param numClients Number of client processes
     * @return Configured SimulationBuilder
     */
    public static SimulationBuilder createRaftSimulation(int numServers, int numClients) throws Exception {
        if (numServers < 3) {
            throw new IllegalArgumentException("Raft requires at least 3 servers for consensus");
        }
        
        return new SimulationBuilder()
            .withProcesses(numServers + numClients)
            .withProtocol(SimulationBuilder.Protocols.RAFT)
            .withDuration(15000) // 15 seconds to see leader election
            .activateServers(IntStream.range(0, numServers).toArray())
            .activateClients(500, IntStream.range(numServers, numServers + numClients).toArray());
    }
    
    /**
     * Create a Raft simulation with fault tolerance testing
     * @param numServers Number of Raft servers
     * @return Configured SimulationBuilder with crash/recovery events
     */
    public static SimulationBuilder createRaftFaultToleranceSimulation(int numServers) throws Exception {
        return createRaftSimulation(numServers, 0)
            .withDuration(30000) // 30 seconds for fault testing
            .addCrashEvent(0, 5000)    // Crash leader after 5s
            .addRecoveryEvent(0, 10000) // Recover after 10s
            .addCrashEvent(1, 15000)    // Crash another server
            .addRecoveryEvent(1, 20000); // Recover after 20s
    }
    
    /**
     * Create a simple ping-pong simulation
     * @param numProcesses Number of processes to ping-pong between
     * @return Configured SimulationBuilder
     */
    public static SimulationBuilder createPingPongSimulation(int numProcesses) throws Exception {
        return new SimulationBuilder()
            .withProcesses(numProcesses)
            .withProtocol(SimulationBuilder.Protocols.PING_PONG)
            .withDuration(5000)
            .activateServers(IntStream.range(0, numProcesses).toArray());
    }
    
    /**
     * Create a Berkeley time synchronization simulation
     * @param numProcesses Number of processes to synchronize
     * @return Configured SimulationBuilder
     */
    public static SimulationBuilder createBerkeleyTimeSimulation(int numProcesses) throws Exception {
        if (numProcesses < 2) {
            throw new IllegalArgumentException("Berkeley algorithm needs at least 2 processes");
        }
        
        return new SimulationBuilder()
            .withProcesses(numProcesses)
            .withProtocol(SimulationBuilder.Protocols.BERKLEY_TIME)
            .withDuration(10000)
            .activateServers(0) // First process is time server
            .activateClients(IntStream.range(1, numProcesses).toArray());
    }
    
    /**
     * Create a two-phase commit simulation
     * @param numParticipants Number of participant processes
     * @return Configured SimulationBuilder
     */
    public static SimulationBuilder createTwoPhaseCommitSimulation(int numParticipants) throws Exception {
        return new SimulationBuilder()
            .withProcesses(numParticipants + 1) // +1 for coordinator
            .withProtocol(SimulationBuilder.Protocols.TWO_PHASE_COMMIT)
            .withDuration(10000)
            .activateServers(0) // Process 0 is coordinator
            .activateClients(300, IntStream.range(1, numParticipants + 1).toArray());
    }
    
    /**
     * Create a reliable multicast simulation
     * @param numProcesses Number of processes in the multicast group
     * @return Configured SimulationBuilder
     */
    public static SimulationBuilder createReliableMulticastSimulation(int numProcesses) throws Exception {
        return new SimulationBuilder()
            .withProcesses(numProcesses)
            .withProtocol(SimulationBuilder.Protocols.RELIABLE_MULTICAST)
            .withDuration(10000)
            .activateServers(IntStream.range(0, numProcesses).toArray());
    }
    
    /**
     * Create a broadcast protocol simulation
     * @param numProcesses Number of processes
     * @return Configured SimulationBuilder
     */
    public static SimulationBuilder createBroadcastSimulation(int numProcesses) throws Exception {
        return new SimulationBuilder()
            .withProcesses(numProcesses)
            .withProtocol(SimulationBuilder.Protocols.BROADCAST)
            .withDuration(8000)
            .activateServers(0) // First process broadcasts
            .activateClients(IntStream.range(1, numProcesses).toArray());
    }
}