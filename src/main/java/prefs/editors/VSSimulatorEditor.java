package prefs.editors;

import java.awt.event.ActionEvent;

import javax.swing.JButton;
import javax.swing.JPanel;

import prefs.VSPrefs;
import simulator.VSSimulator;
import simulator.VSSimulatorFrame;

/**
 * The class VSSimulatorEditor, is for editing a VSSimulator object.
 *
 * @author Paul C. Buetow
 */
public class VSSimulatorEditor extends VSAbstractBetterEditor {
    /** The constant OPENED_NEW_WINDOW */
    public static final boolean OPENED_NEW_WINDOW = true;

    /** The constant OPENED_NEW_TAB */
    public static final boolean OPENED_NEW_TAB = false;

    /** The simulator frame. */
    private VSSimulatorFrame simulatorFrame;

    /** The simulator. */
    private VSSimulator simulator;

    /** The TAKEOVE r_ button. */
    public static boolean TAKEOVER_BUTTON;

    /** The dont start new simulator. */
    private boolean dontStartNewSimulator;

    /** Open a new simulator window. */
    private boolean openedNewWindow;

    /**
     * Instantiates a new VSSimulatorEditor object.
     *
     * @param prefs the prefs
     * @param simulatorFrame the simulator frame
     * @param simulator the simulator
     */
    public VSSimulatorEditor(VSPrefs prefs, VSSimulatorFrame simulatorFrame,
                             VSSimulator simulator) {
        super(prefs, prefs, prefs.getString("lang.name")
              + " - " + prefs.getString("lang.prefs"));
        this.dontStartNewSimulator = true;//simulator != null;
        this.simulatorFrame = simulatorFrame;
        this.simulator = simulator;
    }

    /**
     * Instantiates a new VSSimulatorEditor object.
     *
     * @param prefs the prefs
     * @param simulatorFrame the simulator frame
     */
    public VSSimulatorEditor(VSPrefs prefs, VSSimulatorFrame simulatorFrame,
                             boolean openedNewWindow) {
        super(prefs, prefs, prefs.getString("lang.name")
              + " - " + prefs.getString("lang.prefs"));
        this.simulatorFrame = simulatorFrame;
        this.openedNewWindow = openedNewWindow;
    }

    /* (non-Javadoc)
     * @see prefs.editors.VSAbstractBetterEditor#addToButtonPanelFront(
     *	javax.swing.JPanel)
     */
    protected void addToButtonPanelFront(JPanel buttonPanel) {
        if (TAKEOVER_BUTTON) {
            TAKEOVER_BUTTON = false;
            JButton takeoverButton = new JButton(
                prefs.getString("lang.takeover"));
            takeoverButton.setMnemonic(prefs.getInteger("keyevent.takeover"));
            takeoverButton.addActionListener(this);
            buttonPanel.add(takeoverButton);
        }
    }

    /* (non-Javadoc)
     * @see prefs.editors.VSAbstractBetterEditor#actionPerformed(
     *	java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e) {
        String actionCommand = e.getActionCommand();

        if (actionCommand.equals(prefs.getString("lang.takeover"))) {
            savePrefs();

            if (simulator != null) {
                if (expertModeChanged())
                    simulator.fireExpertModeChanged();
                simulator.updateFromPrefs();
            }

        } else if (actionCommand.equals(prefs.getString("lang.cancel"))) {
            if (!dontStartNewSimulator && openedNewWindow)
                simulatorFrame.dispose();

        } else if (actionCommand.equals(prefs.getString("lang.ok"))) {
            savePrefs();
            if (expertModeChanged()) {
                if (simulator != null)
                    simulator.fireExpertModeChanged();
            }
            if (!dontStartNewSimulator)
                simulatorFrame.addSimulator(new VSSimulator(prefsToEdit,
                                            simulatorFrame));
            else if (simulator != null)
                simulator.updateFromPrefs();

        } else {
            super.actionPerformed(e);
        }
    }
}
