package protocols.implementations;

import java.util.ArrayList;
import java.util.Vector;

import core.VSInternalProcess;
import core.VSMessage;
import protocols.VSAbstractProtocol;

/**
 * The class VSRaftProtocol, a skeleton for a Raft-based protocol.
 *
 * @author Paul C. Buetow
 */
public class VSRaftProtocol extends VSAbstractProtocol {
    /** The current Raft term. */
    private int currentTerm;

    /** The PID voted for in the current term. */
    private int votedFor;

    /** The number of votes received while acting as a candidate. */
    private int votesReceived;

    /** The current leader PID. */
    private int leaderId;

    /** True if this process currently acts as the leader. */
    private boolean isLeader;

    /** True if this process currently acts as a candidate. */
    private boolean isCandidate;

    /** The local time when the last heartbeat was observed. */
    private long lastHeartbeatTime;

    /** The randomized local deadline for the next election timeout. */
    private long electionDeadline;

    /** PIDs which still have to acknowledge the current operation. */
    private ArrayList<Integer> ackPids;

    /** Peer PIDs whose vote responses have been counted in this election. */
    private ArrayList<Integer> voteResponsePids;

    /** The local log index. */
    private int logIndex;

    /** The last committed log index. */
    private int commitIndex;

    /**
     * Instantiates a new Raft protocol skeleton.
     */
    public VSRaftProtocol() {
        super(VSAbstractProtocol.HAS_ON_SERVER_START);
        setClassname(getClass().toString());
        resetState();
    }

    /* (non-Javadoc)
     * @see events.VSAbstractProtocol#onServerInit()
     */
    public void onServerInit() {
        Vector<Integer> vec = new Vector<Integer>();
        vec.add(2);
        vec.add(3);

        initVector("pids", vec, "PIDs of participating follower processes");
        initLong("heartbeatInterval", 1500, "Heartbeat interval", "ms");
        initString("logEntry", "cmd1", "Log entry to replicate");
    }

    /* (non-Javadoc)
     * @see events.VSAbstractProtocol#onClientInit()
     */
    public void onClientInit() {
        initLong("electionTimeout", 4000, "Base election timeout", "ms");
        initLong("electionJitter", 2000, "Election timeout jitter", "ms");
        resetElectionTimeout();
    }

    /* (non-Javadoc)
     * @see protocols.VSAbstractProtocol#onServerStart()
     */
    public void onServerStart() {
        becomeLeader();
    }

    /* (non-Javadoc)
     * @see protocols.VSAbstractProtocol#onServerRecv(core.VSMessage)
     */
    public void onServerRecv(VSMessage recvMessage) {
        handleMessage(recvMessage);
    }

    /* (non-Javadoc)
     * @see protocols.VSAbstractProtocol#onClientRecv(core.VSMessage)
     */
    public void onClientRecv(VSMessage recvMessage) {
        handleMessage(recvMessage);
    }

    /* (non-Javadoc)
     * @see protocols.VSAbstractProtocol#onServerSchedule()
     */
    public void onServerSchedule() {
        if (isLeader) {
            sendHeartbeat();
        }
    }

    /* (non-Javadoc)
     * @see protocols.VSAbstractProtocol#onClientSchedule()
     */
    public void onClientSchedule() {
        long currentTime = process.getTime();

        if (!isLeader && currentTime >= electionDeadline) {
            startElection();
        }
    }

    /* (non-Javadoc)
     * @see protocols.VSAbstractProtocol#onServerReset()
     */
    public void onServerReset() {
        resetState();
    }

    /* (non-Javadoc)
     * @see protocols.VSAbstractProtocol#onClientReset()
     */
    public void onClientReset() {
        resetState();
    }

    /**
     * Resets the shared Raft state to its initial values.
     */
    private void resetState() {
        currentTerm = 0;
        votedFor = -1;
        votesReceived = 0;
        leaderId = -1;
        isLeader = false;
        isCandidate = false;
        lastHeartbeatTime = 0;
        electionDeadline = 0;
        logIndex = 0;
        commitIndex = 0;

        if (ackPids == null) {
            ackPids = new ArrayList<Integer>();
        } else {
            ackPids.clear();
        }

        if (voteResponsePids == null) {
            voteResponsePids = new ArrayList<Integer>();
        } else {
            voteResponsePids.clear();
        }
    }

    /**
     * Transitions this process into the leader role and starts heartbeats.
     */
    private void becomeLeader() {
        isLeader = true;
        isCandidate = false;
        votesReceived = 0;
        voteResponsePids.clear();
        ackPids.clear();
        leaderId = process.getProcessID();
        lastHeartbeatTime = process.getTime();
        isServer(true);

        if (!getLongKeySet().contains("heartbeatInterval")) {
            onServerInit();
        }

        boolean previousContextIsServer = currentContextIsServer();

        currentContextIsServer(false);
        removeSchedules();

        currentContextIsServer(true);
        sendHeartbeat();
        sendAppendEntry();
        currentContextIsServer(previousContextIsServer);
    }

