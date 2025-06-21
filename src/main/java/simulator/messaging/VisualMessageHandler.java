package simulator.messaging;

import core.VSMessage;
import simulator.VSSimulatorVisualization;

/**
 * Visual implementation of MessageHandler that delegates to the
 * existing VSSimulatorVisualization for backward compatibility.
 */
public class VisualMessageHandler implements MessageHandler {
    private final VSSimulatorVisualization visualization;
    
    public VisualMessageHandler(VSSimulatorVisualization visualization) {
        this.visualization = visualization;
    }
    
    @Override
    public void handleMessage(VSMessage message) {
        // Delegate to existing visualization
        visualization.sendMessage(message);
    }
    
    @Override
    public void visualizeMessage(VSMessage message) {
        // Already handled by visualization.sendMessage()
    }
    
    @Override
    public void setNetworkDelay(long delay) {
        // Handled by visualization preferences
    }
    
    @Override
    public void setNetworkVariability(long variability) {
        // Handled by visualization preferences
    }
}