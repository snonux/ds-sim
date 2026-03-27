package protocols;

import core.VSInternalProcess;
import core.VSMessage;
import core.VSMessageStub;
import core.VSTask;
import events.VSAbstractEvent;
import events.internal.VSProtocolScheduleEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import prefs.VSPrefs;
import simulator.VSSimulatorVisualization;
import core.VSTaskManager;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for VSAbstractProtocol.
 */
class VSAbstractProtocolTest {
    
    @Mock
    private VSInternalProcess mockProcess;
    
    @Mock
    private VSSimulatorVisualization mockCanvas;
    
    @Mock
    private VSTaskManager mockTaskManager;
    
    @Mock
    private VSMessage mockMessage;
    
    @Mock
    private VSMessageStub mockMessageStub;
    
    @Mock
    private VSPrefs mockPrefs;
    
    @Mock
    private ObjectOutputStream mockOutputStream;
    
    @Mock
    private ObjectInputStream mockInputStream;
    
    private TestProtocol testProtocol;
    
    /**
     * Test implementation of VSAbstractProtocol.
     */
    private static class TestProtocol extends VSAbstractProtocol {
        boolean clientInitCalled = false;
        boolean serverInitCalled = false;
        boolean clientResetCalled = false;
        boolean serverResetCalled = false;
        boolean clientScheduleCalled = false;
        boolean serverScheduleCalled = false;
        boolean clientRecvCalled = false;
        boolean serverRecvCalled = false;
        VSMessage lastClientMessage = null;
        VSMessage lastServerMessage = null;
        
        TestProtocol(boolean hasOnServerStart) {
            super(hasOnServerStart);
            setClassname("protocols.VSAbstractProtocolTest$TestProtocol");
        }
        
        @Override
        public void onClientInit() {
            clientInitCalled = true;
        }
        
        @Override
        public void onClientReset() {
            clientResetCalled = true;
        }
        
        @Override
        public void onClientSchedule() {
            clientScheduleCalled = true;
        }
        
        @Override
        public void onClientRecv(VSMessage message) {
            clientRecvCalled = true;
            lastClientMessage = message;
        }
        
        @Override
        public void onServerInit() {
            serverInitCalled = true;
        }
        
        @Override
        public void onServerReset() {
            serverResetCalled = true;
        }
        
        @Override
        public void onServerRecv(VSMessage message) {
            serverRecvCalled = true;
            lastServerMessage = message;
        }
        
        @Override
        public void onServerSchedule() {
            serverScheduleCalled = true;
        }
    }
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        testProtocol = new TestProtocol(true);
        
        // Setup mock chain
        when(mockProcess.getSimulatorCanvas()).thenReturn(mockCanvas);
        when(mockCanvas.getTaskManager()).thenReturn(mockTaskManager);
        when(mockCanvas.getNumProcesses()).thenReturn(5);
        when(mockPrefs.getString(anyString())).thenReturn("TestString");
        