    /**
     * Transitions this process into the follower role for the supplied term.
     *
     * @param term the term to adopt
     * @param newLeaderId the known leader in that term, or -1 if unknown
     */
    private void becomeFollower(int term, int newLeaderId) {
        clearServerSchedules();
        isLeader = false;
        isCandidate = false;
        currentTerm = term;
        leaderId = newLeaderId;
        votedFor = -1;
        votesReceived = 0;
        voteResponsePids.clear();
        resetElectionTimeout();
    }

    /**
     * Resets the follower election timeout using a randomized client schedule.
     */
    private void resetElectionTimeout() {
        long jitterPercentage = Math.abs(process.getRandomPercentage());
        long jitter = (getLong("electionJitter") * jitterPercentage) / 100L;
        boolean previousContextIsServer = currentContextIsServer();
        electionDeadline = process.getTime() + getLong("electionTimeout") + jitter;

        currentContextIsServer(false);
        removeSchedules();
        scheduleAt(electionDeadline);
        currentContextIsServer(previousContextIsServer);
    }

    /**
     * Clears any active server-side schedules while preserving the caller
     * context.
     */
    private void clearServerSchedules() {
        boolean previousContextIsServer = currentContextIsServer();

        currentContextIsServer(true);
        removeSchedules();
        currentContextIsServer(previousContextIsServer);
    }

    /**
     * Starts a new election and re-arms the candidate timeout.
     */
    private void startElection() {
        currentTerm++;
        votedFor = process.getProcessID();
        votesReceived = 1;
        voteResponsePids.clear();
        isLeader = false;
        isCandidate = true;
        leaderId = -1;
        lastHeartbeatTime = process.getTime();
        isServer(true);

        VSMessage voteRequest = new VSMessage();
        voteRequest.setString("type", "voteRequest");
        voteRequest.setInteger("term", currentTerm);
        voteRequest.setInteger("candidateId", process.getProcessID());
        sendMessage(voteRequest);

        resetElectionTimeout();
    }

    /**
     * Sends a heartbeat and schedules the next leader heartbeat interval.
     */
    private void sendHeartbeat() {
        VSMessage heartbeat = new VSMessage();
        heartbeat.setString("type", "heartbeat");
        heartbeat.setInteger("term", currentTerm);
        heartbeat.setInteger("leaderId", leaderId);
        sendMessage(heartbeat);

        lastHeartbeatTime = process.getTime();
        scheduleAt(process.getTime() + getLong("heartbeatInterval"));
    }

    /**
     * Sends a simplified append-entry request for the configured log entry.
     */
    private void sendAppendEntry() {
        ackPids.clear();

        if (getVectorKeySet().contains("pids")) {
            ackPids.addAll(getVector("pids"));
        }

        if (ackPids.isEmpty()) {
            return;
        }

        logIndex++;

        VSMessage appendEntry = new VSMessage();
        appendEntry.setString("type", "appendEntry");
        appendEntry.setInteger("term", currentTerm);
        appendEntry.setInteger("leaderId", leaderId);
        appendEntry.setString("entry", getString("logEntry"));
        appendEntry.setInteger("logIndex", logIndex);
        sendMessage(appendEntry);
    }

    /**
     * Dispatches Raft messages to the relevant handlers.
     *
     * @param recvMessage the received message
     */
    private void handleMessage(VSMessage recvMessage) {
        String messageType = recvMessage.getString("type");

        if (messageType == null) {
            return;
        }

        switch (messageType) {
        case "heartbeat":
            handleHeartbeat(recvMessage);
            break;
        case "heartbeatAck":
            handleHeartbeatAck(recvMessage);
            break;
        case "voteRequest":
            handleVoteRequest(recvMessage);
            break;
        case "voteResponse":
            handleVoteResponse(recvMessage);
            break;
        case "appendEntry":
            handleAppendEntry(recvMessage);
            break;
        case "appendAck":
            handleAppendAck(recvMessage);
            break;
        default:
            break;
        }
    }

    /**
     * Handles an incoming heartbeat from the current leader.
     *
     * @param recvMessage the heartbeat message
     */
    private void handleHeartbeat(VSMessage recvMessage) {
        int messageTerm = recvMessage.getInteger("term");
        int messageLeaderId = recvMessage.getInteger("leaderId");

        if (messageTerm < currentTerm) {
            return;
        }

        if (messageTerm > currentTerm) {
            becomeFollower(messageTerm, messageLeaderId);
        } else {
            leaderId = messageLeaderId;
            isLeader = false;
            isCandidate = false;
            resetElectionTimeout();
        }

        lastHeartbeatTime = process.getTime();

        VSMessage heartbeatAck = new VSMessage();
        heartbeatAck.setString("type", "heartbeatAck");
        heartbeatAck.setInteger("term", currentTerm);
        heartbeatAck.setInteger("pid", process.getProcessID());
        heartbeatAck.setInteger("targetPid", messageLeaderId);
        sendMessage(heartbeatAck);
    }

