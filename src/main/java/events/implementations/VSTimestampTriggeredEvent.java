package events.implementations;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import core.VSInternalProcess;
import core.time.VSLamportTime;
import core.time.VSVectorTime;
import events.VSAbstractEvent;
import events.VSCopyableEvent;
import serialize.VSSerialize;

/**
 * Abstract base class for timestamp-triggered events that fire when specific
 * Lamport or vector clock conditions are met.
 * 
 * <p>This class provides the foundation for creating events that trigger based on
 * timestamp conditions. Subclasses can define events that fire when:</p>
 * <ul>
 *   <li>Lamport time reaches a specific value</li>
 *   <li>Vector clock matches certain conditions</li>
 *   <li>Custom timestamp comparisons are satisfied</li>
 * </ul>
 * 
 * <p>Events can use various comparison operators (equal, greater than, less than, etc.)
 * and will only trigger once when their condition is first met.</p>
 * 
 * @see VSLamportTimestampEvent
 * @see VSVectorTimestampEvent
 * @see VSTimestampMonitorEvent
 * @author Paul C. Buetow
 */
public abstract class VSTimestampTriggeredEvent extends VSAbstractEvent implements VSCopyableEvent {
    
    public enum TimestampType {
        LAMPORT,
        VECTOR
    }
    
    public enum ComparisonOperator {
        EQUAL,
        GREATER_THAN,
        LESS_THAN,
        GREATER_EQUAL,
        LESS_EQUAL
    }
    
    protected TimestampType timestampType;
    protected ComparisonOperator operator;
    protected boolean hasTriggered;
    
    protected long targetLamportTime;
    protected VSVectorTime targetVectorTime;
    
    /**
     * Constructor for Lamport timestamp events
     */
    public VSTimestampTriggeredEvent(long targetLamport, ComparisonOperator op) {
        this.timestampType = TimestampType.LAMPORT;
        this.targetLamportTime = targetLamport;
        this.operator = op;
        this.hasTriggered = false;
    }
    
    /**
     * Constructor for Vector timestamp events
     */
    public VSTimestampTriggeredEvent(VSVectorTime targetVector, ComparisonOperator op) {
        this.timestampType = TimestampType.VECTOR;
        this.targetVectorTime = targetVector.getCopy();
        this.operator = op;
        this.hasTriggered = false;
    }
    
    /**
     * Default constructor for serialization
     */
    public VSTimestampTriggeredEvent() {
        this.hasTriggered = false;
    }
    
    @Override
    public void onInit() {
        setClassname(getClass().getName());
    }
    
    @Override
    public void onStart() {
        if (hasTriggered) {
            return;
        }
        
        VSInternalProcess internalProcess = (VSInternalProcess) process;
        boolean conditionMet = false;
        
        if (timestampType == TimestampType.LAMPORT) {
            conditionMet = checkLamportCondition(internalProcess);
        } else if (timestampType == TimestampType.VECTOR) {
            conditionMet = checkVectorCondition(internalProcess);
        }
        
        if (conditionMet) {
            hasTriggered = true;
            onTimestampReached();
        }
    }
    
    /**
     * Check timestamp condition without triggering the event.
     * Used by monitoring systems to test conditions.
     */
    public boolean checkCondition(VSInternalProcess process) {
        if (hasTriggered) {
            return false;
        }
        
        if (timestampType == TimestampType.LAMPORT) {
            return checkLamportCondition(process);
        } else if (timestampType == TimestampType.VECTOR) {
            return checkVectorCondition(process);
        }
        
        return false;
    }
    
    /**
     * Check if Lamport timestamp condition is met
     */
    protected boolean checkLamportCondition(VSInternalProcess process) {
        long currentLamport = process.getLamportTime();
        
        return switch (operator) {
            case EQUAL -> currentLamport == targetLamportTime;
            case GREATER_THAN -> currentLamport > targetLamportTime;
            case LESS_THAN -> currentLamport < targetLamportTime;
            case GREATER_EQUAL -> currentLamport >= targetLamportTime;
            case LESS_EQUAL -> currentLamport <= targetLamportTime;
        };
    }
    
    /**
     * Check if Vector timestamp condition is met
     */
    protected boolean checkVectorCondition(VSInternalProcess process) {
        VSVectorTime currentVector = process.getVectorTime();
        
        if (currentVector == null || targetVectorTime == null) {
            return false;
        }
        
        return switch (operator) {
            case EQUAL -> vectorTimesEqual(currentVector, targetVectorTime);
            case GREATER_THAN -> vectorTimeGreater(currentVector, targetVectorTime, false);
            case LESS_THAN -> vectorTimeGreater(targetVectorTime, currentVector, false);
            case GREATER_EQUAL -> vectorTimeGreater(currentVector, targetVectorTime, true);
            case LESS_EQUAL -> vectorTimeGreater(targetVectorTime, currentVector, true);
        };
    }
    
