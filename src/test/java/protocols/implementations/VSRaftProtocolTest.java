package protocols.implementations;

import core.VSInternalProcess;
import core.VSMessage;
import core.VSTask;
import core.VSTaskManager;
import core.time.VSVectorTime;
import events.internal.VSProtocolScheduleEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import prefs.VSPrefs;
import simulator.VSSimulatorVisualization;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for VSRaftProtocol election and log replication behavior.
 */
class VSRaftProtocolTest {

    @Mock
    private VSInternalProcess mockProcess;

    @Mock
    private VSSimulatorVisualization mockCanvas;

    @Mock
    private VSTaskManager mockTaskManager;

    @Mock
    private VSPrefs mockPrefs;

    @Mock
    private VSVectorTime mockVectorTime;

    private VSRaftProtocol protocol;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        protocol = new VSRaftProtocol();
        protocol.process = mockProcess;
        protocol.prefs = mockPrefs;
        protocol.isServer(true);

        when(mockProcess.getSimulatorCanvas()).thenReturn(mockCanvas);
        when(mockCanvas.getTaskManager()).thenReturn(mockTaskManager);
        when(mockCanvas.getNumProcesses()).thenReturn(3);
        when(mockProcess.getPrefs()).thenReturn(mockPrefs);
        when(mockProcess.getVectorTime()).thenReturn(mockVectorTime);
        when(mockVectorTime.getCopy()).thenReturn(mockVectorTime);
        when(mockPrefs.getString(anyString())).thenReturn("TestString");
        when(mockProcess.getTime()).thenReturn(100L);
        when(mockProcess.getProcessID()).thenReturn(7);
        when(mockProcess.getRandomPercentage()).thenReturn(25);
    }

    @Test
    void testOnStartBecomesLeaderAndSendsHeartbeat() throws Exception {
        ArgumentCaptor<VSMessage> messageCaptor =
            ArgumentCaptor.forClass(VSMessage.class);
        ArgumentCaptor<VSTask> taskCaptor = ArgumentCaptor.forClass(VSTask.class);

        protocol.onStart();

        verify(mockProcess, times(2)).sendMessage(messageCaptor.capture());
        verify(mockTaskManager).addTask(taskCaptor.capture());

        VSMessage heartbeat = messageCaptor.getAllValues().get(0);
        assertEquals("heartbeat", heartbeat.getString("type"));
        assertEquals(0, heartbeat.getInteger("term"));
        assertEquals(7, heartbeat.getInteger("leaderId"));
        VSMessage appendEntry = messageCaptor.getAllValues().get(1);
        assertEquals("appendEntry", appendEntry.getString("type"));
        assertEquals(0, appendEntry.getInteger("term"));
        assertEquals(7, appendEntry.getInteger("leaderId"));
        assertEquals("cmd1", appendEntry.getString("entry"));
        assertEquals(1, appendEntry.getInteger("logIndex"));
        assertEquals(2, getAckPids().size());
        assertTrue(getAckPids().contains(2));
        assertTrue(getAckPids().contains(3));
        assertEquals(1600L, taskCaptor.getValue().getTaskTime());
    }

    @Test
    void testServerScheduleSendsHeartbeatWhenLeader() {
        ArgumentCaptor<VSMessage> messageCaptor =
            ArgumentCaptor.forClass(VSMessage.class);
        ArgumentCaptor<VSTask> taskCaptor = ArgumentCaptor.forClass(VSTask.class);

        protocol.onStart();
        protocol.onServerScheduleStart();

        verify(mockProcess, times(3)).sendMessage(messageCaptor.capture());
        verify(mockTaskManager, times(2)).addTask(taskCaptor.capture());

        assertEquals(3, messageCaptor.getAllValues().size());
        assertEquals(2, taskCaptor.getAllValues().size());

        VSMessage scheduledHeartbeat = messageCaptor.getAllValues().get(2);
        assertEquals("heartbeat", scheduledHeartbeat.getString("type"));
        assertEquals(0, scheduledHeartbeat.getInteger("term"));
        assertEquals(7, scheduledHeartbeat.getInteger("leaderId"));
        assertEquals(1600L, taskCaptor.getAllValues().get(1).getTaskTime());
    }

    @Test
    void testServerScheduleDoesNothingWhenNotLeader() {
        protocol.currentContextIsServer(true);

        protocol.onServerSchedule();

        verify(mockProcess, never()).sendMessage(any());
        verify(mockTaskManager, never()).addTask(any());
    }

    @Test
    void testOnClientInitSchedulesRandomizedElectionTimeout() throws Exception {
        protocol.currentContextIsServer(false);

        ArgumentCaptor<VSTask> taskCaptor = ArgumentCaptor.forClass(VSTask.class);

        protocol.onClientInit();

        verify(mockTaskManager).removeAllTasks(any());
        verify(mockTaskManager).addTask(taskCaptor.capture());

        VSTask task = taskCaptor.getValue();
        assertEquals(4600L, task.getTaskTime());
        assertEquals(4600L, getLongField("electionDeadline"));
        assertFalse(((VSProtocolScheduleEvent) task.getEvent()).isServerSchedule());
    }

    @Test
    void testOnClientScheduleStartsElectionAfterTimeout() throws Exception {
        protocol.currentContextIsServer(false);
        protocol.onClientInit();
        clearInvocations(mockProcess, mockTaskManager);
        when(mockProcess.getTime()).thenReturn(4700L, 4700L, 4700L);

        ArgumentCaptor<VSMessage> messageCaptor =
            ArgumentCaptor.forClass(VSMessage.class);
        ArgumentCaptor<VSTask> taskCaptor = ArgumentCaptor.forClass(VSTask.class);

        protocol.onClientSchedule();

        verify(mockProcess).sendMessage(messageCaptor.capture());
        verify(mockTaskManager).removeAllTasks(any());
        verify(mockTaskManager).addTask(taskCaptor.capture());

        VSMessage voteRequest = messageCaptor.getValue();
        assertEquals("voteRequest", voteRequest.getString("type"));
        assertEquals(1, voteRequest.getInteger("term"));
        assertEquals(7, voteRequest.getInteger("candidateId"));
        assertTrue(getBooleanField("isCandidate"));
        assertFalse(getBooleanField("isLeader"));
        assertEquals(1, getIntField("votesReceived"));
        assertEquals(7, getIntField("votedFor"));
        assertEquals(9200L, taskCaptor.getValue().getTaskTime());
        assertEquals(9200L, getLongField("electionDeadline"));
        assertFalse(
            ((VSProtocolScheduleEvent) taskCaptor.getValue().getEvent())
                .isServerSchedule());
    }

    @Test
    void testOnClientScheduleDoesNotStartElectionBeforeTimeout() throws Exception {
        protocol.currentContextIsServer(false);
        protocol.onClientInit();
        clearInvocations(mockProcess, mockTaskManager);
        when(mockProcess.getTime()).thenReturn(2000L);

        protocol.onClientSchedule();

        verify(mockProcess, never()).sendMessage(any());
        verify(mockTaskManager, never()).removeAllTasks(any());
        verify(mockTaskManager, never()).addTask(any());
        assertFalse(getBooleanField("isCandidate"));
        assertFalse(getBooleanField("isLeader"));
        assertEquals(0, getIntField("currentTerm"));
        assertEquals(0, getIntField("votesReceived"));
        assertEquals(-1, getIntField("votedFor"));
    }

    @Test
    void testOnClientScheduleDoesNotStartElectionInJitterWindow() throws Exception {
        protocol.currentContextIsServer(false);
        protocol.onClientInit();
        clearInvocations(mockProcess, mockTaskManager);
        when(mockProcess.getTime()).thenReturn(4500L);

        protocol.onClientSchedule();

        verify(mockProcess, never()).sendMessage(any());
        verify(mockTaskManager, never()).removeAllTasks(any());
        verify(mockTaskManager, never()).addTask(any());
        assertFalse(getBooleanField("isCandidate"));
        assertEquals(0, getIntField("currentTerm"));
        assertEquals(4600L, getLongField("electionDeadline"));
    }

    @Test
    void testCandidateTimeoutStartsNewElectionAndReschedules() throws Exception {
        protocol.currentContextIsServer(false);
        protocol.onClientInit();
        when(mockProcess.getTime()).thenReturn(4700L, 4700L, 4700L);

        protocol.onClientSchedule();
        clearInvocations(mockProcess, mockTaskManager);
        when(mockProcess.getTime()).thenReturn(9401L, 9401L, 9401L);

        ArgumentCaptor<VSMessage> messageCaptor =
            ArgumentCaptor.forClass(VSMessage.class);
        ArgumentCaptor<VSTask> taskCaptor = ArgumentCaptor.forClass(VSTask.class);

        protocol.onClientSchedule();

        verify(mockProcess).sendMessage(messageCaptor.capture());
        verify(mockTaskManager).removeAllTasks(any());
        verify(mockTaskManager).addTask(taskCaptor.capture());

        VSMessage voteRequest = messageCaptor.getValue();
        assertEquals("voteRequest", voteRequest.getString("type"));
        assertEquals(2, voteRequest.getInteger("term"));
        assertEquals(2, getIntField("currentTerm"));
        assertEquals(1, getIntField("votesReceived"));
        assertEquals(13901L, taskCaptor.getValue().getTaskTime());
        assertEquals(13901L, getLongField("electionDeadline"));
    }

    @Test
    void testBecomeFollowerFromServerContextCancelsHeartbeatsAndRearmsClientTimeout()
    throws Exception {
        protocol.currentContextIsServer(false);
        protocol.onClientInit();
        clearInvocations(mockProcess, mockTaskManager);

        protocol.onStart();
        clearInvocations(mockProcess, mockTaskManager);
        protocol.currentContextIsServer(true);
        when(mockProcess.getTime()).thenReturn(300L);

        ArgumentCaptor<VSTask> taskCaptor = ArgumentCaptor.forClass(VSTask.class);

        invokeBecomeFollower(4, 11);

        verify(mockTaskManager, times(2)).removeAllTasks(any());
        verify(mockTaskManager).addTask(taskCaptor.capture());
        assertTrue(protocol.currentContextIsServer());
        assertFalse(getBooleanField("isLeader"));
        assertFalse(getBooleanField("isCandidate"));
        assertEquals(4, getIntField("currentTerm"));
        assertEquals(11, getIntField("leaderId"));
        assertEquals(-1, getIntField("votedFor"));
        assertEquals(4800L, taskCaptor.getValue().getTaskTime());
        assertFalse(
            ((VSProtocolScheduleEvent) taskCaptor.getValue().getEvent())
                .isServerSchedule());
    }

    @Test
    void testServerReceiveVoteRequestGrantsEligibleCandidate() throws Exception {
        protocol.currentContextIsServer(false);
        protocol.onClientInit();
        clearInvocations(mockProcess, mockTaskManager);
        protocol.currentContextIsServer(true);
        when(mockProcess.getTime()).thenReturn(200L, 200L);

        VSMessage voteRequest = new VSMessage();
        voteRequest.setString("type", "voteRequest");
        voteRequest.setInteger("term", 2);
        voteRequest.setInteger("candidateId", 11);

        ArgumentCaptor<VSMessage> messageCaptor =
            ArgumentCaptor.forClass(VSMessage.class);
        ArgumentCaptor<VSTask> taskCaptor = ArgumentCaptor.forClass(VSTask.class);

        protocol.onServerRecv(voteRequest);

        verify(mockProcess).sendMessage(messageCaptor.capture());
        verify(mockTaskManager, times(2)).removeAllTasks(any());
        verify(mockTaskManager).addTask(taskCaptor.capture());

        VSMessage voteResponse = messageCaptor.getValue();
        assertEquals("voteResponse", voteResponse.getString("type"));
        assertEquals(2, voteResponse.getInteger("term"));
        assertEquals(7, voteResponse.getInteger("pid"));
        assertTrue(voteResponse.getBoolean("voteGranted"));
        assertEquals(11, voteResponse.getInteger("targetPid"));
        assertEquals(2, getIntField("currentTerm"));
        assertEquals(11, getIntField("votedFor"));
        assertFalse(getBooleanField("isCandidate"));
        assertFalse(getBooleanField("isLeader"));
        assertEquals(4700L, taskCaptor.getValue().getTaskTime());
    }

    @Test
    void testServerReceiveVoteRequestDeniesWhenAlreadyVotedForOtherCandidate()
    throws Exception {
        setIntField("currentTerm", 3);
        setIntField("votedFor", 9);
        protocol.currentContextIsServer(true);

        VSMessage voteRequest = new VSMessage();
        voteRequest.setString("type", "voteRequest");
        voteRequest.setInteger("term", 3);
        voteRequest.setInteger("candidateId", 11);

        ArgumentCaptor<VSMessage> messageCaptor =
            ArgumentCaptor.forClass(VSMessage.class);

        protocol.onServerRecv(voteRequest);

        verify(mockProcess).sendMessage(messageCaptor.capture());
        verify(mockTaskManager, never()).removeAllTasks(any());
        verify(mockTaskManager, never()).addTask(any());

        VSMessage voteResponse = messageCaptor.getValue();
        assertEquals("voteResponse", voteResponse.getString("type"));
        assertEquals(3, voteResponse.getInteger("term"));
        assertFalse(voteResponse.getBoolean("voteGranted"));
        assertEquals(9, getIntField("votedFor"));
        assertEquals(3, getIntField("currentTerm"));
    }

    @Test
    void testServerReceiveHigherTermVoteRequestResetsVoteAndGrants()
    throws Exception {
        setIntField("currentTerm", 3);
        setIntField("votedFor", 9);
        setBooleanField("isCandidate", true);
        protocol.currentContextIsServer(false);
        protocol.onClientInit();
        clearInvocations(mockProcess, mockTaskManager);
        protocol.currentContextIsServer(true);
        when(mockProcess.getTime()).thenReturn(250L, 250L);

        VSMessage voteRequest = new VSMessage();
        voteRequest.setString("type", "voteRequest");
        voteRequest.setInteger("term", 4);
        voteRequest.setInteger("candidateId", 11);

        ArgumentCaptor<VSMessage> messageCaptor =
            ArgumentCaptor.forClass(VSMessage.class);
        ArgumentCaptor<VSTask> taskCaptor = ArgumentCaptor.forClass(VSTask.class);

        protocol.onServerRecv(voteRequest);

        verify(mockProcess).sendMessage(messageCaptor.capture());
        verify(mockTaskManager, times(2)).removeAllTasks(any());
        verify(mockTaskManager).addTask(taskCaptor.capture());

        VSMessage voteResponse = messageCaptor.getValue();
        assertEquals("voteResponse", voteResponse.getString("type"));
        assertEquals(4, voteResponse.getInteger("term"));
        assertTrue(voteResponse.getBoolean("voteGranted"));
        assertEquals(4, getIntField("currentTerm"));
        assertEquals(11, getIntField("votedFor"));
        assertFalse(getBooleanField("isCandidate"));
        assertFalse(getBooleanField("isLeader"));
        assertEquals(4750L, taskCaptor.getValue().getTaskTime());
    }

    @Test
    void testClientReceiveHeartbeatBecomesFollowerResetsTimeoutAndSendsAck()
    throws Exception {
        protocol.currentContextIsServer(false);
        protocol.onClientInit();
        clearInvocations(mockProcess, mockTaskManager);
        setIntField("currentTerm", 1);
        setBooleanField("isCandidate", true);
        when(mockProcess.getTime()).thenReturn(350L, 350L);

        VSMessage heartbeat = new VSMessage();
        heartbeat.setString("type", "heartbeat");
        heartbeat.setInteger("term", 2);
        heartbeat.setInteger("leaderId", 11);

        ArgumentCaptor<VSMessage> messageCaptor =
            ArgumentCaptor.forClass(VSMessage.class);
        ArgumentCaptor<VSTask> taskCaptor = ArgumentCaptor.forClass(VSTask.class);

        protocol.onClientRecv(heartbeat);

        verify(mockProcess).sendMessage(messageCaptor.capture());
        verify(mockTaskManager, times(2)).removeAllTasks(any());
        verify(mockTaskManager).addTask(taskCaptor.capture());

        VSMessage heartbeatAck = messageCaptor.getValue();
        assertEquals("heartbeatAck", heartbeatAck.getString("type"));
        assertEquals(2, heartbeatAck.getInteger("term"));
        assertEquals(7, heartbeatAck.getInteger("pid"));
        assertEquals(11, heartbeatAck.getInteger("targetPid"));
        assertEquals(2, getIntField("currentTerm"));
        assertEquals(11, getIntField("leaderId"));
        assertFalse(getBooleanField("isLeader"));
        assertFalse(getBooleanField("isCandidate"));
        assertEquals(350L, getLongField("lastHeartbeatTime"));
        assertEquals(4850L, taskCaptor.getValue().getTaskTime());
    }

    @Test
    void testVoteResponseMajorityPromotesCandidateToLeader() throws Exception {
        protocol.currentContextIsServer(false);
        setIntField("currentTerm", 3);
        setIntField("votesReceived", 1);
        setBooleanField("isCandidate", true);
        when(mockProcess.getTime()).thenReturn(300L, 300L, 300L);

        VSMessage voteResponse = new VSMessage();
        voteResponse.setString("type", "voteResponse");
        voteResponse.setInteger("term", 3);
        voteResponse.setInteger("pid", 2);
        voteResponse.setBoolean("voteGranted", true);
        voteResponse.setInteger("targetPid", 7);

        ArgumentCaptor<VSMessage> messageCaptor =
            ArgumentCaptor.forClass(VSMessage.class);
        ArgumentCaptor<VSTask> taskCaptor = ArgumentCaptor.forClass(VSTask.class);

        protocol.onClientRecv(voteResponse);

        verify(mockProcess, times(2)).sendMessage(messageCaptor.capture());
        verify(mockTaskManager).removeAllTasks(any());
        verify(mockTaskManager).addTask(taskCaptor.capture());

        VSMessage heartbeat = messageCaptor.getAllValues().get(0);
        assertEquals("heartbeat", heartbeat.getString("type"));
        assertEquals(3, heartbeat.getInteger("term"));
        assertEquals(7, heartbeat.getInteger("leaderId"));
        VSMessage appendEntry = messageCaptor.getAllValues().get(1);
        assertEquals("appendEntry", appendEntry.getString("type"));
        assertEquals(3, appendEntry.getInteger("term"));
        assertEquals(7, appendEntry.getInteger("leaderId"));
        assertEquals("cmd1", appendEntry.getString("entry"));
        assertEquals(1, appendEntry.getInteger("logIndex"));
        assertTrue(getBooleanField("isLeader"));
        assertFalse(getBooleanField("isCandidate"));
        assertEquals(7, getIntField("leaderId"));
        assertTrue(protocol.isServer());
        assertEquals(1800L, taskCaptor.getValue().getTaskTime());
    }

    @Test
    void testVoteResponseForDifferentTargetDoesNotCount() throws Exception {
        protocol.currentContextIsServer(false);
        setIntField("currentTerm", 3);
        setIntField("votesReceived", 1);
        setBooleanField("isCandidate", true);

        VSMessage voteResponse = new VSMessage();
        voteResponse.setString("type", "voteResponse");
        voteResponse.setInteger("term", 3);
        voteResponse.setInteger("pid", 2);
        voteResponse.setBoolean("voteGranted", true);
        voteResponse.setInteger("targetPid", 99);

        protocol.onClientRecv(voteResponse);

        verify(mockProcess, never()).sendMessage(any());
        verify(mockTaskManager, never()).removeAllTasks(any());
        verify(mockTaskManager, never()).addTask(any());
        assertEquals(1, getIntField("votesReceived"));
        assertTrue(getBooleanField("isCandidate"));
        assertFalse(getBooleanField("isLeader"));
    }

    @Test
    void testHigherTermVoteResponseDemotesCandidateToFollower() throws Exception {
        protocol.currentContextIsServer(false);
        protocol.onClientInit();
        clearInvocations(mockProcess, mockTaskManager);
        setIntField("currentTerm", 3);
        setIntField("votesReceived", 2);
        setBooleanField("isCandidate", true);
        when(mockProcess.getTime()).thenReturn(500L, 500L);

        VSMessage voteResponse = new VSMessage();
        voteResponse.setString("type", "voteResponse");
        voteResponse.setInteger("term", 4);
        voteResponse.setInteger("pid", 2);
        voteResponse.setBoolean("voteGranted", false);
        voteResponse.setInteger("targetPid", 7);

        ArgumentCaptor<VSTask> taskCaptor = ArgumentCaptor.forClass(VSTask.class);

        protocol.onClientRecv(voteResponse);

        verify(mockProcess, never()).sendMessage(any());
        verify(mockTaskManager, times(2)).removeAllTasks(any());
        verify(mockTaskManager).addTask(taskCaptor.capture());
        assertEquals(4, getIntField("currentTerm"));
        assertEquals(-1, getIntField("votedFor"));
        assertEquals(0, getIntField("votesReceived"));
        assertFalse(getBooleanField("isCandidate"));
        assertFalse(getBooleanField("isLeader"));
        assertEquals(5000L, taskCaptor.getValue().getTaskTime());
    }

    @Test
    void testServerReceiveHeartbeatAckForLeaderLogsAck() throws Exception {
        protocol.onStart();
        clearInvocations(mockProcess, mockTaskManager);

        VSMessage heartbeatAck = new VSMessage();
        heartbeatAck.setString("type", "heartbeatAck");
        heartbeatAck.setInteger("term", 0);
        heartbeatAck.setInteger("pid", 2);
        heartbeatAck.setInteger("targetPid", 7);

        protocol.onServerRecv(heartbeatAck);

        verify(mockProcess).log("Heartbeat ACK from process 2 received");
        verify(mockTaskManager, never()).removeAllTasks(any());
        verify(mockTaskManager, never()).addTask(any());
    }

    @Test
    void testHeartbeatAckForDifferentTargetDoesNotLog() throws Exception {
        protocol.onStart();
        clearInvocations(mockProcess, mockTaskManager);

        VSMessage heartbeatAck = new VSMessage();
        heartbeatAck.setString("type", "heartbeatAck");
        heartbeatAck.setInteger("term", 0);
        heartbeatAck.setInteger("pid", 2);
        heartbeatAck.setInteger("targetPid", 99);

        protocol.onServerRecv(heartbeatAck);

        verify(mockProcess, never()).log(anyString());
        verify(mockTaskManager, never()).removeAllTasks(any());
        verify(mockTaskManager, never()).addTask(any());
    }

    @Test
    void testDuplicateVoteResponsesFromSamePeerDoNotCreateMajority()
    throws Exception {
        protocol.currentContextIsServer(false);
        when(mockCanvas.getNumProcesses()).thenReturn(5);
        setIntField("currentTerm", 3);
        setIntField("votesReceived", 1);
        setBooleanField("isCandidate", true);

        VSMessage voteResponse = new VSMessage();
        voteResponse.setString("type", "voteResponse");
        voteResponse.setInteger("term", 3);
        voteResponse.setInteger("pid", 2);
        voteResponse.setBoolean("voteGranted", true);
        voteResponse.setInteger("targetPid", 7);

        protocol.onClientRecv(voteResponse);
        protocol.onClientRecv(voteResponse);

        verify(mockProcess, never()).sendMessage(any());
        verify(mockTaskManager, never()).removeAllTasks(any());
        verify(mockTaskManager, never()).addTask(any());
        assertEquals(2, getIntField("votesReceived"));
        assertTrue(getBooleanField("isCandidate"));
        assertFalse(getBooleanField("isLeader"));
    }

    @Test
    void testAppendEntryAcceptedByFollowerSendsAckAndAdvancesLogIndex()
    throws Exception {
        protocol.currentContextIsServer(false);
        protocol.onClientInit();
        clearInvocations(mockProcess, mockTaskManager);
        when(mockProcess.getTime()).thenReturn(600L, 600L);

        VSMessage appendEntry = new VSMessage();
        appendEntry.setString("type", "appendEntry");
        appendEntry.setInteger("term", 2);
        appendEntry.setInteger("leaderId", 11);
        appendEntry.setString("entry", "cmd2");
        appendEntry.setInteger("logIndex", 1);

        ArgumentCaptor<VSMessage> messageCaptor =
            ArgumentCaptor.forClass(VSMessage.class);
        ArgumentCaptor<VSTask> taskCaptor = ArgumentCaptor.forClass(VSTask.class);

        protocol.onClientRecv(appendEntry);

        verify(mockProcess).sendMessage(messageCaptor.capture());
        verify(mockTaskManager, times(2)).removeAllTasks(any());
        verify(mockTaskManager).addTask(taskCaptor.capture());

        VSMessage appendAck = messageCaptor.getValue();
        assertEquals("appendAck", appendAck.getString("type"));
        assertEquals(2, appendAck.getInteger("term"));
        assertEquals(7, appendAck.getInteger("pid"));
        assertEquals(1, appendAck.getInteger("logIndex"));
        assertEquals(11, appendAck.getInteger("targetPid"));
        assertEquals(2, getIntField("currentTerm"));
        assertEquals(11, getIntField("leaderId"));
        assertEquals(1, getIntField("logIndex"));
        assertFalse(getBooleanField("isLeader"));
        assertFalse(getBooleanField("isCandidate"));
        assertEquals(5100L, taskCaptor.getValue().getTaskTime());
    }

    @Test
    void testAppendEntryOutOfSyncDoesNotAdvanceFollowerLogOrSendAck()
    throws Exception {
        protocol.currentContextIsServer(false);
        protocol.onClientInit();
        clearInvocations(mockProcess, mockTaskManager);
        setIntField("currentTerm", 2);
        setIntField("leaderId", 5);
        setBooleanField("isCandidate", true);
        long electionDeadline = getLongField("electionDeadline");
        when(mockProcess.getTime()).thenReturn(600L);

        VSMessage appendEntry = new VSMessage();
        appendEntry.setString("type", "appendEntry");
        appendEntry.setInteger("term", 2);
        appendEntry.setInteger("leaderId", 11);
        appendEntry.setString("entry", "cmd2");
        appendEntry.setInteger("logIndex", 2);

        protocol.onClientRecv(appendEntry);

        verify(mockProcess, never()).sendMessage(any());
        verify(mockTaskManager, never()).removeAllTasks(any());
        verify(mockTaskManager, never()).addTask(any());
        assertEquals(0, getIntField("logIndex"));
        assertEquals(2, getIntField("currentTerm"));
        assertEquals(5, getIntField("leaderId"));
        assertTrue(getBooleanField("isCandidate"));
        assertEquals(electionDeadline, getLongField("electionDeadline"));
    }

    @Test
    void testLeaderAppendQuorumStateDrainsAndCommitsAfterSameTermFollowerRoundTrips()
    throws Exception {
        LeaderHarness leaderHarness = createLeaderHarness(11, 300L);
        leaderHarness.protocol.onStart();

        ArrayList<VSMessage> sentMessages = leaderHarness.protocol.getSentMessages();
        assertEquals(2, sentMessages.size());
        VSMessage appendEntry = sentMessages.get(1);
        ArrayList<Integer> ackPids = getAckPids(leaderHarness.protocol);
        assertEquals(2, ackPids.size());
        assertEquals(1, getIntField(leaderHarness.protocol, "logIndex"));

        protocol.currentContextIsServer(false);
        protocol.onClientInit();
        setIntField("currentTerm", 0);
        clearInvocations(mockProcess, mockTaskManager);
        when(mockProcess.getProcessID()).thenReturn(2);
        when(mockProcess.getTime()).thenReturn(700L, 700L);

        ArgumentCaptor<VSMessage> followerAckCaptor =
            ArgumentCaptor.forClass(VSMessage.class);
        protocol.onClientRecv(appendEntry);
        verify(mockProcess).sendMessage(followerAckCaptor.capture());

        leaderHarness.protocol.onServerRecv(followerAckCaptor.getValue());

        verify(leaderHarness.process, never()).log(anyString());
        assertEquals(1, ackPids.size());
        assertTrue(ackPids.contains(3));
        assertEquals(0, getIntField(leaderHarness.protocol, "commitIndex"));

        protocol.onClientReset();
        protocol.currentContextIsServer(false);
        protocol.onClientInit();
        setIntField("currentTerm", 0);
        clearInvocations(mockProcess, mockTaskManager);
        when(mockProcess.getProcessID()).thenReturn(3);
        when(mockProcess.getTime()).thenReturn(800L, 800L);

        protocol.onClientRecv(appendEntry);
        verify(mockProcess).sendMessage(followerAckCaptor.capture());

        leaderHarness.protocol.onServerRecv(followerAckCaptor.getAllValues().get(1));

        verify(leaderHarness.process).log("Committed log index 1");
        assertTrue(ackPids.isEmpty());
        assertEquals(1, getIntField(leaderHarness.protocol, "commitIndex"));
    }

    @Test
    void testAppendAckWithWrongTermOrLogIndexDoesNotDrainLeaderQuorum()
    throws Exception {
        protocol.onStart();
        clearInvocations(mockProcess, mockTaskManager);

        ArrayList<Integer> ackPids = getAckPids();

        VSMessage wrongTermAck = new VSMessage();
        wrongTermAck.setString("type", "appendAck");
        wrongTermAck.setInteger("term", -1);
        wrongTermAck.setInteger("pid", 2);
        wrongTermAck.setInteger("logIndex", 1);
        wrongTermAck.setInteger("targetPid", 7);

        protocol.onServerRecv(wrongTermAck);

        VSMessage wrongIndexAck = new VSMessage();
        wrongIndexAck.setString("type", "appendAck");
        wrongIndexAck.setInteger("term", 0);
        wrongIndexAck.setInteger("pid", 2);
        wrongIndexAck.setInteger("logIndex", 2);
        wrongIndexAck.setInteger("targetPid", 7);

        protocol.onServerRecv(wrongIndexAck);

        verify(mockProcess, never()).log(anyString());
        assertEquals(2, ackPids.size());
        assertTrue(ackPids.contains(2));
        assertTrue(ackPids.contains(3));
        assertEquals(0, getIntField("commitIndex"));
    }

    private void invokeBecomeFollower(int term, int leaderId) throws Exception {
        Method method = VSRaftProtocol.class.getDeclaredMethod(
            "becomeFollower", int.class, int.class);
        method.setAccessible(true);
        method.invoke(protocol, term, leaderId);
    }

    private void setIntField(String fieldName, int value) throws Exception {
        Field field = VSRaftProtocol.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.setInt(protocol, value);
    }

    private void setBooleanField(String fieldName, boolean value)
    throws Exception {
        Field field = VSRaftProtocol.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.setBoolean(protocol, value);
    }

    private int getIntField(String fieldName) throws Exception {
        Field field = VSRaftProtocol.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.getInt(protocol);
    }

    private Object getObjectField(String fieldName) throws Exception {
        Field field = VSRaftProtocol.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(protocol);
    }

    @SuppressWarnings("unchecked")
    private ArrayList<Integer> getAckPids() throws Exception {
        return (ArrayList<Integer>) getObjectField("ackPids");
    }

    private boolean getBooleanField(String fieldName) throws Exception {
        Field field = VSRaftProtocol.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.getBoolean(protocol);
    }

    private long getLongField(String fieldName) throws Exception {
        Field field = VSRaftProtocol.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.getLong(protocol);
    }

    private LeaderHarness createLeaderHarness(int pid, long time) {
        CapturingRaftProtocol peerProtocol = new CapturingRaftProtocol();
        VSInternalProcess peerProcess = mock(VSInternalProcess.class);
        VSSimulatorVisualization peerCanvas = mock(VSSimulatorVisualization.class);
        VSTaskManager peerTaskManager = mock(VSTaskManager.class);
        VSPrefs peerPrefs = mock(VSPrefs.class);
        VSVectorTime peerVectorTime = mock(VSVectorTime.class);

        peerProtocol.process = peerProcess;
        peerProtocol.prefs = peerPrefs;
        peerProtocol.isServer(true);
        peerProtocol.currentContextIsServer(true);

        when(peerProcess.getSimulatorCanvas()).thenReturn(peerCanvas);
        when(peerCanvas.getTaskManager()).thenReturn(peerTaskManager);
        when(peerCanvas.getNumProcesses()).thenReturn(3);
        when(peerProcess.getPrefs()).thenReturn(peerPrefs);
        when(peerProcess.getVectorTime()).thenReturn(peerVectorTime);
        when(peerVectorTime.getCopy()).thenReturn(peerVectorTime);
        when(peerPrefs.getString(anyString())).thenReturn("TestString");
        when(peerProcess.getTime()).thenReturn(time);
        when(peerProcess.getProcessID()).thenReturn(pid);
        when(peerProcess.getRandomPercentage()).thenReturn(25);

        peerProtocol.onServerInit();
        clearInvocations(peerProcess, peerTaskManager);
        return new LeaderHarness(peerProtocol, peerProcess);
    }

    private static final class CapturingRaftProtocol extends VSRaftProtocol {
        private ArrayList<VSMessage> sentMessages = new ArrayList<VSMessage>();

        @Override
        public void sendMessage(VSMessage message) {
            sentMessages.add(message);
        }

        private ArrayList<VSMessage> getSentMessages() {
            return sentMessages;
        }
    }

    private int getIntField(VSRaftProtocol raftProtocol, String fieldName)
    throws Exception {
        Field field = VSRaftProtocol.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.getInt(raftProtocol);
    }

    @SuppressWarnings("unchecked")
    private ArrayList<Integer> getAckPids(VSRaftProtocol raftProtocol)
    throws Exception {
        Field field = VSRaftProtocol.class.getDeclaredField("ackPids");
        field.setAccessible(true);
        return (ArrayList<Integer>) field.get(raftProtocol);
    }

    private record LeaderHarness(
        CapturingRaftProtocol protocol,
        VSInternalProcess process
    ) {
    }
}
