package simulator.messaging;

import core.VSMessage;

/**
 * Interface for handling message delivery in the simulation.
 * Implementations can choose to visualize messages or just deliver them.
 */
public interface MessageHandler {
    
    /**
     * Handle a message that needs to be sent.
     * @param message The message to handle
     */
    void handleMessage(VSMessage message);
    
    /**
     * Visualize a message being sent (optional operation).
     * @param message The message to visualize
     */
    void visualizeMessage(VSMessage message);
    
    /**
     * Set the network delay for message delivery.
     * @param delay Base delay in milliseconds
     */
    void setNetworkDelay(long delay);
    
    /**
     * Set the network delay variability.
     * @param variability Variability in milliseconds
     */
    void setNetworkVariability(long variability);
}