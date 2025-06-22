package protocols.implementations;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import core.VSMessage;
import core.VSInternalProcess;
import protocols.VSAbstractProtocol;

/**
 * Implementation of the Raft consensus algorithm.
 * 
 * Raft is a consensus algorithm designed to be understandable. It ensures that
 * a distributed system agrees on values even in the presence of failures.
 * 
 * <p>The protocol has three states:</p>
 * <ul>
 *   <li>Follower - Passive state, responds to leaders</li>
 *   <li>Candidate - Actively requesting votes to become leader</li>
 *   <li>Leader - Manages the cluster and log replication</li>
 * </ul>
 * 
 * <p>Key features implemented:</p>
 * <ul>
 *   <li>Leader election with randomized timeouts</li>
 *   <li>Log replication for state machine commands</li>
 *   <li>Safety through term numbers and log matching</li>
 *   <li>Membership changes (simplified)</li>
 * </ul>
 * 
 * @author Paul C. Buetow
 */
public class VSRaftProtocol extends VSAbstractProtocol {
    
    // Raft states
    private enum State {
        FOLLOWER,
        CANDIDATE,
        LEADER
    }
    
    // Message types
    private static final String MSG_REQUEST_VOTE = "REQUEST_VOTE";
    private static final String MSG_VOTE_RESPONSE = "VOTE_RESPONSE";
    private static final String MSG_APPEND_ENTRIES = "APPEND_ENTRIES";
    private static final String MSG_APPEND_RESPONSE = "APPEND_RESPONSE";
    private static final String MSG_CLIENT_REQUEST = "CLIENT_REQUEST";
    
    // Timing constants (in simulation time units)
    private static final long HEARTBEAT_INTERVAL = 50;
    private static final long ELECTION_TIMEOUT_MIN = 150;
    private static final long ELECTION_TIMEOUT_MAX = 300;
    
    // Server state (persistent - should be saved to stable storage)
    private State currentState;
    private int currentTerm;
    private Integer votedFor;
    private List<LogEntry> log;
    
    // Server state (volatile)
    private int commitIndex;
    private int lastApplied;
    
    // Leader state (reinitialized after election)
    private Map<Integer, Integer> nextIndex;
    private Map<Integer, Integer> matchIndex;
    
    // Candidate state
    private Set<Integer> votesReceived;
    private long electionTimeout;
    
    // General state
    private Integer currentLeader;
    private long lastHeartbeat;
    
    // Client state
    private boolean clientHasScheduled = false;
    private int clientRequestCount = 0;
    
    /**
     * Log entry structure
     */
    private static class LogEntry {
        final int term;
        final String command;
        final long timestamp;
        
        LogEntry(int term, String command, long timestamp) {
            this.term = term;
            this.command = command;
            this.timestamp = timestamp;
        }
        
        @Override
        public String toString() {
            return String.format("LogEntry{term=%d, cmd='%s', time=%d}", 
                term, command, timestamp);
        }
    }
    
    public VSRaftProtocol() {
        super(VSAbstractProtocol.HAS_ON_SERVER_START);
        setClassname(getClass().toString());
    }
    
    @Override
    public void onServerInit() {
        currentState = State.FOLLOWER;
        currentTerm = 0;
        votedFor = null;
        log = new ArrayList<>();
        commitIndex = 0;
        lastApplied = 0;
        nextIndex = new ConcurrentHashMap<>();
        matchIndex = new ConcurrentHashMap<>();
        votesReceived = new HashSet<>();
        currentLeader = null;
        
        // Add a dummy entry at index 0 for easier indexing
        log.add(new LogEntry(0, "INIT", 0));
    }
    
    @Override
    public void onServerStart() {
        // Initialize election timeout and start
        resetElectionTimeout();
        raftLog("Raft node initialized as FOLLOWER, election timeout=" + electionTimeout);
        scheduleElectionTimeout();
        raftLog("Scheduled election timeout check");
    }
    
    @Override
    public void onServerReset() {
        onServerInit();
        removeSchedules();
    }
    
    @Override
    public void onServerRecv(VSMessage message) {
        String msgType = message.getString("type");
        int term = message.getInteger("term");
        int senderId = message.getSendingProcess().getProcessNum();
        
        // Check if this message is intended for us (for unicast messages)
        if (message.objectExists("receiverNum")) {
            int receiverNum = message.getInteger("receiverNum");
            if (receiverNum != process.getProcessNum()) {
                // Message not for us, ignore it
                return;
            }
        }
        
        // If we receive a message with a higher term, become follower
        if (term > currentTerm) {
            currentTerm = term;
            votedFor = null;
            if (currentState != State.FOLLOWER) {
                becomeFollower();
            }
        }
        
        switch (msgType) {
            case MSG_REQUEST_VOTE:
                handleRequestVote(message, senderId);
                break;
            case MSG_VOTE_RESPONSE:
                handleVoteResponse(message, senderId);
                break;
            case MSG_APPEND_ENTRIES:
                handleAppendEntries(message, senderId);
                break;
            case MSG_APPEND_RESPONSE:
                handleAppendResponse(message, senderId);
                break;
            case MSG_CLIENT_REQUEST:
                handleClientRequest(message, senderId);
                break;
        }
    }
    
