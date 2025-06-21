package testing;

import simulator.VSSimulatorFrame;
import prefs.VSPrefs;
import javax.swing.SwingUtilities;
import java.awt.Dimension;
import java.awt.Point;

/**
 * A minimal simulator frame for headless operation.
 * Creates a real frame but immediately hides it and moves it off-screen.
 */
public class DummySimulatorFrame extends VSSimulatorFrame {
    
    public DummySimulatorFrame(VSPrefs prefs) {
        super(prefs, null);  // null for relativeTo component
        
        // Make the frame as small as possible and move off-screen
        SwingUtilities.invokeLater(() -> {
            setSize(1, 1);
            setLocation(-1000, -1000);
            setVisible(false);
        });
    }
    
    @Override
    public void resetCurrentSimulator() {
        // Check if we have a current simulator before resetting
        if (getCurrentSimulator() != null) {
            // Only reset menu states, don't update GUI
            getCurrentSimulator().getMenuItemStates().setStart(true);
            getCurrentSimulator().getMenuItemStates().setPause(false);
            getCurrentSimulator().getMenuItemStates().setReset(false);
            getCurrentSimulator().getMenuItemStates().setReplay(false);
        }
    }
    
    @Override
    public void updateSimulatorMenu() {
        // Do nothing - no menu updates in headless mode
    }
    
    @Override
    public void setVisible(boolean visible) {
        // Always keep invisible
        super.setVisible(false);
    }
    
    @Override
    public void pack() {
        // Set minimal size instead of packing
        setSize(1, 1);
    }
    
    @Override
    public void toFront() {
        // Do nothing - don't bring to front
    }
    
    @Override
    public void repaint() {
        // Do nothing - no repainting needed
    }
    
    @Override
    public void addSimulator(simulator.VSSimulator simulator) {
        // Add simulator without triggering tab changes and painting
        if (getSimulators() != null) {
            getSimulators().add(simulator);
        }
        setCurrentSimulator(simulator);
    }
    
    protected void setCurrentSimulator(simulator.VSSimulator simulator) {
        try {
            java.lang.reflect.Field field = VSSimulatorFrame.class.getDeclaredField("currentSimulator");
            field.setAccessible(true);
            field.set(this, simulator);
        } catch (Exception e) {
            // Ignore errors
        }
    }
    
    protected java.util.Vector<simulator.VSSimulator> getSimulators() {
        try {
            java.lang.reflect.Field field = VSSimulatorFrame.class.getDeclaredField("simulators");
            field.setAccessible(true);
            return (java.util.Vector<simulator.VSSimulator>) field.get(this);
        } catch (Exception e) {
            return null;
        }
    }
}