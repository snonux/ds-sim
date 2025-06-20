package events;

import protocols.VSAbstractProtocol;
import events.implementations.VSTimestampTriggeredEvent;

/**
 * Sealed hierarchy representing different types of events in the simulator.
 * This uses Java 21's sealed classes feature to create a closed type hierarchy
 * that can be exhaustively matched in switch expressions.
 * 
 * <p>Example usage with pattern matching:</p>
 * <pre>{@code
 * VSEventType eventType = getEventType(event);
 * String description = switch (eventType) {
 *     case InternalEvent e -> "Internal: " + e.description();
 *     case ProtocolEvent e -> "Protocol: " + e.protocolName();
 *     case TimestampEvent e -> "Timestamp: " + e.condition();
 *     case SystemEvent e -> "System: " + e.eventName();
 * };
 * }</pre>
 * 
 * @since Java 21
 */
public sealed interface VSEventType 
    permits VSEventType.InternalEvent, 
            VSEventType.ProtocolEvent, 
            VSEventType.TimestampEvent,
            VSEventType.SystemEvent {
    
    /**
     * Internal events that don't affect timestamps.
     */
    record InternalEvent(String description) implements VSEventType {}
    
    /**
     * Protocol-related events for distributed algorithms.
     */
    record ProtocolEvent(String protocolName, boolean isServer) implements VSEventType {}
    
    /**
     * Timestamp-triggered events based on logical time conditions.
     */
    record TimestampEvent(String condition, long targetTime) implements VSEventType {}
    
    /**
     * System events like process crashes and recoveries.
     */
    record SystemEvent(String eventName, int processId) implements VSEventType {}
    
    /**
     * Utility method to categorize an event.
     * 
     * @param event the event to categorize
     * @return the appropriate event type
     */
    static VSEventType categorize(VSAbstractEvent event) {
        if (event.isInternalEvent()) {
            return new InternalEvent(event.getShortname());
        } else if (event instanceof VSAbstractProtocol protocol) {
            return new ProtocolEvent(protocol.getShortname(), protocol.isServer());
        } else if (event instanceof VSTimestampTriggeredEvent timestampEvent) {
            return new TimestampEvent(
                timestampEvent.getOperator().toString(), 
                timestampEvent.getTargetLamportTime()
            );
        } else {
            return new SystemEvent(event.getShortname(), 
                event.getProcess() != null ? event.getProcess().getProcessNum() : -1);
        }
    }
}