    /**
     * Handles an incoming heartbeat acknowledgement on the leader.
     *
     * @param recvMessage the heartbeat acknowledgement
     */
    private void handleHeartbeatAck(VSMessage recvMessage) {
        int messageTerm = recvMessage.getInteger("term");
        Integer responderPid = recvMessage.getIntegerObj("pid");

        if (messageTerm > currentTerm) {
            becomeFollower(messageTerm, -1);
            return;
        }

        if (!isLeader || !isForMe(recvMessage) || responderPid == null ||
                messageTerm != currentTerm) {
            return;
        }

        log("Heartbeat ACK from process " + responderPid + " received");
    }

    /**
     * Handles an incoming vote request from a candidate.
     *
     * @param recvMessage the vote request
     */
    private void handleVoteRequest(VSMessage recvMessage) {
        int messageTerm = recvMessage.getInteger("term");
        int candidateId = recvMessage.getInteger("candidateId");
        boolean voteGranted = false;

        if (messageTerm > currentTerm) {
            becomeFollower(messageTerm, -1);
        }

        if (messageTerm == currentTerm &&
                (votedFor == -1 || votedFor == candidateId)) {
            votedFor = candidateId;
            voteGranted = true;
        }

        VSMessage voteResponse = new VSMessage();
        voteResponse.setString("type", "voteResponse");
        voteResponse.setInteger("term", currentTerm);
        voteResponse.setInteger("pid", process.getProcessID());
        voteResponse.setBoolean("voteGranted", voteGranted);
        voteResponse.setInteger("targetPid", candidateId);
        sendMessage(voteResponse);
    }

    /**
     * Handles an incoming vote response for an active election.
     *
     * @param recvMessage the vote response
     */
    private void handleVoteResponse(VSMessage recvMessage) {
        int messageTerm = recvMessage.getInteger("term");
        Integer responderPid = recvMessage.getIntegerObj("pid");

        if (messageTerm > currentTerm) {
            becomeFollower(messageTerm, -1);
            return;
        }

        if (!isCandidate || !isForMe(recvMessage) ||
                !recvMessage.getBoolean("voteGranted") ||
                messageTerm != currentTerm ||
                voteResponsePids.contains(responderPid)) {
            return;
        }

        voteResponsePids.add(responderPid);
        votesReceived++;

        if (votesReceived > getClusterSize() / 2) {
            becomeLeader();
        }
    }

    /**
     * Handles an incoming append-entry request from the current leader.
     *
     * @param recvMessage the append-entry message
     */
    private void handleAppendEntry(VSMessage recvMessage) {
        int messageTerm = recvMessage.getInteger("term");
        int messageLeaderId = recvMessage.getInteger("leaderId");
        int messageLogIndex = recvMessage.getInteger("logIndex");
        boolean isSameTerm = messageTerm == currentTerm;

        if (messageTerm < currentTerm) {
            return;
        }

        if (messageTerm > currentTerm) {
            becomeFollower(messageTerm, messageLeaderId);
        }

        if (messageLogIndex != logIndex + 1) {
            return;
        }

        if (isSameTerm) {
            leaderId = messageLeaderId;
            isLeader = false;
            isCandidate = false;
            resetElectionTimeout();
        }

        logIndex = messageLogIndex;

        VSMessage appendAck = new VSMessage();
        appendAck.setString("type", "appendAck");
        appendAck.setInteger("term", currentTerm);
        appendAck.setInteger("pid", process.getProcessID());
        appendAck.setInteger("logIndex", messageLogIndex);
        appendAck.setInteger("targetPid", messageLeaderId);
        sendMessage(appendAck);
    }

    /**
     * Handles an append-entry acknowledgement on the leader.
     *
     * @param recvMessage the append acknowledgement
     */
    private void handleAppendAck(VSMessage recvMessage) {
        int messageTerm = recvMessage.getInteger("term");
        Integer responderPid = recvMessage.getIntegerObj("pid");
        int ackLogIndex = recvMessage.getInteger("logIndex");

        if (messageTerm > currentTerm) {
            becomeFollower(messageTerm, -1);
            return;
        }

        if (!isLeader || !isForMe(recvMessage) || responderPid == null ||
                messageTerm != currentTerm || ackLogIndex != logIndex ||
                !ackPids.contains(responderPid)) {
            return;
        }

        ackPids.remove(responderPid);

        if (ackPids.isEmpty() && commitIndex < ackLogIndex) {
            commitIndex = ackLogIndex;
            log("Committed log index " + commitIndex);
        }
    }

    /**
     * Checks whether a directed response is meant for this process.
     *
     * @param recvMessage the received message
     * @return true if the message targets this process or has no target field
     */
    private boolean isForMe(VSMessage recvMessage) {
        if (!recvMessage.getIntegerKeySet().contains("targetPid")) {
            return true;
        }

        return recvMessage.getInteger("targetPid") == process.getProcessID();
    }

    /**
     * Determines the cluster size used for majority calculations.
     *
     * @return the number of processes participating in the election
     */
    private int getClusterSize() {
        VSInternalProcess internalProcess = (VSInternalProcess) process;
        int numProcesses = internalProcess.getSimulatorCanvas().getNumProcesses();

        if (numProcesses > 0) {
            return numProcesses;
        }

        if (getVectorKeySet().contains("pids")) {
            return getVector("pids").size() + 1;
        }

        return 1;
    }
}