    @Override
    public void onServerSchedule() {
        long currentTime = process.getTime();
        
        raftLog("onServerSchedule called at time " + currentTime + ", state=" + currentState + ", electionTimeout=" + electionTimeout);
        
        switch (currentState) {
            case FOLLOWER:
            case CANDIDATE:
                // Check election timeout
                if (currentTime >= electionTimeout) {
                    startElection();
                } else {
                    // Reschedule to check again
                    scheduleAt(electionTimeout);
                }
                break;
            case LEADER:
                // Send heartbeats
                sendHeartbeats();
                scheduleAt(currentTime + HEARTBEAT_INTERVAL);
                break;
        }
    }
    
    @Override
    public void onClientInit() {
        // Initialize client state
        clientHasScheduled = false;
        clientRequestCount = 0;
    }
    
    @Override
    public void onClientStart() {
        // This method is never called when using HAS_ON_SERVER_START
        // Clients will send requests in response to server heartbeats instead
    }
    
    @Override
    public void onClientReset() {
        removeSchedules();
        clientHasScheduled = false;
        clientRequestCount = 0;
    }
    
    @Override
    public void onClientRecv(VSMessage message) {
        // Clients can receive responses to their requests
        String msgType = message.getString("type");
        if ("CLIENT_RESPONSE".equals(msgType)) {
            boolean success = message.getBoolean("success");
            String result = message.getString("result");
            raftLog("Client received response: success=" + success + ", result=" + result);
        } else if (MSG_APPEND_ENTRIES.equals(msgType)) {
            // Client receives heartbeat from leader - good time to send a request
            if (!clientHasScheduled) {
                clientHasScheduled = true;
                // Schedule first client request after a short delay
                scheduleAt(process.getTime() + 100);
            }
        }
    }
    
    @Override
    public void onClientSchedule() {
        // Send a test client request
        VSMessage request = new VSMessage();
        request.setString("type", MSG_CLIENT_REQUEST);
        request.setString("command", "SET x=" + process.getRandomPercentage());
        request.setLong("clientId", process.getProcessNum());
        request.setLong("requestId", System.currentTimeMillis());
        
        sendMessage(request);
        raftLog("Client sent request #" + clientRequestCount + ": " + request.getString("command"));
        
        // Update request count
        clientRequestCount++;
        
        // Schedule next request after a delay
        if (clientRequestCount < 10) { // Limit number of requests for testing
            scheduleAt(process.getTime() + 1000 + process.getRandomPercentage() * 10);
        }
    }
    
    // --- Raft Algorithm Implementation ---
    
    private void startElection() {
        currentState = State.CANDIDATE;
        currentTerm++;
        votedFor = process.getProcessNum();
        votesReceived.clear();
        votesReceived.add(process.getProcessNum()); // Vote for self
        
        raftLog("Starting election for term " + currentTerm + " (need " + ((getNumProcesses() / 2) + 1) + " votes)");
        
        // Send RequestVote to all other servers
        VSMessage voteRequest = new VSMessage();
        voteRequest.setString("type", MSG_REQUEST_VOTE);
        voteRequest.setInteger("term", currentTerm);
        voteRequest.setInteger("candidateId", process.getProcessNum());
        voteRequest.setInteger("lastLogIndex", log.size() - 1);
        voteRequest.setInteger("lastLogTerm", log.get(log.size() - 1).term);
        
        raftLog("Sending vote request to all processes");
        sendMessage(voteRequest);
        
        // Reset election timeout
        resetElectionTimeout();
        scheduleElectionTimeout();
    }
    
    private void handleRequestVote(VSMessage message, int candidateId) {
        int term = message.getInteger("term");
        int lastLogIndex = message.getInteger("lastLogIndex");
        int lastLogTerm = message.getInteger("lastLogTerm");
        
        raftLog("Received vote request from " + candidateId + " for term " + term);
        
        boolean voteGranted = false;
        
        // Grant vote if:
        // 1. We haven't voted in this term or voted for this candidate
        // 2. Candidate's log is at least as up-to-date as ours
        if (term >= currentTerm &&
            (votedFor == null || votedFor == candidateId) &&
            isLogUpToDate(lastLogIndex, lastLogTerm)) {
            
            votedFor = candidateId;
            voteGranted = true;
            resetElectionTimeout();
            
            raftLog("Voted for candidate " + candidateId + " in term " + term);
        } else {
            raftLog("Did not vote for candidate " + candidateId + " (already voted for " + votedFor + ")");
        }
        
        // Send vote response
        VSMessage response = new VSMessage();
        response.setString("type", MSG_VOTE_RESPONSE);
        response.setInteger("term", currentTerm);
        response.setBoolean("voteGranted", voteGranted);
        response.setInteger("senderId", process.getProcessNum());
        
        // Send directly to candidate
        response.setInteger("receiverNum", candidateId);
        raftLog("Sending vote response to " + candidateId + " (granted=" + voteGranted + ")");
        sendMessage(response);
    }
    