    /**
     * Check if two vector times are equal
     */
    protected boolean vectorTimesEqual(VSVectorTime v1, VSVectorTime v2) {
        int maxSize = Math.max(v1.size(), v2.size());
        
        for (int i = 0; i < maxSize; i++) {
            long val1 = i < v1.size() ? v1.get(i) : 0;
            long val2 = i < v2.size() ? v2.get(i) : 0;
            
            if (val1 != val2) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Check if v1 > v2 (or >= if allowEqual is true) using vector clock ordering
     */
    protected boolean vectorTimeGreater(VSVectorTime v1, VSVectorTime v2, boolean allowEqual) {
        int maxSize = Math.max(v1.size(), v2.size());
        boolean hasGreater = false;
        
        for (int i = 0; i < maxSize; i++) {
            long val1 = i < v1.size() ? v1.get(i) : 0;
            long val2 = i < v2.size() ? v2.get(i) : 0;
            
            if (val1 < val2) {
                return false;
            } else if (val1 > val2) {
                hasGreater = true;
            }
        }
        
        return hasGreater || (allowEqual && vectorTimesEqual(v1, v2));
    }
    
    /**
     * This method is called when the timestamp condition is met.
     * Subclasses should override this to define the actual behavior.
     */
    protected abstract void onTimestampReached();
    
    @Override
    protected String createShortname(String savedShortname) {
        return "TimestampTrigger";
    }
    
    @Override
    public void initCopy(VSAbstractEvent copy) {
        if (copy instanceof VSTimestampTriggeredEvent) {
            VSTimestampTriggeredEvent copyEvent = (VSTimestampTriggeredEvent) copy;
            copyEvent.timestampType = this.timestampType;
            copyEvent.operator = this.operator;
            copyEvent.targetLamportTime = this.targetLamportTime;
            copyEvent.hasTriggered = this.hasTriggered;
            
            if (this.targetVectorTime != null) {
                copyEvent.targetVectorTime = this.targetVectorTime.getCopy();
            }
        }
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(" [TimestampTrigger: ");
        
        if (timestampType == TimestampType.LAMPORT) {
            sb.append("Lamport ").append(operator).append(" ").append(targetLamportTime);
        } else {
            sb.append("Vector ").append(operator).append(" ").append(targetVectorTime);
        }
        
        if (hasTriggered) {
            sb.append(" (TRIGGERED)");
        }
        
        sb.append("]");
        return sb.toString();
    }
    
    // Getters and setters
    public TimestampType getTimestampType() {
        return timestampType;
    }
    
    public ComparisonOperator getOperator() {
        return operator;
    }
    
    public long getTargetLamportTime() {
        return targetLamportTime;
    }
    
    public VSVectorTime getTargetVectorTime() {
        return targetVectorTime != null ? targetVectorTime.getCopy() : null;
    }
    
    public boolean hasTriggered() {
        return hasTriggered;
    }
    
    public void reset() {
        hasTriggered = false;
    }
    
    @Override
    public synchronized void serialize(VSSerialize serialize,
                                     ObjectOutputStream objectOutputStream)
    throws IOException {
        super.serialize(serialize, objectOutputStream);
        
        if (VSSerialize.DEBUG)
            System.out.println("Serializing: VSTimestampTriggeredEvent; id="+getID());
        
        /** For later backwards compatibility, to add more stuff */
        objectOutputStream.writeObject(Boolean.valueOf(false));
        
        objectOutputStream.writeObject(timestampType);
        objectOutputStream.writeObject(operator);
        objectOutputStream.writeObject(Long.valueOf(targetLamportTime));
        objectOutputStream.writeObject(Boolean.valueOf(hasTriggered));
        
        // Serialize vector time if present
        boolean hasVectorTime = (targetVectorTime != null);
        objectOutputStream.writeObject(Boolean.valueOf(hasVectorTime));
        if (hasVectorTime) {
            objectOutputStream.writeObject(targetVectorTime);
        }
        
        /** For later backwards compatibility, to add more stuff */
        objectOutputStream.writeObject(Boolean.valueOf(false));
    }
    
    @Override
    public synchronized void deserialize(VSSerialize serialize,
                                       ObjectInputStream objectInputStream)
    throws IOException, ClassNotFoundException {
        super.deserialize(serialize, objectInputStream);
        
        if (VSSerialize.DEBUG)
            System.out.print("Deserializing: VSTimestampTriggeredEvent ");
        
        /** For later backwards compatibility, to add more stuff */
        objectInputStream.readObject();
        
        timestampType = (TimestampType) objectInputStream.readObject();
        operator = (ComparisonOperator) objectInputStream.readObject();
        targetLamportTime = ((Long) objectInputStream.readObject()).longValue();
        hasTriggered = ((Boolean) objectInputStream.readObject()).booleanValue();
        
        // Deserialize vector time if present
        boolean hasVectorTime = ((Boolean) objectInputStream.readObject()).booleanValue();
        if (hasVectorTime) {
            targetVectorTime = (VSVectorTime) objectInputStream.readObject();
        }
        
        /** For later backwards compatibility, to add more stuff */
        objectInputStream.readObject();
    }
}