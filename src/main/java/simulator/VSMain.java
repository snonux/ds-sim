package simulator;

import java.awt.Component;
import java.lang.reflect.InvocationTargetException;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.UIManager;
import javax.swing.SwingUtilities;

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
    interface SimulatorFrameFactory {
        VSSimulatorFrame create(VSPrefs prefs, Component relativeTo);
    }

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
     * Resolves the initial simulation filename from the CLI arguments.
     *
     * @param args the arguments passed to main
     *
     * @return the first non-blank argument, or null if none was provided
     */
    static String resolveStartupSimulationFile(String[] args) {
        if (args == null || args.length == 0 || args[0] == null)
            return null;

        String filename = args[0].trim();
        return filename.isEmpty() ? null : filename;
    }

    static VSSimulatorFrame launchSimulatorFrame(VSPrefs prefs,
                                                 Component relativeTo,
                                                 String startupSimulationFile) {
        return launchSimulatorFrame(prefs, relativeTo, startupSimulationFile,
                                    new SimulatorFrameFactory() {
            public VSSimulatorFrame create(VSPrefs framePrefs,
                                           Component frameRelativeTo) {
                return new VSSimulatorFrame(framePrefs, frameRelativeTo);
            }
        });
    }

    static VSSimulatorFrame launchSimulatorFrame(VSPrefs prefs,
                                                 Component relativeTo,
                                                 String startupSimulationFile,
                                                 SimulatorFrameFactory factory) {
        Objects.requireNonNull(prefs, "prefs");
        Objects.requireNonNull(factory, "factory");

        AtomicReference<VSSimulatorFrame> frameRef =
            new AtomicReference<VSSimulatorFrame>();
        Runnable openWindow = new Runnable() {
            public void run() {
                VSSimulatorFrame simulatorFrame =
                    factory.create(prefs, relativeTo);
                frameRef.set(simulatorFrame);
                if (startupSimulationFile != null)
                    simulatorFrame.openAndStartSimulator(startupSimulationFile);
            }
        };

        runOnEventDispatchThread(openWindow);
        return frameRef.get();
    }

    static void runOnEventDispatchThread(Runnable action) {
        Objects.requireNonNull(action, "action");

        if (SwingUtilities.isEventDispatchThread()) {
            action.run();
            return;
        }

        try {
            SwingUtilities.invokeAndWait(action);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while launching UI",
                                            e);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException("Failed to launch UI",
                                            e.getCause());
        }
    }

    /**
     * The main method.
     *
     * @param args the arguments
     */
    public static void main(String[] args) {
        // Show splash screen
        VSSplashScreen splash = new VSSplashScreen();
        splash.showSplash();
        
        try {
            UIManager.setLookAndFeel(
                UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception e) { }

        Locale.setDefault(Locale.ENGLISH);
        javax.swing.JPopupMenu.setDefaultLightWeightPopupEnabled(false);
        VSPrefs prefs = VSDefaultPrefs.init();
        VSRegisteredEvents.init(prefs);
        VSMain.prefs = prefs;

        // Wait for splash screen to finish before showing main window
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        String startupSimulationFile = resolveStartupSimulationFile(args);
        launchSimulatorFrame(prefs, null, startupSimulationFile);
    }
}
