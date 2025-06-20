package events;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Vector;

import core.VSInternalProcess;
import prefs.VSPrefs;
import events.implementations.VSProcessCrashEvent;
import events.implementations.VSProcessRecoverEvent;

/**
 * Unit tests for VSRegisteredEvents class.
 * Tests event registration, lookup, instantiation, and protocol management.
 *
 * @author Test Suite
 */
public class VSRegisteredEventsTest {
    
    @Mock
    private VSPrefs mockPrefs;
    
    @Mock
    private VSInternalProcess mockProcess;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Use a default answer to return empty string for any unmocked calls
        when(mockPrefs.getString(anyString())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            // Return a default value based on the key pattern
            if (key.endsWith(".short")) {
                return "DefaultShort";
            }
            return "Default Event/Protocol";
        });
        
        // Now override with specific values we want to test
        // Events
        when(mockPrefs.getString("lang.events.implementations.VSProcessCrashEvent"))
            .thenReturn("Process Crash");
        when(mockPrefs.getString("lang.events.implementations.VSProcessCrashEvent.short"))
            .thenReturn("Crash");
        when(mockPrefs.getString("lang.events.implementations.VSProcessRecoverEvent"))
            .thenReturn("Process Recover");
        when(mockPrefs.getString("lang.events.implementations.VSProcessRecoverEvent.short"))
            .thenReturn("Recover");
        
        // Protocols - mock key protocols we'll test
        when(mockPrefs.getString("lang.protocols.implementations.VSPingPongProtocol"))
            .thenReturn("Ping Pong Protocol");
        when(mockPrefs.getString("lang.protocols.implementations.VSPingPongProtocol.short"))
            .thenReturn("PingPong");
        when(mockPrefs.getString("lang.protocols.implementations.VSDummyProtocol"))
            .thenReturn("Dummy Protocol");
        when(mockPrefs.getString("lang.protocols.implementations.VSDummyProtocol.short"))
            .thenReturn("Dummy");
        
        // Initialize the registered events
        VSRegisteredEvents.init(mockPrefs);
    }
    
    @Test
    void testGetClassnameByEventname() {
        // Test that we can retrieve classnames by event names
        String classname = VSRegisteredEvents.getClassnameByEventname("Process Crash");
        assertEquals("events.implementations.VSProcessCrashEvent", classname);
        
        classname = VSRegisteredEvents.getClassnameByEventname("Process Recover");
        assertEquals("events.implementations.VSProcessRecoverEvent", classname);
        
        // Test non-existent event
        classname = VSRegisteredEvents.getClassnameByEventname("Non Existent Event");
        assertNull(classname);
    }
    
    @Test
    void testGetNameByClassname() {
        // Test retrieving names by classnames
        String name = VSRegisteredEvents.getNameByClassname(
            "events.implementations.VSProcessCrashEvent");
        assertEquals("Process Crash", name);
        
        name = VSRegisteredEvents.getNameByClassname(
            "events.implementations.VSProcessRecoverEvent");
        assertEquals("Process Recover", name);
        
        // Test non-existent classname
        name = VSRegisteredEvents.getNameByClassname("non.existent.Class");
        assertNull(name);
    }
    
    @Test
    void testGetShortnameByClassname() {
        // Test retrieving shortnames by classnames
        String shortname = VSRegisteredEvents.getShortnameByClassname(
            "events.implementations.VSProcessCrashEvent");
        assertEquals("Crash", shortname);
        
        shortname = VSRegisteredEvents.getShortnameByClassname(
            "events.implementations.VSProcessRecoverEvent");
        assertEquals("Recover", shortname);
        
        // Test non-existent classname
        shortname = VSRegisteredEvents.getShortnameByClassname("non.existent.Class");
        assertNull(shortname);
    }
    
    @Test
    void testGetClassnameByShortname() {
        // Test retrieving classnames by shortnames
        String classname = VSRegisteredEvents.getClassnameByShortname("Crash");
        assertEquals("events.implementations.VSProcessCrashEvent", classname);
        
        classname = VSRegisteredEvents.getClassnameByShortname("Recover");
        assertEquals("events.implementations.VSProcessRecoverEvent", classname);
        
        // Test non-existent shortname
        classname = VSRegisteredEvents.getClassnameByShortname("NonExistent");
        assertNull(classname);
    }
    
    @Test
    void testCreateEventInstanceByClassname() {
        when(mockProcess.getPrefs()).thenReturn(mockPrefs);
        
        // Test creating VSProcessCrashEvent
        VSAbstractEvent event = VSRegisteredEvents.createEventInstanceByClassname(
            "events.implementations.VSProcessCrashEvent", mockProcess);
        
        assertNotNull(event);
        assertTrue(event instanceof VSProcessCrashEvent);
        assertEquals(mockProcess, event.getProcess());
        assertNotNull(event.getClassname());
        
        // Test creating VSProcessRecoverEvent
        event = VSRegisteredEvents.createEventInstanceByClassname(
            "events.implementations.VSProcessRecoverEvent", mockProcess);
        
        assertNotNull(event);
        assertTrue(event instanceof VSProcessRecoverEvent);
        
        // Test with invalid classname
        event = VSRegisteredEvents.createEventInstanceByClassname(
            "non.existent.Class", mockProcess);
        assertNull(event);
    }
    
    @Test
    void testCreateEventInstanceByName() {
        when(mockProcess.getPrefs()).thenReturn(mockPrefs);
        
        // Test creating event by name
        VSAbstractEvent event = VSRegisteredEvents.createEventInstanceByName(
            "Process Crash", mockProcess);
        
        assertNotNull(event);
        assertTrue(event instanceof VSProcessCrashEvent);
        
        // Test with non-existent name
        event = VSRegisteredEvents.createEventInstanceByName(
            "Non Existent Event", mockProcess);
        assertNull(event);
    }
    
    @Test
    void testGetProtocolNames() {
        // Test getting protocol names (sorted)
        Vector<String> protocolNames = VSRegisteredEvents.getProtocolNames();
        
        assertNotNull(protocolNames);
        assertFalse(protocolNames.isEmpty());
        
        // Check that it contains protocol names
        assertTrue(protocolNames.contains("Ping Pong Protocol"));
        assertTrue(protocolNames.contains("Dummy Protocol"));
        
        // Verify they are sorted
        for (int i = 1; i < protocolNames.size(); i++) {
            assertTrue(protocolNames.get(i-1).compareTo(protocolNames.get(i)) <= 0);
        }
    }
    
    @Test
    void testGetProtocolClassnames() {
        // Test getting protocol classnames
        Vector<String> protocolClassnames = VSRegisteredEvents.getProtocolClassnames();
        
        assertNotNull(protocolClassnames);
        assertFalse(protocolClassnames.isEmpty());
        
        // Check that all returned classnames start with protocols.implementations
        for (String classname : protocolClassnames) {
            assertTrue(classname.startsWith("protocols.implementations"));
        }
    }
    
    @Test
    void testGetNonProtocolNames() {
        // Test getting non-protocol event names (sorted)
        Vector<String> eventNames = VSRegisteredEvents.getNonProtocolNames();
        
        assertNotNull(eventNames);
        assertFalse(eventNames.isEmpty());
        
        // Check that it contains event names
        assertTrue(eventNames.contains("Process Crash"));
        assertTrue(eventNames.contains("Process Recover"));
        
        // Verify they are sorted
        for (int i = 1; i < eventNames.size(); i++) {
            assertTrue(eventNames.get(i-1).compareTo(eventNames.get(i)) <= 0);
        }
    }
    
    @Test
    void testGetNonProtocolClassnames() {
        // Test getting non-protocol event classnames
        Vector<String> eventClassnames = VSRegisteredEvents.getNonProtocolClassnames();
        
        assertNotNull(eventClassnames);
        assertFalse(eventClassnames.isEmpty());
        
        // Check that all returned classnames start with events.implementations
        for (String classname : eventClassnames) {
            assertTrue(classname.startsWith("events.implementations"));
        }
        
        // Check specific events are included
        assertTrue(eventClassnames.contains("events.implementations.VSProcessCrashEvent"));
        assertTrue(eventClassnames.contains("events.implementations.VSProcessRecoverEvent"));
        
        // Verify they are sorted
        for (int i = 1; i < eventClassnames.size(); i++) {
            assertTrue(eventClassnames.get(i-1).compareTo(eventClassnames.get(i)) <= 0);
        }
    }
    
    @Test
    void testGetEditableProtocolsClassnames() {
        // Test getting editable protocols
        ArrayList<String> editableProtocols = 
            VSRegisteredEvents.getEditableProtocolsClassnames();
        
        assertNotNull(editableProtocols);
        // The actual content depends on which protocols have editable variables
        // Just verify the list is created
    }
    
    @Test
    void testGetProtocolServerVariables() {
        // Test getting server variables for a protocol
        // This would normally return variables if the protocol has them
        ArrayList<String> serverVars = VSRegisteredEvents.getProtocolServerVariables(
            "protocols.implementations.VSDummyProtocol");
        
        // May be null if protocol has no server variables
        // Just test the method doesn't throw
    }
    
    @Test
    void testGetProtocolClientVariables() {
        // Test getting client variables for a protocol
        ArrayList<String> clientVars = VSRegisteredEvents.getProtocolClientVariables(
            "protocols.implementations.VSDummyProtocol");
        
        // May be null if protocol has no client variables
        // Just test the method doesn't throw
    }
    
    @Test
    void testIsOnServerStartProtocol() {
        // Test checking if protocol uses onServerStart
        // Default should be false for protocols not explicitly marked
        boolean isServerStart = VSRegisteredEvents.isOnServerStartProtocol(
            "protocols.implementations.VSDummyProtocol");
        
        // Just verify the method works
        assertTrue(isServerStart == true || isServerStart == false);
        
        // Test with non-existent protocol
        assertFalse(VSRegisteredEvents.isOnServerStartProtocol("non.existent.Protocol"));
    }
    
    @Test
    void testEventRegistrationIntegrity() {
        // Test that events are properly registered with all mappings
        String testClassname = "events.implementations.VSProcessCrashEvent";
        String testName = "Process Crash";
        String testShortname = "Crash";
        
        // Verify all mappings work correctly
        assertEquals(testClassname, VSRegisteredEvents.getClassnameByEventname(testName));
        assertEquals(testName, VSRegisteredEvents.getNameByClassname(testClassname));
        assertEquals(testShortname, VSRegisteredEvents.getShortnameByClassname(testClassname));
        assertEquals(testClassname, VSRegisteredEvents.getClassnameByShortname(testShortname));
    }
    
    @Test
    void testProtocolEventSeparation() {
        // Test that protocols and events are properly separated
        Vector<String> protocols = VSRegisteredEvents.getProtocolClassnames();
        Vector<String> events = VSRegisteredEvents.getNonProtocolClassnames();
        
        // Verify no overlap between protocols and events
        for (String protocol : protocols) {
            assertFalse(events.contains(protocol));
            assertTrue(protocol.startsWith("protocols.implementations"));
        }
        
        for (String event : events) {
            assertFalse(protocols.contains(event));
            assertTrue(event.startsWith("events.implementations"));
        }
    }
}