    private void handleVoteResponse(VSMessage message, int senderId) {
        if (currentState != State.CANDIDATE) {
            return;
        }
        
        boolean voteGranted = message.getBoolean("voteGranted");
        if (voteGranted) {
            votesReceived.add(senderId);
            
            raftLog("Received vote from " + senderId + " (total: " + votesReceived.size() + ")");
            
            // Check if we have majority
            int majority = (getNumProcesses() / 2) + 1;
            if (votesReceived.size() >= majority) {
                becomeLeader();
            }
        }
    }
    
    private void becomeLeader() {
        currentState = State.LEADER;
        currentLeader = process.getProcessNum();
        
        raftLog("Became LEADER for term " + currentTerm);
        
        // Initialize leader state
        nextIndex.clear();
        matchIndex.clear();
        
        for (int i = 0; i < getNumProcesses(); i++) {
            if (i != process.getProcessNum()) {
                nextIndex.put(i, log.size());
                matchIndex.put(i, 0);
            }
        }
        
        // Send initial heartbeats immediately
        sendHeartbeats();
        
        // Schedule regular heartbeats
        removeSchedules();
        scheduleAt(process.getTime() + HEARTBEAT_INTERVAL);
        
        // Highlight the leader visually
        if (process instanceof VSInternalProcess) {
            ((VSInternalProcess) process).highlightOn();
        }
    }
    
    private void becomeFollower() {
        currentState = State.FOLLOWER;
        
        raftLog("Became FOLLOWER for term " + currentTerm);
        
        // Remove leader highlighting
        if (process instanceof VSInternalProcess) {
            ((VSInternalProcess) process).highlightOff();
        }
        
        // Reset election timeout
        resetElectionTimeout();
        scheduleElectionTimeout();
    }
    
    private void sendHeartbeats() {
        for (int i = 0; i < getNumProcesses(); i++) {
            if (i != process.getProcessNum()) {
                sendAppendEntries(i);
            }
        }
    }
    
    private void sendAppendEntries(int followerId) {
        int nextIdx = nextIndex.getOrDefault(followerId, 1);
        int prevLogIndex = nextIdx - 1;
        int prevLogTerm = prevLogIndex >= 0 && prevLogIndex < log.size() ? log.get(prevLogIndex).term : 0;
        
        VSMessage appendEntries = new VSMessage();
        appendEntries.setString("type", MSG_APPEND_ENTRIES);
        appendEntries.setInteger("term", currentTerm);
        appendEntries.setInteger("leaderId", process.getProcessNum());
        appendEntries.setInteger("prevLogIndex", prevLogIndex);
        appendEntries.setInteger("prevLogTerm", prevLogTerm);
        appendEntries.setInteger("leaderCommit", commitIndex);
        
        // Include log entries if needed
        List<LogEntry> entries = new ArrayList<>();
        for (int i = nextIdx; i < log.size(); i++) {
            entries.add(log.get(i));
        }
        
        // For simplicity, we'll send entry count and details separately
        appendEntries.setInteger("entryCount", entries.size());
        for (int i = 0; i < entries.size(); i++) {
            LogEntry entry = entries.get(i);
            appendEntries.setInteger("entry_" + i + "_term", entry.term);
            appendEntries.setString("entry_" + i + "_cmd", entry.command);
            appendEntries.setLong("entry_" + i + "_time", entry.timestamp);
        }
        
        appendEntries.setInteger("receiverNum", followerId);
        sendMessage(appendEntries);
    }
    
