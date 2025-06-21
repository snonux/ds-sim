package testing;

import simulator.*;
import prefs.*;
import java.util.*;
import java.awt.*;
import javax.swing.*;

/**
 * A headless implementation of VSSimulatorFrame that avoids GUI initialization.
 * This frame is used for loading simulations in test/headless environments.
 */
public class HeadlessSimulatorFrame {
    private Vector<VSSimulator> simulators = new Vector<>();
    private VSPrefs prefs;
    private VSSimulator currentSimulator;
    
    public HeadlessSimulatorFrame(VSPrefs prefs) {
        this.prefs = prefs;
    }
    
    public void addSimulator(VSSimulator simulator) {
        simulators.add(simulator);
        currentSimulator = simulator;
    }
    
    public void removeSimulator(VSSimulator simulator) {
        simulators.remove(simulator);
        if (currentSimulator == simulator) {
            currentSimulator = simulators.isEmpty() ? null : simulators.lastElement();
        }
    }
    
    public void resetCurrentSimulator() {
        if (currentSimulator != null) {
            simulators.remove(currentSimulator);
            currentSimulator = null;
        }
    }
    
    public VSPrefs getPrefs() {
        return prefs;
    }
    
    public Vector<VSSimulator> getSimulators() {
        return simulators;
    }
    
    public VSSimulator getCurrentSimulator() {
        return currentSimulator;
    }
    
    public void setVisible(boolean visible) {
        // Do nothing - no GUI to show
    }
    
    public void pack() {
        // Do nothing - no GUI to pack
    }
    
    public void repaint() {
        // Do nothing - no GUI to repaint
    }
    
    public boolean isDisplayable() {
        return false;
    }
    
    public void dispose() {
        simulators.clear();
        currentSimulator = null;
    }
}