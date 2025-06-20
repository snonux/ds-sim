package protocols.implementations;

import core.VSInternalProcess;
import core.VSMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import prefs.VSPrefs;
import simulator.VSSimulatorVisualization;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for VSPingPongProtocol.
 */
class VSPingPongProtocolTest {
    
    @Mock
    private VSInternalProcess mockProcess;
    
    @Mock
    private VSSimulatorVisualization mockCanvas;
    
    @Mock
    private VSPrefs mockPrefs;
    
    private VSPingPongProtocol protocol;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        protocol = new VSPingPongProtocol();
        protocol.process = mockProcess;
        protocol.prefs = mockPrefs;
        
        // Setup mock chain
        when(mockProcess.getSimulatorCanvas()).thenReturn(mockCanvas);
        when(mockPrefs.getString(anyString())).thenReturn("TestString");
    }
    
    @Test
    void testConstructor() {
        VSPingPongProtocol newProtocol = new VSPingPongProtocol();
        assertFalse(newProtocol.hasOnServerStart()); // Should use client start
        assertNotNull(newProtocol.getClassname());
        assertTrue(newProtocol.getClassname().contains("VSPingPongProtocol"));
    }
    
    @Test
    void testClientInit() {
        // Should do nothing but not throw exception
        assertDoesNotThrow(() -> protocol.onClientInit());
    }
    
    @Test
    void testServerInit() {
        // Should do nothing but not throw exception
        assertDoesNotThrow(() -> protocol.onServerInit());
    }
    
    @Test
    void testClientReset() {
        // First send some messages to increment counter
        protocol.isClient(true);
        protocol.onClientStart();
        protocol.onClientStart();
        
        // Reset should clear counter
        protocol.onClientReset();
        
        // Verify counter was reset by checking next message
        ArgumentCaptor<VSMessage> messageCaptor = ArgumentCaptor.forClass(VSMessage.class);
        protocol.onClientStart();
        
        verify(mockProcess, atLeast(1)).sendMessage(messageCaptor.capture());
        VSMessage lastMessage = messageCaptor.getValue();
        assertEquals(1, lastMessage.getInteger("counter"));
    }
    
    @Test
    void testServerReset() {
        // Set up server mode
        protocol.isServer(true);
        protocol.currentContextIsServer(true);
        
        // Create a mock client message
        VSMessage clientMessage = new VSMessage();
        clientMessage.setBoolean("fromClient", true);
        clientMessage.setInteger("counter", 1);
        
        // Process some messages to increment counter
        protocol.onServerRecv(clientMessage);
        protocol.onServerRecv(clientMessage);
        
        // Reset should clear counter
        protocol.onServerReset();
        
        // Verify counter was reset by checking next message
        ArgumentCaptor<VSMessage> messageCaptor = ArgumentCaptor.forClass(VSMessage.class);
        protocol.onServerRecv(clientMessage);
        
        verify(mockProcess, atLeast(1)).sendMessage(messageCaptor.capture());
        VSMessage lastMessage = messageCaptor.getValue();
        assertEquals(1, lastMessage.getInteger("counter"));
    }
    
    @Test
    void testClientStart() {
        protocol.isClient(true);
        
        ArgumentCaptor<VSMessage> messageCaptor = ArgumentCaptor.forClass(VSMessage.class);
        
        protocol.onClientStart();
        
        verify(mockProcess).sendMessage(messageCaptor.capture());
        
        VSMessage sentMessage = messageCaptor.getValue();
        assertTrue(sentMessage.getBoolean("fromClient"));
        assertEquals(1, sentMessage.getInteger("counter"));
        
        // Test counter increment
        protocol.onClientStart();
        
        verify(mockProcess, times(2)).sendMessage(messageCaptor.capture());
        sentMessage = messageCaptor.getValue();
        assertEquals(2, sentMessage.getInteger("counter"));
    }
    
    @Test
    void testClientRecv() {
        protocol.isClient(true);
        
        // Create a valid server message
        VSMessage serverMessage = new VSMessage();
        serverMessage.setBoolean("fromServer", true);
        serverMessage.setInteger("counter", 5);
        
        ArgumentCaptor<VSMessage> messageCaptor = ArgumentCaptor.forClass(VSMessage.class);
        
        protocol.onClientRecv(serverMessage);
        
        verify(mockProcess).sendMessage(messageCaptor.capture());
        
        VSMessage sentMessage = messageCaptor.getValue();
        assertTrue(sentMessage.getBoolean("fromClient"));
        assertEquals(1, sentMessage.getInteger("counter"));
    }
    
    @Test
    void testClientRecvIgnoresNonServerMessage() {
        protocol.isClient(true);
        
        // Create an invalid message (not from server)
        VSMessage invalidMessage = new VSMessage();
        invalidMessage.setBoolean("fromServer", false);
        invalidMessage.setInteger("counter", 5);
        
        protocol.onClientRecv(invalidMessage);
        
        // Should not send any message
        verify(mockProcess, never()).sendMessage(any());
    }
    
    @Test
    void testServerRecv() {
        protocol.isServer(true);
        protocol.currentContextIsServer(true);
        
        // Create a valid client message
        VSMessage clientMessage = new VSMessage();
        clientMessage.setBoolean("fromClient", true);
        clientMessage.setInteger("counter", 3);
        
        ArgumentCaptor<VSMessage> messageCaptor = ArgumentCaptor.forClass(VSMessage.class);
        
        protocol.onServerRecv(clientMessage);
        
        verify(mockProcess).sendMessage(messageCaptor.capture());
        
        VSMessage sentMessage = messageCaptor.getValue();
        assertTrue(sentMessage.getBoolean("fromServer"));
        assertEquals(1, sentMessage.getInteger("counter"));
    }
    
    @Test
    void testServerRecvIgnoresNonClientMessage() {
        protocol.isServer(true);
        protocol.currentContextIsServer(true);
        
        // Create an invalid message (not from client)
        VSMessage invalidMessage = new VSMessage();
        invalidMessage.setBoolean("fromClient", false);
        invalidMessage.setInteger("counter", 5);
        
        protocol.onServerRecv(invalidMessage);
        
        // Should not send any message
        verify(mockProcess, never()).sendMessage(any());
    }
    
    @Test
    void testClientSchedule() {
        // Should do nothing but not throw exception
        assertDoesNotThrow(() -> protocol.onClientSchedule());
    }
    
    @Test
    void testServerSchedule() {
        // Should do nothing but not throw exception
        assertDoesNotThrow(() -> protocol.onServerSchedule());
    }
    
    @Test
    void testToString() {
        String result = protocol.toString();
        
        assertNotNull(result);
        assertTrue(result.contains("New message afterwards"));
        assertTrue(result.contains("TestString"));
    }
    
    @Test
    void testPingPongInteraction() {
        // Test a full ping-pong interaction
        protocol.isClient(true);
        protocol.isServer(true);
        
        ArgumentCaptor<VSMessage> messageCaptor = ArgumentCaptor.forClass(VSMessage.class);
        
        // Client starts the ping
        protocol.currentContextIsServer(false);
        protocol.onClientStart();
        
        verify(mockProcess, times(1)).sendMessage(messageCaptor.capture());
        VSMessage ping = messageCaptor.getValue();
        assertTrue(ping.getBoolean("fromClient"));
        assertEquals(1, ping.getInteger("counter"));
        
        // Server receives ping and sends pong
        protocol.currentContextIsServer(true);
        protocol.onServerRecv(ping);
        
        verify(mockProcess, times(2)).sendMessage(messageCaptor.capture());
        VSMessage pong = messageCaptor.getValue();
        assertTrue(pong.getBoolean("fromServer"));
        assertEquals(1, pong.getInteger("counter"));
        
        // Client receives pong and sends another ping
        protocol.currentContextIsServer(false);
        protocol.onClientRecv(pong);
        
        verify(mockProcess, times(3)).sendMessage(messageCaptor.capture());
        VSMessage ping2 = messageCaptor.getValue();
        assertTrue(ping2.getBoolean("fromClient"));
        assertEquals(2, ping2.getInteger("counter"));
    }
}