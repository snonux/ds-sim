package protocols.implementations;

import java.util.Vector;

import core.VSInternalProcess;
import core.VSTask;
import core.time.VSVectorTime;
import events.implementations.VSLamportTimestampEvent;
import events.implementations.VSTimestampMonitorEvent;
import events.implementations.VSTimestampTriggeredEvent;
import events.implementations.VSVectorTimestampEvent;
import protocols.VSAbstractProtocol;

/**
 * Demonstration protocol showing how to use timestamp-triggered events.
 * This protocol sets up various timestamp conditions and monitors them.
 * 
 * Example scenarios:
 * - Log message when Lamport time reaches 10
 * - Change process color when vector clock element reaches threshold
 * - Trigger protocol action at specific timestamp conditions
 *
 * @author Paul C. Buetow
 */
public class VSTimestampDemoProtocol extends VSAbstractProtocol {
    
    public VSTimestampDemoProtocol() {
        super(HAS_ON_SERVER_START);
    }
    
    private VSTimestampMonitorEvent lamportMonitor;
    
    @Override
    public void onServerInit() {
        initInteger("lamport.target", 10, "Target Lamport time", 1, 100, "");
        initString("lamport.action", "Log message", "Action to perform");
        
        Vector<Integer> defaultVector = new Vector<>();
        defaultVector.add(5);
        defaultVector.add(3);
        defaultVector.add(7);
        initVector("vector.target", defaultVector, "Target vector clock values");
        
        initString("vector.condition", "GREATER_EQUAL", "Comparison operator (EQUAL, GREATER_THAN, LESS_THAN, GREATER_EQUAL, LESS_EQUAL)");
    }
    
    @Override
    public void onClientInit() {
        initInteger("lamport.target", 15, "Target Lamport time", 1, 100, "");
        initString("lamport.action", "Highlight process", "Action to perform");
        
        Vector<Integer> defaultVector = new Vector<>();
        defaultVector.add(3);
        defaultVector.add(8);
        defaultVector.add(2);
        initVector("vector.target", defaultVector, "Target vector clock values");
        
        initString("vector.condition", "EQUAL", "Comparison operator");
    }
    
    @Override
    public void onServerStart() {
        setupTimestampEvents();
    }
    
    @Override
    public void onClientStart() {
        setupTimestampEvents();
    }
    
    /**
     * Set up timestamp-triggered events based on configuration
     */
    private void setupTimestampEvents() {
        VSInternalProcess internalProcess = (VSInternalProcess) process;
        
        // Create monitors for timestamp events
        lamportMonitor = new VSTimestampMonitorEvent(1); // Check every time unit
        lamportMonitor.init(internalProcess);
        
        // Set up Lamport timestamp event
        setupLamportEvent();
        
        // Set up Vector timestamp event
        setupVectorEvent();
        
        // Start Lamport monitoring by scheduling the monitor
        VSTask monitorTask = new VSTask(internalProcess.getTime() + 1, 
                                       internalProcess, lamportMonitor, VSTask.LOCAL);
        internalProcess.getSimulatorCanvas().getTaskManager().addTask(monitorTask);
        
        // Vector monitoring will be triggered by clock changes, not time
        
        log("Timestamp demo protocol started - monitoring Lamport and Vector timestamp events");
    }
    
    /**
     * Configure Lamport timestamp event
     */
    private void setupLamportEvent() {
        long targetLamport = getLong("lamport.target");
        String action = getString("lamport.action");
        
        VSLamportTimestampEvent lamportEvent = new VSLamportTimestampEvent(
            targetLamport, 
            VSTimestampTriggeredEvent.ComparisonOperator.GREATER_EQUAL,
            action + " (Lamport >= " + targetLamport + ")",
            () -> {
                // Custom action when Lamport condition is met
                log("Lamport timestamp condition met! Executing: " + action);
                if (action.contains("Highlight") || action.contains("highlight")) {
                    ((VSInternalProcess) process).highlightOn();
                }
            }
        );
        
        lamportMonitor.addLamportEvent(lamportEvent);
    }
    
    /**
     * Configure Vector timestamp event
     */
    private void setupVectorEvent() {
        Vector<Integer> targetVectorInts = getVector("vector.target");
        String conditionStr = getString("vector.condition");
        
        // Convert Vector<Integer> to VSVectorTime
        VSVectorTime targetVector = new VSVectorTime(0);
        for (Integer val : targetVectorInts) {
            targetVector.add(val.longValue());
        }
        
        // Parse condition string
        VSTimestampTriggeredEvent.ComparisonOperator operator;
        try {
            operator = VSTimestampTriggeredEvent.ComparisonOperator.valueOf(conditionStr);
        } catch (IllegalArgumentException e) {
            operator = VSTimestampTriggeredEvent.ComparisonOperator.GREATER_EQUAL;
            log("Invalid condition '" + conditionStr + "', using GREATER_EQUAL");
        }
        
        VSVectorTimestampEvent vectorEvent = new VSVectorTimestampEvent(
            targetVector,
            operator,
            "Vector clock condition: " + targetVector + " " + operator,
            () -> {
                // Custom action when Vector condition is met
                log("Vector timestamp condition met! Current: " + 
                    ((VSInternalProcess) process).getVectorTime());
                ((VSInternalProcess) process).highlightOn();
            }
        );
        
        ((VSInternalProcess) process).getVectorClockMonitor().addVectorEvent(vectorEvent);
    }
    
    @Override
    public void onServerRecv(core.VSMessage message) {
        // Could add timestamp events based on received messages
        log("Server received message - current timestamps: L=" + 
            ((VSInternalProcess) process).getLamportTime() + 
            ", V=" + ((VSInternalProcess) process).getVectorTime());
    }
    
    @Override
    public void onClientRecv(core.VSMessage message) {
        // Could add timestamp events based on received messages
        log("Client received message - current timestamps: L=" + 
            ((VSInternalProcess) process).getLamportTime() + 
            ", V=" + ((VSInternalProcess) process).getVectorTime());
    }
    
    @Override
    public String toString() {
        return " [TimestampDemo]";
    }
    
    @Override
    public void onServerReset() {
        if (lamportMonitor != null) {
            lamportMonitor.stopMonitoring();
        }
        if (process instanceof VSInternalProcess) {
            ((VSInternalProcess) process).getVectorClockMonitor().clearVectorEvents();
        }
    }
    
    @Override
    public void onClientReset() {
        if (lamportMonitor != null) {
            lamportMonitor.stopMonitoring();
        }
        if (process instanceof VSInternalProcess) {
            ((VSInternalProcess) process).getVectorClockMonitor().clearVectorEvents();
        }
    }
    
    @Override
    public void onServerSchedule() {
        // No scheduled operations needed
    }
    
    @Override
    public void onClientSchedule() {
        // No scheduled operations needed
    }
    
    @Override
    protected String createShortname(String savedShortname) {
        return "TimestampDemo";
    }
}