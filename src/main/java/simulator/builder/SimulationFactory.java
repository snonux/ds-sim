package simulator.builder;

import java.util.stream.IntStream;

/**
 * Factory for creating common simulation patterns using SimulationBuilder.
 * Provides convenience methods for standard distributed systems scenarios.
 */
public class SimulationFactory {
    
    
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
            .activateClientsAt(300, IntStream.range(1, numParticipants + 1).toArray());
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

    /**
     * Create a Raft simulation with a leader crash and staggered follower
     * activation so the election deadlines do not stay perfectly aligned.
     *
     * @return configured Raft simulation builder
     */
    public static SimulationBuilder createRaftSimulation() throws Exception {
        return new SimulationBuilder()
            .withProcesses(3)
            .withProtocol(SimulationBuilder.Protocols.RAFT)
            .withDuration(60000)
            .activateServers(0)
            .activateClientsAt(100, 1)
            .activateClientsAt(1700, 2)
            // Bias process 1 toward a fast, clean post-crash election while
            // keeping process 2's timeout comfortably behind it. The shorter
            // timeout also keeps the headless replay active long enough to
            // reach the first post-crash election deterministically.
            .setProtocolLong(1, "electionTimeout", 2500)
            .setProtocolLong(1, "electionJitter", 0)
            .setProtocolLong(2, "electionTimeout", 12000)
            .setProtocolLong(2, "electionJitter", 0)
            .addCrashEvent(0, 3500);
    }
}
