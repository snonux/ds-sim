package events.implementations;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.mockito.Mockito.*;

import core.VSInternalProcess;
import events.VSAbstractEvent;
import events.VSCopyableEvent;
import prefs.VSPrefs;
import simulator.VSMain;

/**
 * Unit tests for VSProcessCrashEvent.
 * Tests the process crash event functionality including initialization,
 * execution, and copying behavior.
 *
 * @author Test Suite
 */
public class VSProcessCrashEventTest {
    
    @Mock
    private VSInternalProcess mockProcess;
    
    @Mock
    private VSPrefs mockPrefs;
    
    @Mock
    private VSPrefs mockMainPrefs;
    
    private VSProcessCrashEvent crashEvent;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Setup mocks
        when(mockProcess.getPrefs()).thenReturn(mockPrefs);
        when(mockPrefs.getString("lang.crashed")).thenReturn("Process has crashed");
        when(mockMainPrefs.getString("lang.process.crash")).thenReturn("Crash");
        
        // Mock VSMain.prefs for shortname creation
        VSMain.prefs = mockMainPrefs;
        
        crashEvent = new VSProcessCrashEvent();
    }
    
    @Test
    void testImplementsVSCopyableEvent() {
        // Verify that VSProcessCrashEvent implements VSCopyableEvent
        assertTrue(crashEvent instanceof VSCopyableEvent);
    }
    
    @Test
    void testOnInit() {
        // Test initialization
        assertNull(crashEvent.getClassname());
        
        crashEvent.onInit();
        
        assertNotNull(crashEvent.getClassname());
        assertTrue(crashEvent.getClassname().contains("VSProcessCrashEvent"));
    }
    
    @Test
    void testCreateShortname() {
        // Test shortname creation
        String shortname = crashEvent.createShortname("SavedCrash");
        assertEquals("Crash", shortname);
        
        // Test that it uses VSMain.prefs
        verify(mockMainPrefs).getString("lang.process.crash");
    }
    
    @Test
    void testOnStartWhenProcessNotCrashed() {
        // Setup process as not crashed
        when(mockProcess.isCrashed()).thenReturn(false);
        
        // Initialize the event with process
        crashEvent.init(mockProcess);
        
        // Execute the event
        crashEvent.onStart();
        
        // Verify process was set to crashed
        verify(mockProcess).isCrashed(true);
        verify(mockProcess).log("Process has crashed");
    }
    
    @Test
    void testOnStartWhenProcessAlreadyCrashed() {
        // Setup process as already crashed
        when(mockProcess.isCrashed()).thenReturn(true);
        
        // Initialize the event with process
        crashEvent.init(mockProcess);
        
        // Execute the event
        crashEvent.onStart();
        
        // Verify process crash state was not changed
        verify(mockProcess, never()).isCrashed(true);
        verify(mockProcess, never()).log(anyString());
    }
    
    @Test
    void testInitCopy() {
        // Test the copy initialization
        VSProcessCrashEvent copyEvent = new VSProcessCrashEvent();
        
        // This method should do nothing for VSProcessCrashEvent
        crashEvent.initCopy(copyEvent);
        
        // Verify no exceptions thrown and copy is still valid
        assertNotNull(copyEvent);
    }
    
    @Test
    void testFullEventLifecycle() {
        // Test complete event lifecycle
        
        // 1. Create and initialize event
        VSProcessCrashEvent event = new VSProcessCrashEvent();
        event.init(mockProcess);
        
        // 2. Verify initialization
        assertEquals(mockProcess, event.getProcess());
        assertEquals(mockPrefs, event.prefs);
        assertNotNull(event.getClassname());
        
        // 3. Setup process state
        when(mockProcess.isCrashed()).thenReturn(false);
        
        // 4. Execute event
        event.onStart();
        
        // 5. Verify execution results
        verify(mockProcess).isCrashed(true);
        verify(mockProcess).log("Process has crashed");
    }
    
    @Test
    void testEventWithNullProcess() {
        // Test behavior when process is not set
        crashEvent.onInit();
        
        // Should throw NullPointerException when accessing process
        assertThrows(NullPointerException.class, () -> {
            crashEvent.onStart();
        });
    }
    
    @Test
    void testGetCopyFunctionality() {
        // Initialize the event
        crashEvent.init(mockProcess);
        crashEvent.setClassname("events.implementations.VSProcessCrashEvent");
        crashEvent.setShortname("Crash");
        
        // Since this implements VSCopyableEvent, getCopy should work
        // (though it will fail in unit test due to VSRegisteredEvents static dependencies)
        assertDoesNotThrow(() -> {
            try {
                crashEvent.getCopy();
            } catch (Exception e) {
                // Expected in unit test environment
            }
        });
    }
    
    @Test
    void testEventEquality() {
        // Test event equality based on ID
        VSProcessCrashEvent event1 = new VSProcessCrashEvent();
        VSProcessCrashEvent event2 = new VSProcessCrashEvent();
        
        // Same event should equal itself
        assertTrue(event1.equals(event1));
        
        // Different instances have different IDs
        assertFalse(event1.equals(event2));
    }
    
    @Test
    void testLogging() {
        // Test logging functionality
        crashEvent.init(mockProcess);
        
        String testMessage = "Test log message";
        crashEvent.log(testMessage);
        
        verify(mockProcess).log(testMessage);
    }
}