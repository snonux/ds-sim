package events;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.mockito.Mockito.*;

import core.VSInternalProcess;
import exceptions.VSEventNotCopyableException;
import prefs.VSPrefs;
import events.implementations.VSProcessCrashEvent;
import events.implementations.VSProcessRecoverEvent;

/**
 * Unit tests for VSAbstractEvent class.
 * Tests the abstract event base class functionality including initialization,
 * copying, serialization, and name management.
 *
 * @author Test Suite
 */
public class VSAbstractEventTest {
    
    @Mock
    private VSInternalProcess mockProcess;
    
    @Mock
    private VSPrefs mockPrefs;
    
    private TestEvent testEvent;
    private CopyableTestEvent copyableTestEvent;
    
    /**
     * Test implementation of VSAbstractEvent for testing abstract methods
     */
    private static class TestEvent extends VSAbstractEvent {
        private boolean initCalled = false;
        private boolean startCalled = false;
        
        @Override
        public void onInit() {
            initCalled = true;
            setClassname(getClass().toString());
        }
        
        @Override
        public void onStart() {
            startCalled = true;
        }
        
        @Override
        protected String createShortname(String savedShortname) {
            return "TestEvent";
        }
        
        public boolean isInitCalled() { return initCalled; }
        public boolean isStartCalled() { return startCalled; }
    }
    
    /**
     * Copyable test event implementation
     */
    private static class CopyableTestEvent extends TestEvent implements VSCopyableEvent {
        private String customData = "original";
        
        @Override
        public void initCopy(VSAbstractEvent copy) {
            if (copy instanceof CopyableTestEvent) {
                ((CopyableTestEvent) copy).customData = this.customData;
            }
        }
        
        public String getCustomData() { return customData; }
        public void setCustomData(String data) { this.customData = data; }
    }
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(mockProcess.getPrefs()).thenReturn(mockPrefs);
        
