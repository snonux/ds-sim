package simulator;

import java.awt.Component;
import java.util.Locale;

import javax.swing.UIManager;

import events.VSRegisteredEvents;
import prefs.VSDefaultPrefs;
import prefs.VSPrefs;

/**
 * The class VSMain. This class contains the static main method. The simulator
 * starts here!
 *
 * @author Paul C. Buetow
 */
public class VSMain {
    /** The global preferences */
    public static VSPrefs prefs;

    /**
     * Instantiates a new VSMain object.
     *
     * @param prefs the prefs
     */
    public VSMain(VSPrefs prefs) {
        init(prefs, null);
    }

    /**
     * Instantiates a new VSMain object
     *
     * @param prefs the prefs
     * @param relativeTo the component to open the window relative to
     */
    public VSMain(VSPrefs prefs, Component relativeTo) {
        init(prefs, relativeTo);
    }

    /**
     * Inits the VSMain object.
     *
     * @param prefs the prefs
     * @param relativeTo the component to open the window relative to
     */
    private void init(VSPrefs prefs, Component relativeTo) {
        //VSSimulatorFrame simulatorFrame =
        VSMain.prefs = prefs;
        new VSSimulatorFrame(prefs, relativeTo);
    }

    /**
     * The main method.
     *
     * @param args the arguments
     */
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(
                UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception e) { }

        Locale.setDefault(Locale.GERMAN);
        javax.swing.JPopupMenu.setDefaultLightWeightPopupEnabled(false);
        VSPrefs prefs = VSDefaultPrefs.init();
        VSRegisteredEvents.init(prefs);
        new VSMain(prefs);
    }
}
