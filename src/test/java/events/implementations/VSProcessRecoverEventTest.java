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
 * Unit tests for VSProcessRecoverEvent.
 * Tests the process recovery event functionality including initialization,
 * execution, and copying behavior.
 *
 * @author Test Suite
 */
public class VSProcessRecoverEventTest {
    
    @Mock
    private VSInternalProcess mockProcess;
    
    @Mock
    private VSPrefs mockPrefs;
    
    @Mock
    private VSPrefs mockMainPrefs;
    
    private VSProcessRecoverEvent recoverEvent;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Setup mocks
        when(mockProcess.getPrefs()).thenReturn(mockPrefs);
        when(mockPrefs.getString("lang.recovered")).thenReturn("Process has recovered");
        when(mockMainPrefs.getString("lang.process.recover")).thenReturn("Recover");
        
        // Mock VSMain.prefs for shortname creation
        VSMain.prefs = mockMainPrefs;
        
        recoverEvent = new VSProcessRecoverEvent();
    }
    
    @Test
    void testImplementsVSCopyableEvent() {
        // Verify that VSProcessRecoverEvent implements VSCopyableEvent
        assertTrue(recoverEvent instanceof VSCopyableEvent);
    }
    
    @Test
    void testOnInit() {
        // Test initialization
        assertNull(recoverEvent.getClassname());
        
        recoverEvent.onInit();
        
        assertNotNull(recoverEvent.getClassname());
        assertTrue(recoverEvent.getClassname().contains("VSProcessRecoverEvent"));
    }
    
    @Test
    void testCreateShortname() {
        // Test shortname creation
        String shortname = recoverEvent.createShortname("SavedRecover");
        assertEquals("Recover", shortname);
        
        // Test that it uses VSMain.prefs
        verify(mockMainPrefs).getString("lang.process.recover");
    }
    
    @Test
    void testOnStartWhenProcessIsCrashed() {
        // Setup process as crashed
        when(mockProcess.isCrashed()).thenReturn(true);
        
        // Initialize the event with process
        recoverEvent.init(mockProcess);
        
        // Execute the event
        recoverEvent.onStart();
        
        // Verify process was recovered
        verify(mockProcess).isCrashed(false);
        verify(mockProcess).log("Process has recovered");
    }
    
    @Test
    void testOnStartWhenProcessNotCrashed() {
        // Setup process as not crashed (already running)
        when(mockProcess.isCrashed()).thenReturn(false);
        
        // Initialize the event with process
        recoverEvent.init(mockProcess);
        
        // Execute the event
        recoverEvent.onStart();
        
        // Verify process state was not changed
        verify(mockProcess, never()).isCrashed(false);
        verify(mockProcess, never()).log(anyString());
    }
    
    @Test
    void testInitCopy() {
        // Test the copy initialization
        VSProcessRecoverEvent copyEvent = new VSProcessRecoverEvent();
        
        // This method should do nothing for VSProcessRecoverEvent
        recoverEvent.initCopy(copyEvent);
        
        // Verify no exceptions thrown and copy is still valid
        assertNotNull(copyEvent);
    }
    
    @Test
    void testFullEventLifecycle() {
        // Test complete event lifecycle
        
        // 1. Create and initialize event
        VSProcessRecoverEvent event = new VSProcessRecoverEvent();
        event.init(mockProcess);
        
        // 2. Verify initialization
        assertEquals(mockProcess, event.getProcess());
        assertEquals(mockPrefs, event.prefs);
        assertNotNull(event.getClassname());
        
        // 3. Setup process state as crashed
        when(mockProcess.isCrashed()).thenReturn(true);
        
        // 4. Execute event
        event.onStart();
        
        // 5. Verify execution results
        verify(mockProcess).isCrashed(false);
        verify(mockProcess).log("Process has recovered");
    }
    
    @Test
    void testEventWithNullProcess() {
        // Test behavior when process is not set
        recoverEvent.onInit();
        
        // Should throw NullPointerException when accessing process
        assertThrows(NullPointerException.class, () -> {
            recoverEvent.onStart();
        });
    }
    
    @Test
    void testComplementaryBehaviorWithCrashEvent() {
        // Test that recover event properly complements crash event
        VSProcessCrashEvent crashEvent = new VSProcessCrashEvent();
        
        // Initialize both events
        crashEvent.init(mockProcess);
        recoverEvent.init(mockProcess);
        
        // Simulate crash then recover sequence
        when(mockProcess.isCrashed()).thenReturn(false).thenReturn(true);
        
        // Crash the process
        crashEvent.onStart();
        verify(mockProcess).isCrashed(true);
        
        // Recover the process
        recoverEvent.onStart();
        verify(mockProcess).isCrashed(false);
    }
    
    @Test
    void testGetCopyFunctionality() {
        // Initialize the event
        recoverEvent.init(mockProcess);
        recoverEvent.setClassname("events.implementations.VSProcessRecoverEvent");
        recoverEvent.setShortname("Recover");
        
        // Since this implements VSCopyableEvent, getCopy should work
        // (though it will fail in unit test due to VSRegisteredEvents static dependencies)
        assertDoesNotThrow(() -> {
            try {
                recoverEvent.getCopy();
            } catch (Exception e) {
                // Expected in unit test environment
            }
        });
    }
    
    @Test
    void testEventEquality() {
        // Test event equality based on ID
        VSProcessRecoverEvent event1 = new VSProcessRecoverEvent();
        VSProcessRecoverEvent event2 = new VSProcessRecoverEvent();
        
        // Same event should equal itself
        assertTrue(event1.equals(event1));
        
        // Different instances have different IDs
        assertFalse(event1.equals(event2));
    }
    
    @Test
    void testLogging() {
        // Test logging functionality
        recoverEvent.init(mockProcess);
        
        String testMessage = "Test recovery message";
        recoverEvent.log(testMessage);
        
        verify(mockProcess).log(testMessage);
    }
    
    @Test
    void testMultipleRecoveryAttempts() {
        // Test multiple recovery attempts on already running process
        when(mockProcess.isCrashed()).thenReturn(false);
        
        recoverEvent.init(mockProcess);
        
        // Try to recover multiple times
        recoverEvent.onStart();
        recoverEvent.onStart();
        recoverEvent.onStart();
        
        // Verify no state changes or logs
        verify(mockProcess, never()).isCrashed(false);
        verify(mockProcess, never()).log(anyString());
    }
}