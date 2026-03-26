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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for VSRaftProtocol heartbeat behavior.
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
        when(mockProcess.getPrefs()).thenReturn(mockPrefs);
        when(mockProcess.getVectorTime()).thenReturn(mockVectorTime);
        when(mockVectorTime.getCopy()).thenReturn(mockVectorTime);
        when(mockPrefs.getString(anyString())).thenReturn("TestString");
        when(mockProcess.getTime()).thenReturn(100L);
        when(mockProcess.getProcessID()).thenReturn(7);
        when(mockProcess.getRandomPercentage()).thenReturn(25);
    }

    @Test
    void testOnStartBecomesLeaderAndSendsHeartbeat() {
        ArgumentCaptor<VSMessage> messageCaptor =
            ArgumentCaptor.forClass(VSMessage.class);
        ArgumentCaptor<VSTask> taskCaptor = ArgumentCaptor.forClass(VSTask.class);

        protocol.onStart();

        verify(mockProcess).sendMessage(messageCaptor.capture());
        verify(mockTaskManager).addTask(taskCaptor.capture());

        VSMessage heartbeat = messageCaptor.getValue();
        assertEquals("heartbeat", heartbeat.getString("type"));
        assertEquals(0, heartbeat.getInteger("term"));
        assertEquals(7, heartbeat.getInteger("leaderId"));
        assertEquals(1600L, taskCaptor.getValue().getTaskTime());
    }

    @Test
    void testServerScheduleSendsHeartbeatWhenLeader() {
        ArgumentCaptor<VSMessage> messageCaptor =
            ArgumentCaptor.forClass(VSMessage.class);
        ArgumentCaptor<VSTask> taskCaptor = ArgumentCaptor.forClass(VSTask.class);

        protocol.onStart();
        protocol.onServerScheduleStart();

        verify(mockProcess, times(2)).sendMessage(messageCaptor.capture());
        verify(mockTaskManager, times(2)).addTask(taskCaptor.capture());

        assertEquals(2, messageCaptor.getAllValues().size());
        assertEquals(2, taskCaptor.getAllValues().size());

        VSMessage scheduledHeartbeat = messageCaptor.getAllValues().get(1);
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

    private void invokeBecomeFollower(int term, int leaderId) throws Exception {
        Method method = VSRaftProtocol.class.getDeclaredMethod(
            "becomeFollower", int.class, int.class);
        method.setAccessible(true);
        method.invoke(protocol, term, leaderId);
    }

    private int getIntField(String fieldName) throws Exception {
        Field field = VSRaftProtocol.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.getInt(protocol);
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
}
