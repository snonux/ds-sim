package core;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import events.VSAbstractEvent;
import events.implementations.VSProcessCrashEvent;
import events.implementations.VSProcessRecoverEvent;
import events.internal.VSMessageReceiveEvent;
import events.internal.VSProtocolEvent;
import exceptions.VSEventNotCopyableException;
import prefs.VSPrefs;
import protocols.VSAbstractProtocol;
import serialize.VSSerialize;

/**
 * Unit tests for VSTask class.
 * Tests task creation, execution, comparison, and serialization functionality.
 * 
 * @author Test Suite
 */
class VSTaskTest {
    
    @Mock
    private VSInternalProcess mockProcess;
    
    @Mock
    private VSAbstractEvent mockEvent;
    
    @Mock
    private VSPrefs mockPrefs;
    
    @Mock
    private VSAbstractProtocol mockProtocol;
    
    private VSTask task;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(mockProcess.getPrefs()).thenReturn(mockPrefs);
        when(mockPrefs.getString("lang.task")).thenReturn("Task");
        when(mockProcess.getProcessID()).thenReturn(1);
    }
    
    @Test
    @DisplayName("Test VSTask constructor with local timing")
    void testConstructorLocalTiming() {
        // Given
        long taskTime = 1000L;
        
        // When
        task = new VSTask(taskTime, mockProcess, mockEvent, VSTask.LOCAL);
        
        // Then
        assertEquals(taskTime, task.getTaskTime());
        assertEquals(mockProcess, task.getProcess());
        assertEquals(mockEvent, task.getEvent());
        assertFalse(task.isGlobalTimed());
        assertTrue(task.getTaskNum() > 0);
    }
    
    @Test
    @DisplayName("Test VSTask constructor with global timing")
    void testConstructorGlobalTiming() {
        // Given
        long taskTime = 2000L;
        
        // When
        task = new VSTask(taskTime, mockProcess, mockEvent, VSTask.GLOBAL);
        
        // Then
        assertEquals(taskTime, task.getTaskTime());
        assertTrue(task.isGlobalTimed());
    }
    
    @Test
    @DisplayName("Test VSTask constructor with negative time")
    void testConstructorNegativeTime() {
        // Given
        long negativeTime = -100L;
        
        // When
        task = new VSTask(negativeTime, mockProcess, mockEvent, VSTask.LOCAL);
        
        // Then
        assertEquals(0L, task.getTaskTime()); // Should be set to 0
    }
    
    @Test
    @DisplayName("Test VSTask copy constructor with copyable event")
    void testCopyConstructorWithCopyableEvent() throws VSEventNotCopyableException {
        // Given
        VSTask originalTask = new VSTask(1000L, mockProcess, mockEvent, VSTask.LOCAL);
        VSAbstractEvent mockCopyEvent = mock(VSAbstractEvent.class);
        when(mockEvent.getCopy()).thenReturn(mockCopyEvent);
        
        // When
        VSTask copiedTask = new VSTask(originalTask);
        
        // Then
        assertEquals(originalTask.getTaskTime(), copiedTask.getTaskTime());
        assertEquals(originalTask.getProcess(), copiedTask.getProcess());
        assertEquals(mockCopyEvent, copiedTask.getEvent());
        assertEquals(originalTask.isGlobalTimed(), copiedTask.isGlobalTimed());
    }
    
    @Test
    @DisplayName("Test VSTask copy constructor with non-copyable event")
    void testCopyConstructorWithNonCopyableEvent() throws VSEventNotCopyableException {
        // Given
        VSTask originalTask = new VSTask(1000L, mockProcess, mockEvent, VSTask.LOCAL);
        when(mockEvent.getCopy()).thenThrow(new VSEventNotCopyableException("Not copyable"));
        
        // When
        VSTask copiedTask = new VSTask(originalTask);
        
        // Then
        assertEquals(originalTask.getEvent(), copiedTask.getEvent()); // Should use original event
    }
    
    @Test
    @DisplayName("Test isProgrammed getter and setter")
    void testIsProgrammedGetterSetter() {
        // Given
        task = new VSTask(1000L, mockProcess, mockEvent, VSTask.LOCAL);
        
        // When/Then
        assertFalse(task.isProgrammed()); // Default value
        
        task.isProgrammed(true);
        assertTrue(task.isProgrammed());
        
        task.isProgrammed(false);
        assertFalse(task.isProgrammed());
    }
    
    @Test
    @DisplayName("Test hasInternalEvent method")
    void testHasInternalEvent() {
        // Given
        VSAbstractEvent normalEvent = mock(VSAbstractEvent.class);
        VSMessageReceiveEvent internalEvent = mock(VSMessageReceiveEvent.class);
        
        // Setup mocks to return correct values
        when(normalEvent.isInternalEvent()).thenReturn(false);
        when(internalEvent.isInternalEvent()).thenReturn(true);
        
        // When/Then
        task = new VSTask(1000L, mockProcess, normalEvent, VSTask.LOCAL);
        assertFalse(task.hasInternalEvent());
        
        task = new VSTask(1000L, mockProcess, internalEvent, VSTask.LOCAL);
        assertTrue(task.hasInternalEvent());
    }
    
    @Test
    @DisplayName("Test hasMessageReceiveEvent method")
    void testHasMessageReceiveEvent() {
        // Given
        VSMessageReceiveEvent messageEvent = mock(VSMessageReceiveEvent.class);
        when(messageEvent.isMessageReceiveEvent()).thenReturn(true);
        
        // When
        task = new VSTask(1000L, mockProcess, messageEvent, VSTask.LOCAL);
        
        // Then
        assertTrue(task.hasMessageReceiveEvent());
    }
    
    @Test
    @DisplayName("Test hasProcessRecoverEvent method")
    void testHasProcessRecoverEvent() {
        // Given
        VSProcessRecoverEvent recoverEvent = mock(VSProcessRecoverEvent.class);
        when(recoverEvent.isProcessRecoverEvent()).thenReturn(true);
        
        // When
        task = new VSTask(1000L, mockProcess, recoverEvent, VSTask.LOCAL);
        
        // Then
        assertTrue(task.hasProcessRecoverEvent());
    }
    
    @Test
    @DisplayName("Test isProtocol method")
    void testIsProtocol() {
        // Given
        when(mockProtocol.equals(mockProtocol)).thenReturn(true);
        
        // When
        task = new VSTask(1000L, mockProcess, mockProtocol, VSTask.LOCAL);
        
        // Then
        assertTrue(task.isProtocol(mockProtocol));
        
        VSAbstractProtocol otherProtocol = mock(VSAbstractProtocol.class);
        assertFalse(task.isProtocol(otherProtocol));
    }
    
    @Test
    @DisplayName("Test timeOver method for local timed task")
    void testTimeOverLocalTimed() {
        // Given
        task = new VSTask(1000L, mockProcess, mockEvent, VSTask.LOCAL);
        
        // When/Then
        when(mockProcess.getTime()).thenReturn(500L);
        assertFalse(task.timeOver());
        
        when(mockProcess.getTime()).thenReturn(1001L);
        assertTrue(task.timeOver());
    }
    
    @Test
    @DisplayName("Test timeOver method for global timed task")
    void testTimeOverGlobalTimed() {
        // Given
        task = new VSTask(1000L, mockProcess, mockEvent, VSTask.GLOBAL);
        
        // When/Then
        when(mockProcess.getGlobalTime()).thenReturn(500L);
        assertFalse(task.timeOver());
        
        when(mockProcess.getGlobalTime()).thenReturn(1001L);
        assertTrue(task.timeOver());
    }
    
    @Test
    @DisplayName("Test equals method")
    void testEquals() {
        // Given
        VSTask task1 = new VSTask(1000L, mockProcess, mockEvent, VSTask.LOCAL);
        VSTask task2 = new VSTask(2000L, mockProcess, mockEvent, VSTask.GLOBAL);
        VSTask task3 = new VSTask(task1); // Copy constructor ensures different taskNum
        
        // When/Then
        assertTrue(task1.equals(task1)); // Same object
        assertFalse(task1.equals(task2)); // Different task nums
        assertFalse(task1.equals(task3)); // Different task nums even though copied
    }
    
    @Test
    @DisplayName("Test isProcess method")
    void testIsProcess() {
        // Given
        task = new VSTask(1000L, mockProcess, mockEvent, VSTask.LOCAL);
        VSInternalProcess otherProcess = mock(VSInternalProcess.class);
        
        when(mockProcess.equals(mockProcess)).thenReturn(true);
        when(mockProcess.equals(otherProcess)).thenReturn(false);
        
        // When/Then
        assertTrue(task.isProcess(mockProcess));
        assertFalse(task.isProcess(otherProcess));
    }
    
    @Test
    @DisplayName("Test run method initializes event and calls onStart")
    void testRunMethod() {
        // Given
        task = new VSTask(1000L, mockProcess, mockEvent, VSTask.LOCAL);
        when(mockEvent.getProcess()).thenReturn(null);
        when(mockEvent.shouldIncreaseTimestamps()).thenReturn(true);
        
        // When
        task.run();
        
        // Then
        verify(mockEvent).init(mockProcess);
        verify(mockProcess).increaseVectorAndLamportTimeIfAll();
        verify(mockEvent).onStart();
    }
    
    @Test
    @DisplayName("Test run method with MessageReceiveEvent does not increase time")
    void testRunMethodWithMessageReceiveEvent() {
        // Given
        VSMessageReceiveEvent messageEvent = mock(VSMessageReceiveEvent.class);
        task = new VSTask(1000L, mockProcess, messageEvent, VSTask.LOCAL);
        when(messageEvent.getProcess()).thenReturn(mockProcess);
        when(messageEvent.shouldIncreaseTimestamps()).thenReturn(false);
        
        // When
        task.run();
        
        // Then
        verify(mockProcess, never()).increaseVectorAndLamportTimeIfAll();
        verify(messageEvent).onStart();
    }
    
    @Test
    @DisplayName("Test run method with Protocol does not increase time")
    void testRunMethodWithProtocol() {
        // Given
        task = new VSTask(1000L, mockProcess, mockProtocol, VSTask.LOCAL);
        when(mockProtocol.getProcess()).thenReturn(mockProcess);
        when(mockProtocol.shouldIncreaseTimestamps()).thenReturn(false);
        
        // When
        task.run();
        
        // Then
        verify(mockProcess, never()).increaseVectorAndLamportTimeIfAll();
        verify(mockProtocol).onStart();
    }
    
    @Test
    @DisplayName("Test setTaskTime method")
    void testSetTaskTime() {
        // Given
        task = new VSTask(1000L, mockProcess, mockEvent, VSTask.LOCAL);
        
        // When
        task.setTaskTime(2000L);
        
        // Then
        assertEquals(2000L, task.getTaskTime());
    }
    
    @Test
    @DisplayName("Test setProcess method with different process")
    void testSetProcessWithDifferentProcess() throws VSEventNotCopyableException {
        // Given
        task = new VSTask(1000L, mockProcess, mockEvent, VSTask.LOCAL);
        VSInternalProcess newProcess = mock(VSInternalProcess.class);
        VSAbstractEvent copiedEvent = mock(VSAbstractEvent.class);
        
        when(mockProcess.equals(newProcess)).thenReturn(false);
        when(mockEvent.getCopy(newProcess)).thenReturn(copiedEvent);
        
        // When
        task.setProcess(newProcess);
        
        // Then
        assertEquals(newProcess, task.getProcess());
        assertEquals(copiedEvent, task.getEvent());
    }
    
    @Test
    @DisplayName("Test setProcess method with same process")
    void testSetProcessWithSameProcess() throws VSEventNotCopyableException {
        // Given
        task = new VSTask(1000L, mockProcess, mockEvent, VSTask.LOCAL);
        when(mockProcess.equals(mockProcess)).thenReturn(true);
        
        // When
        task.setProcess(mockProcess);
        
        // Then
        verify(mockEvent, never()).getCopy(any());
    }
    
    @Test
    @DisplayName("Test setProcess method with non-copyable protocol")
    void testSetProcessWithNonCopyableProtocol() throws VSEventNotCopyableException {
        // Given
        task = new VSTask(1000L, mockProcess, mockProtocol, VSTask.LOCAL);
        VSInternalProcess newProcess = mock(VSInternalProcess.class);
        VSAbstractProtocol newProtocol = mock(VSAbstractProtocol.class);
        
        when(mockProcess.equals(newProcess)).thenReturn(false);
        when(mockProtocol.getCopy(newProcess)).thenThrow(new VSEventNotCopyableException("Not copyable"));
        when(mockProtocol.getClassname()).thenReturn("test.Protocol");
        when(mockProtocol.getShortname()).thenReturn("TEST");
        when(newProcess.getProtocolObject("test.Protocol")).thenReturn(newProtocol);
        
        // When
        task.setProcess(newProcess);
        
        // Then
        assertEquals(newProcess, task.getProcess());
        assertEquals(newProtocol, task.getEvent());
        verify(newProtocol).setShortname("TEST");
    }
    
    @Test
    @DisplayName("Test toString method")
    void testToString() {
        // Given
        task = new VSTask(1000L, mockProcess, mockEvent, VSTask.LOCAL);
        when(mockEvent.toString()).thenReturn("TestEvent");
        when(mockProcess.getProcessID()).thenReturn(42);
        
        // When
        String result = task.toString();
        
        // Then
        assertTrue(result.contains("Task"));
        assertTrue(result.contains("1000"));
        assertTrue(result.contains("TestEvent"));
        assertTrue(result.contains("PID: 42"));
    }
    
    @Test
    @DisplayName("Test compareTo method with different task times")
    void testCompareToWithDifferentTimes() {
        // Given
        VSTask task1 = new VSTask(1000L, mockProcess, mockEvent, VSTask.LOCAL);
        VSTask task2 = new VSTask(2000L, mockProcess, mockEvent, VSTask.LOCAL);
        
        // When/Then
        assertTrue(task1.compareTo(task2) < 0);
        assertTrue(task2.compareTo(task1) > 0);
        assertEquals(0, task1.compareTo(task1));
    }
    
    @Test
    @DisplayName("Test compareTo method prioritizes ProcessRecoverEvent")
    void testCompareToProcessRecoverEventPriority() {
        // Given
        VSProcessRecoverEvent recoverEvent = mock(VSProcessRecoverEvent.class);
        when(recoverEvent.getEventPriority()).thenReturn(-3); // Highest priority
        when(mockEvent.getEventPriority()).thenReturn(0); // Normal priority
        VSTask recoverTask = new VSTask(1000L, mockProcess, recoverEvent, VSTask.LOCAL);
        VSTask normalTask = new VSTask(1000L, mockProcess, mockEvent, VSTask.LOCAL);
        
        // When/Then
        assertTrue(recoverTask.compareTo(normalTask) < 0);
        assertTrue(normalTask.compareTo(recoverTask) > 0);
    }
    
    @Test
    @DisplayName("Test compareTo method prioritizes ProcessCrashEvent second")
    void testCompareToProcessCrashEventPriority() {
        // Given
        VSProcessCrashEvent crashEvent = mock(VSProcessCrashEvent.class);
        when(crashEvent.getEventPriority()).thenReturn(-2); // Second highest priority
        when(mockEvent.getEventPriority()).thenReturn(0); // Normal priority
        VSTask crashTask = new VSTask(1000L, mockProcess, crashEvent, VSTask.LOCAL);
        VSTask normalTask = new VSTask(1000L, mockProcess, mockEvent, VSTask.LOCAL);
        
        // When/Then
        assertTrue(crashTask.compareTo(normalTask) < 0);
        assertTrue(normalTask.compareTo(crashTask) > 0);
    }
    
    @Test
    @DisplayName("Test compareTo method prioritizes ProtocolEvent third")
    void testCompareToProtocolEventPriority() {
        // Given
        VSProtocolEvent protocolEvent = mock(VSProtocolEvent.class);
        when(protocolEvent.getEventPriority()).thenReturn(-1); // Third highest priority
        when(mockEvent.getEventPriority()).thenReturn(0); // Normal priority
        VSTask protocolTask = new VSTask(1000L, mockProcess, protocolEvent, VSTask.LOCAL);
        VSTask normalTask = new VSTask(1000L, mockProcess, mockEvent, VSTask.LOCAL);
        
        // When/Then
        assertTrue(protocolTask.compareTo(normalTask) < 0);
        assertTrue(normalTask.compareTo(protocolTask) > 0);
    }
    
    @Test
    @DisplayName("Test compareTo method with null shortnames")
    void testCompareToWithNullShortnames() {
        // Given
        VSTask task1 = new VSTask(1000L, mockProcess, mockEvent, VSTask.LOCAL);
        VSTask task2 = new VSTask(1000L, mockProcess, mockEvent, VSTask.LOCAL);
        
        when(mockEvent.getShortname()).thenReturn(null);
        
        // When/Then
        assertEquals(0, task1.compareTo(task2));
    }
    
    @Test
    @DisplayName("Test compareTo method with non-VSTask object")
    void testCompareToWithNonTaskObject() {
        // Given
        task = new VSTask(1000L, mockProcess, mockEvent, VSTask.LOCAL);
        Object notATask = new Object();
        
        // When/Then
        assertEquals(0, task.compareTo(notATask));
    }
    
    @Test
    @DisplayName("Test serialization and deserialization")
    void testSerializationDeserialization() throws Exception {
        // Given
        task = new VSTask(1500L, mockProcess, mockEvent, VSTask.GLOBAL);
        task.isProgrammed(true);
        
        when(mockEvent.getClassname()).thenReturn("test.Event");
        when(mockEvent.getID()).thenReturn(123);
        when(mockProcess.getProcessNum()).thenReturn(5);
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        VSSerialize serialize = mock(VSSerialize.class);
        
        // When - Serialize
        task.serialize(serialize, oos);
        oos.flush();
        
        // Then - Verify serialization calls
        verify(mockEvent).serialize(serialize, oos);
        
        // When - Deserialize
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(bais);
        
        // Mock the deserialization environment
        when(serialize.getObject(5, "process")).thenReturn(mockProcess);
        when(serialize.objectExists(123, "event")).thenReturn(true);
        when(serialize.getObject(123, "event")).thenReturn(mockEvent);
        
        VSTask deserializedTask = new VSTask(serialize, ois);
        
        // Then - Verify deserialization
        assertNotNull(deserializedTask);
        verify(mockEvent).deserialize(serialize, ois);
    }
}