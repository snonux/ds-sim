package simulator.messaging;

import core.VSMessage;
import simulator.engine.SimulationEngine;

/**
 * Headless implementation of MessageHandler that processes messages
 * without any GUI visualization.
 */
public class HeadlessMessageHandler implements MessageHandler {
    private final SimulationEngine engine;
    private long networkDelay = 100;
    private long networkVariability = 0;
    
    public HeadlessMessageHandler(SimulationEngine engine) {
        this.engine = engine;
    }
    
    @Override
    public void handleMessage(VSMessage message) {
        // Just send to engine, no visualization
        engine.sendMessage(message);
    }
    
    @Override
    public void visualizeMessage(VSMessage message) {
        // No-op in headless mode
    }
    
    @Override
    public void setNetworkDelay(long delay) {
        this.networkDelay = delay;
    }
    
    @Override
    public void setNetworkVariability(long variability) {
        this.networkVariability = variability;
    }
}