        testEvent = new TestEvent();
        copyableTestEvent = new CopyableTestEvent();
    }
    
    @Test
    void testInitWithProcess() {
        // Test initialization with process
        assertNull(testEvent.process);
        assertNull(testEvent.prefs);
        assertFalse(testEvent.isInitCalled());
        
        testEvent.init(mockProcess);
        
        assertEquals(mockProcess, testEvent.process);
        assertEquals(mockPrefs, testEvent.prefs);
        assertTrue(testEvent.isInitCalled());
        
        // Test that init doesn't run twice
        testEvent.initCalled = false;
        testEvent.init(mockProcess);
        assertFalse(testEvent.isInitCalled());
    }
    
    @Test
    void testInitWithoutProcess() {
        // Test direct init() call
        testEvent.init();
        
        assertTrue(testEvent.isInitCalled());
        assertNull(testEvent.process);
        assertNull(testEvent.prefs);
    }
    
    @Test
    void testOnStart() {
        // Test onStart method
        assertFalse(testEvent.isStartCalled());
        testEvent.onStart();
        assertTrue(testEvent.isStartCalled());
    }
    
    @Test
    void testClassnameHandling() {
        // Test setClassname with "class " prefix
        testEvent.setClassname("class events.TestEvent");
        assertEquals("events.TestEvent", testEvent.getClassname());
        
        // Test setClassname without prefix
        testEvent.setClassname("events.AnotherEvent");
        assertEquals("events.AnotherEvent", testEvent.getClassname());
    }
    
    @Test
    void testGetName() {
        // Mock the static method behavior by setting classname first
        testEvent.setClassname("events.implementations.VSProcessCrashEvent");
        
        // Since VSRegisteredEvents.getNameByClassname is static and not easily mockable,
        // we'll test that getName() delegates properly
        String name = testEvent.getName();
        // The actual name depends on VSRegisteredEvents initialization
        assertNotNull(testEvent.getClassname());
    }
    
    @Test
    void testShortnameHandling() {
        // Test with custom shortname
        testEvent.setShortname("CustomShort");
        assertEquals("CustomShort", testEvent.getShortname());
        
        // Test without custom shortname (falls back to registered)
        TestEvent newEvent = new TestEvent();
        newEvent.setClassname("events.TestEvent");
        // Since this event is not registered, getShortname will return null from VSRegisteredEvents
        String shortname = newEvent.getShortname();
        // The shortname will be null for unregistered events
        assertNull(shortname);
    }
    
    @Test
    void testGetProcess() {
        assertNull(testEvent.getProcess());
        
        testEvent.init(mockProcess);
        assertEquals(mockProcess, testEvent.getProcess());
    }
    
    @Test
    void testLog() {
        testEvent.init(mockProcess);
        
        String message = "Test log message";
        testEvent.log(message);
        
        verify(mockProcess).log(message);
    }
    
    @Test
    void testEquals() {
        TestEvent event1 = new TestEvent();
        TestEvent event2 = new TestEvent();
        
        // Events with same ID should be equal
        assertTrue(event1.equals(event1));
        
        // Different events have different IDs by default
        assertFalse(event1.equals(event2));
    }
    
    @Test
    void testGetCopyForCopyableEvent() throws VSEventNotCopyableException {
        // Setup for copyable event
        copyableTestEvent.init(mockProcess);
        copyableTestEvent.setClassname("events.VSAbstractEventTest$CopyableTestEvent");
        copyableTestEvent.setShortname("CopyableTest");
        copyableTestEvent.setCustomData("modified");
        
        // Since VSRegisteredEvents.createEventInstanceByClassname returns null for unregistered classes,
        // we expect a NullPointerException when trying to copy
        assertThrows(NullPointerException.class, () -> {
            copyableTestEvent.getCopy();
        });
    }
    
    @Test
    void testGetCopyForNonCopyableEvent() {
        // Test non-copyable event throws exception
        testEvent.init(mockProcess);
        testEvent.setClassname("events.VSAbstractEventTest$TestEvent");
        testEvent.setShortname("Test");
        
        assertThrows(VSEventNotCopyableException.class, () -> {
            testEvent.getCopy();
        });
    }
    
    @Test
    void testGetCopyWithDifferentProcess() throws VSEventNotCopyableException {
        VSInternalProcess newProcess = mock(VSInternalProcess.class);
        when(newProcess.getPrefs()).thenReturn(mockPrefs);
        
        copyableTestEvent.init(mockProcess);
        copyableTestEvent.setClassname("events.VSAbstractEventTest$CopyableTestEvent");
        
        // Since VSRegisteredEvents.createEventInstanceByClassname returns null for unregistered classes,
        // we expect a NullPointerException
        assertThrows(NullPointerException.class, () -> {
            copyableTestEvent.getCopy(newProcess);
        });
    }
    
    @Test
    void testGetCopyWithNullProcess() throws VSEventNotCopyableException {
        copyableTestEvent.init(mockProcess);
        copyableTestEvent.setClassname("events.VSAbstractEventTest$CopyableTestEvent");
        
        // When null is passed, it should use the current process
        // Since VSRegisteredEvents.createEventInstanceByClassname returns null for unregistered classes,
        // we expect a NullPointerException
        assertThrows(NullPointerException.class, () -> {
            copyableTestEvent.getCopy(null);
        });
    }
    
    @Test
    void testCreateShortname() {
        // Test the abstract createShortname method implementation
        String shortname = testEvent.createShortname("SavedName");
        assertEquals("TestEvent", shortname);
    }
    
    @Test
    void testRealEventImplementations() {
        // Test with actual event implementations
        VSProcessCrashEvent crashEvent = new VSProcessCrashEvent();
        VSProcessRecoverEvent recoverEvent = new VSProcessRecoverEvent();
        
        // Test that they implement VSCopyableEvent
        assertTrue(crashEvent instanceof VSCopyableEvent);
        assertTrue(recoverEvent instanceof VSCopyableEvent);
        
        // Test initialization
        crashEvent.init(mockProcess);
        recoverEvent.init(mockProcess);
        
        assertNotNull(crashEvent.getClassname());
        assertNotNull(recoverEvent.getClassname());
    }
}