        testProtocol.prefs = mockPrefs;
    }
    
    @Test
    void testConstructor() {
        TestProtocol serverStartProtocol = new TestProtocol(true);
        assertTrue(serverStartProtocol.hasOnServerStart());
        
        TestProtocol clientStartProtocol = new TestProtocol(false);
        assertFalse(clientStartProtocol.hasOnServerStart());
    }
    
    @Test
    void testServerClientFlags() {
        assertFalse(testProtocol.isServer());
        assertFalse(testProtocol.isClient());
        
        testProtocol.isServer(true);
        assertTrue(testProtocol.isServer());
        
        testProtocol.isClient(true);
        assertTrue(testProtocol.isClient());
    }
    
    @Test
    void testSendMessage() {
        testProtocol.process = mockProcess;
        testProtocol.currentContextIsServer(true);
        
        testProtocol.sendMessage(mockMessage);
        
        verify(mockProcess).increaseLamportTime();
        verify(mockProcess).increaseVectorTime();
        verify(mockProcess).sendMessage(mockMessage);
    }
    
    @Test
    void testSendMessageWithNullProcess() {
        testProtocol.process = null;
        
        // Should not throw exception
        assertDoesNotThrow(() -> testProtocol.sendMessage(mockMessage));
        
        // Should not interact with anything
        verifyNoInteractions(mockProcess);
    }
    
    @Test
    void testOnStartWithServerMode() {
        testProtocol = new TestProtocol(true); // Has server start
        testProtocol.process = mockProcess;
        testProtocol.isServer(true);
        testProtocol.prefs = mockPrefs;
        
        testProtocol.onStart();
        
        assertTrue(testProtocol.serverInitCalled);
        assertFalse(testProtocol.clientInitCalled);
    }
    
    @Test
    void testOnStartWithClientMode() {
        testProtocol = new TestProtocol(false); // Has client start
        testProtocol.process = mockProcess;
        testProtocol.isClient(true);
        testProtocol.prefs = mockPrefs;
        
        testProtocol.onStart();
        
        assertTrue(testProtocol.clientInitCalled);
        assertFalse(testProtocol.serverInitCalled);
    }
    
    @Test
    void testOnInit() {
        testProtocol.process = mockProcess;
        testProtocol.isServer(true);
        testProtocol.isClient(true);
        
        testProtocol.onInit();
        
        assertTrue(testProtocol.serverInitCalled);
        assertTrue(testProtocol.clientInitCalled);
    }
    
    @Test
    void testOnMessageRecvWithCorrectProtocol() {
        when(mockMessage.getProtocolClassname()).thenReturn(testProtocol.getClassname());
        when(mockMessage.isServerMessage()).thenReturn(false); // Client message
        
        testProtocol.process = mockProcess;
        testProtocol.isServer(true);
        
        testProtocol.onMessageRecvStart(mockMessage);
        
        assertTrue(testProtocol.serverRecvCalled);
        assertEquals(mockMessage, testProtocol.lastServerMessage);
    }
    
    @Test
    void testOnMessageRecvWithIncorrectProtocol() {
        when(mockMessage.getProtocolClassname()).thenReturn("DifferentProtocol");
        
        testProtocol.process = mockProcess;
        testProtocol.isServer(true);
        
        testProtocol.onMessageRecvStart(mockMessage);
        
        assertFalse(testProtocol.serverRecvCalled);
        assertNull(testProtocol.lastServerMessage);
    }
    
    @Test
    void testIsRelevantMessage() {
        when(mockMessage.getProtocolClassname()).thenReturn(testProtocol.getClassname());
        
        // Server receiving client message - should be relevant
        when(mockMessage.isServerMessage()).thenReturn(false);
        testProtocol.isServer(true);
        testProtocol.isClient(false);
        assertTrue(testProtocol.isRelevantMessage(mockMessage));
        
        // Client receiving server message - should be relevant
        when(mockMessage.isServerMessage()).thenReturn(true);
        testProtocol.isServer(false);
        testProtocol.isClient(true);
        assertTrue(testProtocol.isRelevantMessage(mockMessage));
        
        // Server receiving server message - should NOT be relevant
        when(mockMessage.isServerMessage()).thenReturn(true);
        testProtocol.isServer(true);
        testProtocol.isClient(false);
        assertFalse(testProtocol.isRelevantMessage(mockMessage));
    }
    
    @Test
    void testScheduleAt() {
        testProtocol.process = mockProcess;
        testProtocol.currentContextIsServer(true);
        when(mockProcess.getPrefs()).thenReturn(mockPrefs);
        when(mockPrefs.getString("lang.server")).thenReturn("Server");
        when(mockPrefs.getString("lang.client")).thenReturn("Client");
        when(mockPrefs.getString("lang.events.internal.VSProtocolScheduleEvent.short"))
            .thenReturn("Protocol Schedule");
        
        long scheduleTime = 100L;
        testProtocol.scheduleAt(scheduleTime);
        
        ArgumentCaptor<VSTask> taskCaptor = ArgumentCaptor.forClass(VSTask.class);
        verify(mockTaskManager).addTask(taskCaptor.capture());
        
        VSTask capturedTask = taskCaptor.getValue();
        assertNotNull(capturedTask);
        assertEquals(mockProcess, capturedTask.getProcess());
        assertInstanceOf(VSProtocolScheduleEvent.class, capturedTask.getEvent());
        assertEquals("events.internal.VSProtocolScheduleEvent",
                     capturedTask.getEvent().getClassname());
        assertEquals("protocols.VSAbstractProtocolTest$TestProtocol "
                     + "Server Protocol Schedule",
                     capturedTask.getEvent().getShortname());
    }
    
    @Test
    void testRemoveSchedules() {
        testProtocol.process = mockProcess;
        testProtocol.currentContextIsServer(true);
        
        // First add a schedule
        testProtocol.scheduleAt(100L);
        
        // Then remove it
        testProtocol.removeSchedules();
        
        verify(mockTaskManager).removeAllTasks(any());
    }
    
    @Test
    void testReset() {
        testProtocol.isServer(true);
        testProtocol.isClient(true);
        
        testProtocol.reset();
        
        assertFalse(testProtocol.isServer());
        assertFalse(testProtocol.isClient());
        assertTrue(testProtocol.serverResetCalled);
        assertTrue(testProtocol.clientResetCalled);
    }
    
    @Test
    void testGetNumProcesses() {
        testProtocol.process = mockProcess;
        
        assertEquals(5, testProtocol.getNumProcesses());
        
        testProtocol.process = null;
        assertEquals(0, testProtocol.getNumProcesses());
    }
    
    @Test
    void testToString() {
        testProtocol.process = mockProcess;
        testProtocol.currentContextIsServer(true);
        
        String result = testProtocol.toString();
        
        assertNotNull(result);
        assertTrue(result.contains("TestString"));
        
        // Test with null process
        testProtocol.process = null;
        assertEquals("", testProtocol.toString());
    }
    
    @Test
    void testOnClientScheduleStart() {
        testProtocol.isClient(true);
        
        testProtocol.onClientScheduleStart();
        
        assertTrue(testProtocol.clientScheduleCalled);
        assertFalse(testProtocol.serverScheduleCalled);
    }
    
    @Test
    void testOnServerScheduleStart() {
        testProtocol.isServer(true);
        
        testProtocol.onServerScheduleStart();
        
        assertTrue(testProtocol.serverScheduleCalled);
        assertFalse(testProtocol.clientScheduleCalled);
    }
    
    @Test
    void testSerialization() throws IOException, ClassNotFoundException {
        testProtocol.serialize(null, mockOutputStream);
        
        // Verify that writeObject was called multiple times (parent classes also serialize)
        verify(mockOutputStream, atLeast(2)).writeObject(Boolean.FALSE);
        verify(mockOutputStream, atLeast(1)).writeObject(Boolean.TRUE); // hasOnServerStart
    }
    
}
