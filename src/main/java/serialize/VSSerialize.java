package serialize;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;

import javax.swing.JFileChooser;

import prefs.VSDefaultPrefs;
import prefs.VSPrefs;
import prefs.VSSerializablePrefs;
import simulator.VSSimulator;
import simulator.VSSimulatorFrame;

/**
 * The class VSSerialize, this class helps do serialize/deserialize a saved
 * simulator!
 *
 * @author Paul C. Buetow
 */
public final class VSSerialize {
    /** True if debug mode of serialization/deserialization */
    public static final boolean DEBUG = true;

    /** The last filename used for saveing/opening*/
    public static volatile String LAST_FILENAME = null;

    /** For temp object storage */
    private static HashMap<String,Object> objects;

    /** Holds the current VSSerialize object */
    private static VSSerialize serialize;

    /**
     * Creates the VSSerialize object.
     */
    public VSSerialize() {
        init();
    }

    /**
     * Initializes the helper.
     */
    private void init() {
        serialize = this;
        objects = new HashMap<String,Object>();
    }

    /**
     * Gets the current VSSerialize object.
     *
     * @return The current serialize object
     */
    public static VSSerialize getSerialize() {
        return serialize;
    }

    /**
     * Sets an object.
     *
     * @param key The object key
     * @param object The object itself
     */
    public void setObject(String key, Object object) {
        if (DEBUG)
            System.out.println("setObject("+key+")");

        objects.put(key, object);
    }

    /**
     * Sets an object.
     *
     * @param num The object number
     * @param key The object key
     * @param object The object itself
     */
    public void setObject(int num, String key, Object object) {
        if (DEBUG)
            System.out.println("setObject("+key+":"+num+")");

        objects.put(key + ":" + num, object);
    }

    /**
     * Checks if an object exists.
     *
     * @param num The object number
     * @param key The object key
     *
     * @return true, if the object exists. false, if the object does not exist
     */
    public boolean objectExists(int num, String key) {
        return null != objects.get(key + ":" + num);
    }

    /**
     * Gets an object.
     *
     * @param num The object number
     * @param key The object key
     *
     * @return The object itself
     */
    public Object getObject(int num, String key) {
        Object object = objects.get(key + ":" + num);

        if (object == null) {
            System.err.println("No such deserialization helper key " + key + ":" + num);
            System.exit(1);
        }

        return object;
    }

    /**
     * Gets an object.
     *
     * @param key The object key
     *
     * @return The object itself
     */
    public Object getObject(String key) {
        Object object = objects.get(key);

        if (object == null) {
            System.err.println("No such deserialization helper key " + key);
            System.exit(1);
        }

        return object;
    }

    /**
     * Saves the given simulator to the given filename.
     *
     * @param filename The filename
     * @param simulator The simulator
     */
    public void saveSimulator(String filename, VSSimulator simulator) {
        if (filename == null) {
            saveSimulator(simulator);
            return;
        }

        LAST_FILENAME = filename;

        try {
            FileOutputStream fileOutputStream =
                new FileOutputStream(filename);
            ObjectOutputStream objectOutputStream =
                new ObjectOutputStream(fileOutputStream);

            VSSerializablePrefs prefs = (VSSerializablePrefs) simulator.getPrefs();
            prefs.serialize(this, objectOutputStream);
            simulator.serialize(this, objectOutputStream);

        } catch (IOException e) {
            //e.printStackTrace();

        } finally {
            //objectOutputStream.close();
        }
    }

    /**
     * Saves the given simulator to a file choosen by the file chooser.
     *
     * @param simulator The simulator
     */
    public void saveSimulator(VSSimulator simulator) {
        VSPrefs prefs = simulator.getPrefs();
        VSSimulatorFrame simulatorFrame = simulator.getSimulatorFrame();

        String saveText = prefs.getString("lang.save");
        JFileChooser fileChooser = new JFileChooser(new File(".").getPath());
        fileChooser.setMultiSelectionEnabled(false);
        fileChooser.addChoosableFileFilter(createFileFilter(prefs));
        fileChooser.setApproveButtonText(saveText);

        if (fileChooser.showOpenDialog(simulatorFrame) ==
                JFileChooser.APPROVE_OPTION) {
            String fileName = fileChooser.getSelectedFile().getAbsolutePath();
            System.out.println(saveText + ": " + fileName);
            saveSimulator(fileName, simulator);
        }
    }

    /**
     * Opens a simulator from the given filename.
     *
     * @param filename The filename.
     * @param simulatorFrame The simulator frame
     *
     * @return The simulator object, and null if no success
     */
    public VSSimulator openSimulator(String filename,
                                     VSSimulatorFrame simulatorFrame) {
        VSSimulator simulator = null;
        simulatorFrame.resetCurrentSimulator();
        LAST_FILENAME = filename;

        try {
            FileInputStream fileInputStream = new FileInputStream(filename);
            ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);

            VSDefaultPrefs prefs = new VSDefaultPrefs();
            prefs.deserialize(this, objectInputStream);
            prefs.addWithDefaults();

            this.setObject("prefs", prefs);

            simulator = new VSSimulator(prefs, simulatorFrame);
            simulatorFrame.addSimulator(simulator);
            simulator.deserialize(this, objectInputStream);

        } catch (Exception e) {
            e.printStackTrace();

        } finally {
            //objectInputStream.close();
        }

        return simulator;
    }

    /**
     * Opens a simulator from a file selected from a file chooser.
     *
     * @param simulatorFrame The simulator frame
     *
     * @return The simulator object, and null if no success
     */
    public VSSimulator openSimulator(VSSimulatorFrame simulatorFrame) {
        VSPrefs prefs = simulatorFrame.getPrefs();

        String openText = prefs.getString("lang.open");
        JFileChooser fileChooser = new JFileChooser(
            new File(".").getPath());
        fileChooser.setMultiSelectionEnabled(false);
        fileChooser.addChoosableFileFilter(createFileFilter(prefs));
        fileChooser.setApproveButtonText(openText);

        if (fileChooser.showOpenDialog(simulatorFrame) ==
                JFileChooser.APPROVE_OPTION) {
            String fileName = fileChooser.getSelectedFile().getAbsolutePath();
            System.out.println(openText + ": " + fileName);
            return openSimulator(fileName, simulatorFrame);
        }

        return null;
    }

    /**
     * Creates a file filter for the file choosers
     *
     * @param prefs The default prefs
     */
    private javax.swing.filechooser.FileFilter createFileFilter(
        final VSPrefs prefs) {
        return new javax.swing.filechooser.FileFilter() {
            public boolean accept(File file) {
                if (file.isDirectory())
                    return true;
                return file.getName().toLowerCase().endsWith(".dat");
            }

            public String getDescription() {
                return prefs.getString("lang.dat");
            }
        };
    }

}
