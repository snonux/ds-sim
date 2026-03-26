package protocols.implementations;

import core.VSInternalProcess;
import core.VSMessage;
import core.VSTask;
import core.VSTaskManager;
import core.time.VSVectorTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import prefs.VSPrefs;
import simulator.VSSimulatorVisualization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
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

        verify(mockProcess, never()).sendMessage(org.mockito.ArgumentMatchers.any());
        verify(mockTaskManager, never()).addTask(org.mockito.ArgumentMatchers.any());
    }
}