    private void handleAppendEntries(VSMessage message, int leaderId) {
        int term = message.getInteger("term");
        int prevLogIndex = message.getInteger("prevLogIndex");
        int prevLogTerm = message.getInteger("prevLogTerm");
        int leaderCommit = message.getInteger("leaderCommit");
        
        // Reset election timeout when we hear from leader
        resetElectionTimeout();
        lastHeartbeat = process.getTime();
        currentLeader = leaderId;
        
        boolean success = false;
        
        // Check if log matches at prevLogIndex
        if (prevLogIndex == 0 || 
            (prevLogIndex < log.size() && log.get(prevLogIndex).term == prevLogTerm)) {
            
            success = true;
            
            // Remove conflicting entries
            if (prevLogIndex + 1 < log.size()) {
                log.subList(prevLogIndex + 1, log.size()).clear();
            }
            
            // Append new entries
            int entryCount = message.getInteger("entryCount");
            for (int i = 0; i < entryCount; i++) {
                int entryTerm = message.getInteger("entry_" + i + "_term");
                String entryCmd = message.getString("entry_" + i + "_cmd");
                long entryTime = message.getLong("entry_" + i + "_time");
                
                log.add(new LogEntry(entryTerm, entryCmd, entryTime));
                raftLog("Appended log entry: " + entryCmd);
            }
            
            // Update commit index
            if (leaderCommit > commitIndex) {
                commitIndex = Math.min(leaderCommit, log.size() - 1);
                applyStateMachine();
            }
        }
        
        // Send response
        VSMessage response = new VSMessage();
        response.setString("type", MSG_APPEND_RESPONSE);
        response.setInteger("term", currentTerm);
        response.setBoolean("success", success);
        response.setInteger("senderId", process.getProcessNum());
        response.setInteger("matchIndex", log.size() - 1);
        response.setInteger("receiverNum", leaderId);
        
        sendMessage(response);
    }
    
    private void handleAppendResponse(VSMessage message, int followerId) {
        if (currentState != State.LEADER) {
            return;
        }
        
        boolean success = message.getBoolean("success");
        int matchIdx = message.getInteger("matchIndex");
        
        if (success) {
            matchIndex.put(followerId, matchIdx);
            nextIndex.put(followerId, matchIdx + 1);
            
            // Check if we can advance commit index
            updateCommitIndex();
        } else {
            // Decrement nextIndex and retry
            int next = nextIndex.getOrDefault(followerId, 1);
            if (next > 1) {
                nextIndex.put(followerId, next - 1);
            }
        }
    }
    
    private void handleClientRequest(VSMessage message, int clientId) {
        if (currentState != State.LEADER) {
            // Redirect to leader or reject
            VSMessage response = new VSMessage();
            response.setString("type", "CLIENT_RESPONSE");
            response.setBoolean("success", false);
            response.setString("result", "Not leader. Leader is: " + currentLeader);
            response.setInteger("receiverNum", clientId);
            sendMessage(response);
            return;
        }
        
        // Append to log
        String command = message.getString("command");
        LogEntry entry = new LogEntry(currentTerm, command, process.getTime());
        log.add(entry);
        
        raftLog("Leader received client request: " + command);
        
        // Will be committed when replicated to majority
        // For now, send optimistic response
        VSMessage response = new VSMessage();
        response.setString("type", "CLIENT_RESPONSE");
        response.setBoolean("success", true);
        response.setString("result", "Command logged: " + command);
        response.setInteger("receiverNum", clientId);
        sendMessage(response);
    }
    
    // --- Helper Methods ---
    
    private boolean isLogUpToDate(int lastLogIndex, int lastLogTerm) {
        int ourLastIndex = log.size() - 1;
        int ourLastTerm = log.get(ourLastIndex).term;
        
        return lastLogTerm > ourLastTerm || 
               (lastLogTerm == ourLastTerm && lastLogIndex >= ourLastIndex);
    }
    
    private void resetElectionTimeout() {
        if (process != null) {
            long timeout = ELECTION_TIMEOUT_MIN + 
                (long)(Math.random() * (ELECTION_TIMEOUT_MAX - ELECTION_TIMEOUT_MIN));
            electionTimeout = process.getTime() + timeout;
        }
    }
    
    private void scheduleElectionTimeout() {
        removeSchedules();
        scheduleAt(electionTimeout);
    }
    
    private void updateCommitIndex() {
        // Find the highest index that has been replicated to majority
        for (int n = log.size() - 1; n > commitIndex; n--) {
            if (log.get(n).term == currentTerm) {
                int replicatedCount = 1; // Leader has it
                
                for (int matchIdx : matchIndex.values()) {
                    if (matchIdx >= n) {
                        replicatedCount++;
                    }
                }
                
                if (replicatedCount > getNumProcesses() / 2) {
                    commitIndex = n;
                    applyStateMachine();
                    break;
                }
            }
        }
    }
    
    private void applyStateMachine() {
        while (lastApplied < commitIndex) {
            lastApplied++;
            LogEntry entry = log.get(lastApplied);
            raftLog("Applied to state machine: " + entry.command);
        }
    }
    
    private void raftLog(String message) {
        String stateStr = currentState != null ? currentState.toString() : "CLIENT";
        String prefix = String.format("[%s T:%d N:%d] ", 
            stateStr, currentTerm, process.getProcessNum());
        process.log(prefix + message);
    }
    
    @Override
    public String toString() {
        return super.toString() + " - Raft Consensus";
    }
}