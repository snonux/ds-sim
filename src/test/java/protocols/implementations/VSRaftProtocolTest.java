package protocols.implementations;

import core.VSAbstractProcess;
import core.VSInternalProcess;
import core.VSMessage;
import core.VSTaskManager;
import core.time.VSVectorTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import prefs.VSPrefs;
import simulator.VSLogging;
import simulator.VSSimulatorVisualization;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class VSRaftProtocolTest {
    
    private VSRaftProtocol protocol;
    private VSInternalProcess mockProcess;
    private VSPrefs mockPrefs;
    private VSSimulatorVisualization mockCanvas;
    private VSTaskManager mockTaskManager;
    private VSVectorTime mockVectorTime;
    
    @BeforeEach
    void setUp() {
        protocol = new VSRaftProtocol();
        mockProcess = mock(VSInternalProcess.class);
        mockPrefs = mock(VSPrefs.class);
        mockCanvas = mock(VSSimulatorVisualization.class);
        mockTaskManager = mock(VSTaskManager.class);
        mockVectorTime = mock(VSVectorTime.class);
        
        // Set up process behavior
        when(mockProcess.getProcessNum()).thenReturn(1);
        when(mockProcess.getTime()).thenReturn(1000L);
        when(mockProcess.getRandomPercentage()).thenReturn(50);
        when(mockProcess.getSimulatorCanvas()).thenReturn(mockCanvas);
        when(mockCanvas.getNumProcesses()).thenReturn(3);
        when(mockCanvas.getTaskManager()).thenReturn(mockTaskManager);
        when(mockProcess.getVectorTime()).thenReturn(mockVectorTime);
        when(mockVectorTime.getCopy()).thenReturn(mockVectorTime);
        when(mockProcess.getLamportTime()).thenReturn(100L);
        
        // Set process and prefs directly via field access (like other protocol tests)
        protocol.process = mockProcess;
        protocol.prefs = mockPrefs;
        protocol.isServer(true);
    }
    
    @Test
    void testServerInitialization() {
        // Test server initialization
        protocol.onServerInit();
        protocol.onServerStart();
        
        // Protocol should start as follower
        verify(mockProcess).log(contains("FOLLOWER"));
    }
    
    @Test
    void testElectionTimeout() {
        // Initialize protocol
        protocol.onServerInit();
        
        // Remove any scheduled tasks to clean state
        doNothing().when(mockTaskManager).removeAllTasks(any());
        protocol.onServerReset();
        protocol.onServerInit();
        protocol.onServerStart();
        
        // Simulate election timeout by calling onServerSchedule
        when(mockProcess.getTime()).thenReturn(2000L); // Well past timeout
        protocol.onServerSchedule();
        
        // Should start election and send vote requests
        ArgumentCaptor<VSMessage> messageCaptor = ArgumentCaptor.forClass(VSMessage.class);
        verify(mockProcess, atLeastOnce()).sendMessage(messageCaptor.capture());
        
        VSMessage sentMessage = messageCaptor.getValue();
        assertEquals("REQUEST_VOTE", sentMessage.getString("type"));
    }
    
    @Test
    void testVoteRequest() {
        // Initialize protocol
        protocol.onServerInit();
        
        // Create vote request from another node
        VSMessage voteRequest = mock(VSMessage.class);
        when(voteRequest.getString("type")).thenReturn("REQUEST_VOTE");
        when(voteRequest.getInteger("term")).thenReturn(2);
        when(voteRequest.getInteger("candidateId")).thenReturn(2);
        when(voteRequest.getInteger("lastLogIndex")).thenReturn(0);
        when(voteRequest.getInteger("lastLogTerm")).thenReturn(0);
        
        // Mock sender process
        VSInternalProcess mockSender = mock(VSInternalProcess.class);
        when(mockSender.getProcessNum()).thenReturn(2);
        when(voteRequest.getSendingProcess()).thenReturn(mockSender);
        
        // Process vote request
        protocol.onServerRecv(voteRequest);
        
        // Should send vote response
        ArgumentCaptor<VSMessage> responseCaptor = ArgumentCaptor.forClass(VSMessage.class);
        verify(mockProcess).sendMessage(responseCaptor.capture());
        
        VSMessage response = responseCaptor.getValue();
        assertEquals("VOTE_RESPONSE", response.getString("type"));
        assertTrue(response.getBoolean("voteGranted"));
    }
    
    @Test
    void testBecomeLeader() {
        // Initialize protocol
        protocol.onServerInit();
        protocol.isServer(true);
        
        // Start election
        when(mockProcess.getTime()).thenReturn(2000L);
        protocol.onServerSchedule();
        
        // Should vote for itself and need one more vote (in 3-node cluster)
        // Send vote response from node 0
        VSMessage voteResponse1 = mock(VSMessage.class);
        when(voteResponse1.getString("type")).thenReturn("VOTE_RESPONSE");
        when(voteResponse1.getInteger("term")).thenReturn(1);
        when(voteResponse1.getBoolean("voteGranted")).thenReturn(true);
        
        // Mock sender process
        VSInternalProcess mockSender1 = mock(VSInternalProcess.class);
        when(mockSender1.getProcessNum()).thenReturn(0);
        when(voteResponse1.getSendingProcess()).thenReturn(mockSender1);
        
        protocol.onServerRecv(voteResponse1);
        
        // Should become leader and highlight
        verify(mockProcess).highlightOn();
        verify(mockProcess, atLeastOnce()).log(contains("LEADER"));
    }
    
    @Test
    void testHeartbeats() {
        // Make node a leader
        protocol.onServerInit();
        protocol.isServer(true);
        
        // Simulate becoming leader
        when(mockProcess.getTime()).thenReturn(2000L);
        protocol.onServerSchedule(); // Start election
        
        // Get majority votes
        VSMessage voteResponse = mock(VSMessage.class);
        when(voteResponse.getString("type")).thenReturn("VOTE_RESPONSE");
        when(voteResponse.getInteger("term")).thenReturn(1);
        when(voteResponse.getBoolean("voteGranted")).thenReturn(true);
        
        // Mock sender process
        VSInternalProcess mockSender = mock(VSInternalProcess.class);
        when(mockSender.getProcessNum()).thenReturn(0);
        when(voteResponse.getSendingProcess()).thenReturn(mockSender);
        protocol.onServerRecv(voteResponse);
        
        // Clear previous invocations
        clearInvocations(mockProcess);
        
        // Trigger heartbeat
        protocol.onServerSchedule();
        
        // Should send append entries (heartbeats) to other nodes
        ArgumentCaptor<VSMessage> heartbeatCaptor = ArgumentCaptor.forClass(VSMessage.class);
        verify(mockProcess, atLeast(2)).sendMessage(heartbeatCaptor.capture());
        
        boolean foundAppendEntries = false;
        for (VSMessage msg : heartbeatCaptor.getAllValues()) {
            if ("APPEND_ENTRIES".equals(msg.getString("type"))) {
                foundAppendEntries = true;
                assertEquals(1, msg.getInteger("term"));
                assertEquals(1, msg.getInteger("leaderId"));
            }
        }
        assertTrue(foundAppendEntries);
    }
    
    @Test
    void testLogReplication() {
        // Initialize as leader
        protocol.onServerInit();
        protocol.isServer(true);
        
        // Become leader (simplified)
        when(mockProcess.getTime()).thenReturn(2000L);
        protocol.onServerSchedule();
        VSMessage voteResponse = mock(VSMessage.class);
        when(voteResponse.getString("type")).thenReturn("VOTE_RESPONSE");
        when(voteResponse.getInteger("term")).thenReturn(1);
        when(voteResponse.getBoolean("voteGranted")).thenReturn(true);
        
        // Mock sender process
        VSInternalProcess mockSender = mock(VSInternalProcess.class);
        when(mockSender.getProcessNum()).thenReturn(0);
        when(voteResponse.getSendingProcess()).thenReturn(mockSender);
        protocol.onServerRecv(voteResponse);
        
        // Client request
        VSMessage clientRequest = mock(VSMessage.class);
        when(clientRequest.getString("type")).thenReturn("CLIENT_REQUEST");
        when(clientRequest.getString("command")).thenReturn("SET x=42");
        
        // Mock sender process (client)
        VSInternalProcess mockClient = mock(VSInternalProcess.class);
        when(mockClient.getProcessNum()).thenReturn(2);
        when(clientRequest.getSendingProcess()).thenReturn(mockClient);
        
        protocol.onServerRecv(clientRequest);
        
        // Should log the command
        verify(mockProcess, atLeastOnce()).log(contains("SET x=42"));
    }
    
    @Test
    void testFollowerRejectsClientRequests() {
        // Initialize as follower
        protocol.onServerInit();
        protocol.isServer(true);
        
        // Client request to follower
        VSMessage clientRequest = mock(VSMessage.class);
        when(clientRequest.getString("type")).thenReturn("CLIENT_REQUEST");
        when(clientRequest.getString("command")).thenReturn("SET x=42");
        
        // Mock sender process
        VSInternalProcess mockClient = mock(VSInternalProcess.class);
        when(mockClient.getProcessNum()).thenReturn(2);
        when(clientRequest.getSendingProcess()).thenReturn(mockClient);
        
        protocol.onServerRecv(clientRequest);
        
        // Should send rejection response
        ArgumentCaptor<VSMessage> responseCaptor = ArgumentCaptor.forClass(VSMessage.class);
        verify(mockProcess).sendMessage(responseCaptor.capture());
        
        VSMessage response = responseCaptor.getValue();
        assertEquals("CLIENT_RESPONSE", response.getString("type"));
        assertFalse(response.getBoolean("success"));
    }
    
    @Test
    void testClientBehavior() {
        // Test client side
        protocol.isClient(true);
        protocol.onClientInit();
        
        // Mock scheduled task addition
        doNothing().when(mockTaskManager).addTask(any());
        
        protocol.onClientStart();
        
        // Should schedule client requests
        verify(mockProcess).getTime();
        
        // Simulate scheduled client request
        protocol.onClientSchedule();
        
        // Should send client request
        ArgumentCaptor<VSMessage> requestCaptor = ArgumentCaptor.forClass(VSMessage.class);
        verify(mockProcess).sendMessage(requestCaptor.capture());
        
        VSMessage request = requestCaptor.getValue();
        assertEquals("CLIENT_REQUEST", request.getString("type"));
        assertNotNull(request.getString("command"));
    }
    
    @Test
    void testTermUpdate() {
        // Initialize protocol
        protocol.onServerInit();
        protocol.isServer(true);
        
        // Receive message with higher term
        VSMessage higherTermMsg = mock(VSMessage.class);
        when(higherTermMsg.getString("type")).thenReturn("APPEND_ENTRIES");
        when(higherTermMsg.getInteger("term")).thenReturn(5);
        when(higherTermMsg.getInteger("leaderId")).thenReturn(2);
        when(higherTermMsg.getInteger("prevLogIndex")).thenReturn(0);
        when(higherTermMsg.getInteger("prevLogTerm")).thenReturn(0);
        when(higherTermMsg.getInteger("leaderCommit")).thenReturn(0);
        when(higherTermMsg.getInteger("entryCount")).thenReturn(0);
        
        // Mock sender process
        VSInternalProcess mockLeader = mock(VSInternalProcess.class);
        when(mockLeader.getProcessNum()).thenReturn(2);
        when(higherTermMsg.getSendingProcess()).thenReturn(mockLeader);
        
        protocol.onServerRecv(higherTermMsg);
        
        // Should become follower (no longer logs in onServerRecv)
        // Just verify the message was processed correctly by checking response
        ArgumentCaptor<VSMessage> responseCaptor = ArgumentCaptor.forClass(VSMessage.class);
        verify(mockProcess).sendMessage(responseCaptor.capture());
        
        VSMessage response = responseCaptor.getValue();
        assertEquals("APPEND_RESPONSE", response.getString("type"));
    }
}