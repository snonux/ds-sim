package core;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import core.time.VSVectorTime;
import events.VSRegisteredEvents;
import prefs.VSPrefs;

/**
 * Unit tests for VSMessage class.
 * Tests message creation, initialization, and various getter/setter functionality.
 * 
 * @author Test Suite
 */
class VSMessageTest {
    
    @Mock
    private VSInternalProcess mockSendingProcess;
    
    @Mock
    private VSPrefs mockPrefs;
    
    @Mock
    private VSVectorTime mockVectorTime;
    
    @Mock
    private VSVectorTime mockVectorTimeCopy;
    
    private VSMessage message;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Setup common mocks
        when(mockSendingProcess.getPrefs()).thenReturn(mockPrefs);
        when(mockSendingProcess.getLamportTime()).thenReturn(42L);
        when(mockSendingProcess.getVectorTime()).thenReturn(mockVectorTime);
        when(mockVectorTime.getCopy()).thenReturn(mockVectorTimeCopy);
        
        when(mockPrefs.getString("lang.protocol")).thenReturn("Protocol");
        
        message = new VSMessage();
    }
    
    @Test
    @DisplayName("Test VSMessage constructor creates unique message IDs")
    void testConstructorCreatesUniqueIds() {
        // Given/When
        VSMessage message1 = new VSMessage();
        VSMessage message2 = new VSMessage();
        VSMessage message3 = new VSMessage();
        
        // Then
        assertNotEquals(message1.getMessageID(), message2.getMessageID());
        assertNotEquals(message2.getMessageID(), message3.getMessageID());
        assertNotEquals(message1.getMessageID(), message3.getMessageID());
        assertTrue(message2.getMessageID() > message1.getMessageID());
        assertTrue(message3.getMessageID() > message2.getMessageID());
    }
    
    @Test
    @DisplayName("Test init method for server message")
    void testInitServerMessage() {
        // Given
        String protocolClassname = "test.protocol.MyProtocol";
        
        // When
        message.init(mockSendingProcess, protocolClassname, VSMessage.IS_SERVER_MESSAGE);
        
        // Then
        assertEquals(mockSendingProcess, message.getSendingProcess());
        assertEquals(protocolClassname, message.getProtocolClassname());
        assertTrue(message.isServerMessage());
        assertEquals(42L, message.getLamportTime());
        assertEquals(mockVectorTimeCopy, message.getVectorTime());
    }
    
    @Test
    @DisplayName("Test init method for client message")
    void testInitClientMessage() {
        // Given
        String protocolClassname = "test.protocol.ClientProtocol";
        
        // When
        message.init(mockSendingProcess, protocolClassname, VSMessage.IS_CLIENT_MESSAGE);
        
        // Then
        assertEquals(mockSendingProcess, message.getSendingProcess());
        assertEquals(protocolClassname, message.getProtocolClassname());
        assertFalse(message.isServerMessage());
    }
    
    @Test
    @DisplayName("Test getName method")
    void testGetName() {
        // Given
        String protocolClassname = "test.protocol.MyProtocol";
        String expectedName = "My Protocol";
        message.init(mockSendingProcess, protocolClassname, VSMessage.IS_SERVER_MESSAGE);
        
        // Mock static method behavior
        try (var mockedStatic = mockStatic(VSRegisteredEvents.class)) {
            mockedStatic.when(() -> VSRegisteredEvents.getNameByClassname(protocolClassname))
                        .thenReturn(expectedName);
            
            // When
            String name = message.getName();
            
            // Then
            assertEquals(expectedName, name);
        }
    }
    
    @Test
    @DisplayName("Test getMessageID returns correct ID")
    void testGetMessageID() {
        // Given - message created in setUp
        
        // When
        long messageId = message.getMessageID();
        
        // Then
        assertTrue(messageId > 0);
    }
    
    @Test
    @DisplayName("Test getSendingProcess returns correct process")
    void testGetSendingProcess() {
        // Given
        message.init(mockSendingProcess, "test.Protocol", VSMessage.IS_SERVER_MESSAGE);
        
        // When
        VSAbstractProcess process = message.getSendingProcess();
        
        // Then
        assertEquals(mockSendingProcess, process);
    }
    
    @Test
    @DisplayName("Test getLamportTime returns correct time")
    void testGetLamportTime() {
        // Given
        when(mockSendingProcess.getLamportTime()).thenReturn(100L);
        message.init(mockSendingProcess, "test.Protocol", VSMessage.IS_SERVER_MESSAGE);
        
        // When
        long lamportTime = message.getLamportTime();
        
        // Then
        assertEquals(100L, lamportTime);
    }
    
    @Test
    @DisplayName("Test getVectorTime returns correct vector time")
    void testGetVectorTime() {
        // Given
        message.init(mockSendingProcess, "test.Protocol", VSMessage.IS_SERVER_MESSAGE);
        
        // When
        VSVectorTime vectorTime = message.getVectorTime();
        
        // Then
        assertEquals(mockVectorTimeCopy, vectorTime);
    }
    
    @Test
    @DisplayName("Test toString method")
    void testToString() {
        // Given
        String protocolClassname = "test.protocol.MyProtocol";
        String shortname = "MP";
        message.init(mockSendingProcess, protocolClassname, VSMessage.IS_SERVER_MESSAGE);
        
        try (var mockedStatic = mockStatic(VSRegisteredEvents.class)) {
            mockedStatic.when(() -> VSRegisteredEvents.getShortnameByClassname(protocolClassname))
                        .thenReturn(shortname);
            
            // When
            String result = message.toString();
            
            // Then
            assertTrue(result.contains("ID: " + message.getMessageID()));
            assertTrue(result.contains("Protocol"));
            assertTrue(result.contains(shortname));
        }
    }
    
    @Test
    @DisplayName("Test toStringFull method includes parent toString")
    void testToStringFull() {
        // Given
        String protocolClassname = "test.protocol.MyProtocol";
        message.init(mockSendingProcess, protocolClassname, VSMessage.IS_SERVER_MESSAGE);
        
        // Setup some preferences in the message
        message.setInteger("test.value", 123);
        
        // When
        String result = message.toStringFull();
        
        // Then
        assertTrue(result.contains("ID: " + message.getMessageID()));
        assertTrue(result.contains("test.value"));
        assertTrue(result.contains("123"));
    }
    
    @Test
    @DisplayName("Test equals method with same message ID")
    void testEqualsWithSameId() {
        // Given
        VSMessage message1 = new VSMessage();
        VSMessage message2 = message1; // Same reference
        
        // When/Then
        assertTrue(message1.equals(message2));
    }
    
    @Test
    @DisplayName("Test equals method with different message IDs")
    void testEqualsWithDifferentIds() {
        // Given
        VSMessage message1 = new VSMessage();
        VSMessage message2 = new VSMessage();
        
        // When/Then
        assertFalse(message1.equals(message2));
    }
    
    @Test
    @DisplayName("Test message inherits VSPrefs functionality")
    void testPrefsInheritance() {
        // Given
        message.init(mockSendingProcess, "test.Protocol", VSMessage.IS_SERVER_MESSAGE);
        
        // When - Test some VSPrefs methods
        message.setInteger("test.int", 42);
        message.setString("test.string", "Hello");
        message.setBoolean("test.bool", true);
        
        // Then
        assertEquals(42, message.getInteger("test.int"));
        assertEquals("Hello", message.getString("test.string"));
        assertTrue(message.getBoolean("test.bool"));
    }
    
    @Test
    @DisplayName("Test message initialization captures timestamps at send time")
    void testTimestampsCapturedAtInit() {
        // Given
        when(mockSendingProcess.getLamportTime()).thenReturn(10L);
        VSVectorTime firstVectorTime = mock(VSVectorTime.class);
        VSVectorTime firstCopy = mock(VSVectorTime.class);
        when(mockSendingProcess.getVectorTime()).thenReturn(firstVectorTime);
        when(firstVectorTime.getCopy()).thenReturn(firstCopy);
        
        // When - Initialize message
        message.init(mockSendingProcess, "test.Protocol", VSMessage.IS_SERVER_MESSAGE);
        
        // Change process times after initialization
        when(mockSendingProcess.getLamportTime()).thenReturn(20L);
        VSVectorTime secondVectorTime = mock(VSVectorTime.class);
        when(mockSendingProcess.getVectorTime()).thenReturn(secondVectorTime);
        
        // Then - Message should retain original timestamps
        assertEquals(10L, message.getLamportTime());
        assertEquals(firstCopy, message.getVectorTime());
    }
    
    @Test
    @DisplayName("Test server message flag")
    void testServerMessageFlag() {
        // Given/When - Server message
        message.init(mockSendingProcess, "test.Protocol", VSMessage.IS_SERVER_MESSAGE);
        
        // Then
        assertTrue(message.isServerMessage());
        
        // Given/When - Client message
        VSMessage clientMessage = new VSMessage();
        clientMessage.init(mockSendingProcess, "test.Protocol", VSMessage.IS_CLIENT_MESSAGE);
        
        // Then
        assertFalse(clientMessage.isServerMessage());
    }
    
    @Test
    @DisplayName("Test multiple messages can be created with different protocols")
    void testMultipleMessagesWithDifferentProtocols() {
        // Given
        VSMessage message1 = new VSMessage();
        VSMessage message2 = new VSMessage();
        VSMessage message3 = new VSMessage();
        
        // When
        message1.init(mockSendingProcess, "protocol.One", VSMessage.IS_SERVER_MESSAGE);
        message2.init(mockSendingProcess, "protocol.Two", VSMessage.IS_CLIENT_MESSAGE);
        message3.init(mockSendingProcess, "protocol.Three", VSMessage.IS_SERVER_MESSAGE);
        
        // Then
        assertEquals("protocol.One", message1.getProtocolClassname());
        assertEquals("protocol.Two", message2.getProtocolClassname());
        assertEquals("protocol.Three", message3.getProtocolClassname());
        assertTrue(message1.isServerMessage());
        assertFalse(message2.isServerMessage());
        assertTrue(message3.isServerMessage());
    }
}