package simulator;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import core.VSInternalProcess;
import prefs.VSDefaultPrefs;
import prefs.VSPrefs;
import prefs.editors.VSEditorFrame;
import prefs.editors.VSSimulatorEditor;
import serialize.VSSerialize;
import utils.VSAboutFrame;
import utils.VSFrame;

/**
 * The class VSSimulatorFrame, an object of this class represents a window
 * of the simulator. The window can have several tabs. Each tab contains
 * an independent simulator.
 *
 * @author Paul C. Buetow
 */
public class VSSimulatorFrame extends VSFrame {
    /** The serial version uid */
    private static final long serialVersionUID = 1L;

    /** The pause item. */
    private JMenuItem pauseItem;

    /** The replay item. */
    private JMenuItem replayItem;

    /** The reset item. */
    private JMenuItem resetItem;

    /** The start item. */
    private JMenuItem startItem;

    /** The pause button. */
    private JButton pauseButton;

    /** The replay button. */
    private JButton replayButton;

    /** The reset button. */
    private JButton resetButton;

    /** The start button. */
    private JButton startButton;

    /** The menu edit. */
    private JMenu menuEdit;

    /** The menu file. */
    private JMenu menuFile;

    /** The close item. */
    private JMenuItem closeItem;

    /** The save item. */
    private JMenuItem saveItem;

    /** The save as item. */
    private JMenuItem saveAsItem;

    /** The menu simulator. */
    private JMenu menuSimulator;

    /** The tool bar. */
    private JToolBar toolBar;

    /** The prefs. */
    private VSPrefs prefs;

    /** The simulators. */
    private Vector<VSSimulator> simulators;

    /** The current simulator. */
    private VSSimulator currentSimulator;

    /** The tabbed pane. */
    private JTabbedPane tabbedPane;

    /** The action listener */
    private ActionListener actionListener;

    /**
     * Instantiates a new VSSimulatorFrame object.
     *
     * @param prefs the prefs
     * @param relativeTo the component to open the window relative to
     */
    public VSSimulatorFrame(VSPrefs prefs, Component relativeTo) {
        super(prefs.getString("lang.name"), relativeTo);
        this.prefs = prefs;
        this.simulators = new Vector<VSSimulator>();

        final VSPrefs finalPrefs = this.prefs;
        actionListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Object source = e.getSource();
                String sourceText = null;

                if (source instanceof JMenuItem)
                    sourceText = ((JMenuItem) source).getText();
                else
                    sourceText = ((ImageIcon) ((JButton) source).getIcon()).
                                 getDescription();

                if (sourceText.equals(
                            finalPrefs.getString("lang.simulator.close"))) {
                    removeCurrentSimulator();

                } else if (sourceText.equals(
                               finalPrefs.getString("lang.simulator.new"))) {
                    VSPrefs newPrefs = VSDefaultPrefs.init();
                    VSSimulatorEditor simulatorEditor =
                        new VSSimulatorEditor(newPrefs, VSSimulatorFrame.this,
                                              VSSimulatorEditor.OPENED_NEW_TAB);
                    new VSEditorFrame(newPrefs, VSSimulatorFrame.this,
                                      simulatorEditor);

                } else if (sourceText.equals(
                               finalPrefs.getString("lang.window.new"))) {
                    new VSMain(VSDefaultPrefs.init(),
                               VSSimulatorFrame.this);

                } else if (sourceText.equals(
                               finalPrefs.getString("lang.window.close"))) {
                    dispose();

                } else if (sourceText.equals(
                               finalPrefs.getString("lang.open"))) {
                    VSSerialize serialize = new VSSerialize();
                    serialize.openSimulator(VSSimulatorFrame.this);

                } else if (sourceText.equals(
                               finalPrefs.getString("lang.save"))) {
                    VSSimulatorVisualization simulatorVisualization =
                        currentSimulator.getSimulatorCanvas();
                    boolean flag = !simulatorVisualization.isPaused()
                                   && !simulatorVisualization.isResetted()
                                   && !simulatorVisualization.hasFinished();

                    if (flag)
                        pauseCurrentSimulator();

                    VSSerialize serialize = new VSSerialize();
                    serialize.saveSimulator(VSSerialize.LAST_FILENAME,
                                            currentSimulator);
                    if (flag)
                        startCurrentSimulator();

                } else if (sourceText.equals(
                               finalPrefs.getString("lang.saveas"))) {
                    VSSimulatorVisualization simulatorVisualization =
                        currentSimulator.getSimulatorCanvas();
                    boolean flag = !simulatorVisualization.isPaused()
                                   && !simulatorVisualization.isResetted()
                                   && !simulatorVisualization.hasFinished();
                    if (flag)
                        pauseCurrentSimulator();

                    VSSerialize serialize = new VSSerialize();
                    serialize.saveSimulator(currentSimulator);

                    if (flag)
                        startCurrentSimulator();

                } else if (sourceText.equals(
                               finalPrefs.getString("lang.about"))) {
                    new VSAboutFrame(finalPrefs, VSSimulatorFrame.this);

                } else if (sourceText.equals(
                               finalPrefs.getString("lang.quit"))) {
                    System.exit(0);

                } else if (sourceText.equals(
                               finalPrefs.getString("lang.start"))) {
                    startCurrentSimulator();

                } else if (sourceText.equals(
                               finalPrefs.getString("lang.pause"))) {
                    pauseCurrentSimulator();

                } else if (sourceText.equals(
                               finalPrefs.getString("lang.reset"))) {
                    resetCurrentSimulator();

                } else if (sourceText.equals(
                               finalPrefs.getString("lang.replay"))) {
                    VSMenuItemStates menuItemState =
                        currentSimulator.getMenuItemStates();
                    menuItemState.setStart(false);
                    menuItemState.setPause(true);
                    menuItemState.setReset(false);
                    menuItemState.setReplay(true);
                    currentSimulator.getSimulatorCanvas().reset();
                    currentSimulator.getSimulatorCanvas().play();
                    updateSimulatorMenu();
                }
            }
        };

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        setSize(prefs.getInteger("div.window.xsize"),
                prefs.getInteger("div.window.ysize"));

        setJMenuBar(createMenuBar());
        setLayout(new BorderLayout());
        setContentPane(createContentPane());
        setVisible(true);

        pauseButton.setEnabled(false);
        replayButton.setEnabled(false);
        resetButton.setEnabled(false);
        startButton.setEnabled(false);
        menuEdit.setEnabled(false);
        //menuFile.setEnabled(false);
        closeItem.setEnabled(false);
        saveItem.setEnabled(false);
        saveAsItem.setEnabled(false);
        menuSimulator.setEnabled(false);
    }

    /**
     * Creates the menu bar.
     *
     * @return the j menu bar
     */
    private JMenuBar createMenuBar() {
        /* File menu */
        menuFile = new JMenu(prefs.getString("lang.file"));
        menuFile.setMnemonic(prefs.getInteger("keyevent.file"));
        JMenuItem menuItem;

        menuItem = new JMenuItem(prefs.getString("lang.simulator.new"));
        menuItem.setAccelerator(KeyStroke.getKeyStroke(
                                    prefs.getInteger("keyevent.new"),
                                    ActionEvent.ALT_MASK));
        menuItem.addActionListener(actionListener);
        menuFile.add(menuItem);

        closeItem = new JMenuItem(
            prefs.getString("lang.simulator.close"));
        closeItem.setAccelerator(KeyStroke.getKeyStroke(
                                     prefs.getInteger("keyevent.close"),
                                     ActionEvent.ALT_MASK));
        closeItem.addActionListener(actionListener);
        menuFile.add(closeItem);

        menuFile.addSeparator();

        menuItem = new JMenuItem(prefs.getString("lang.window.new"));
        menuItem.addActionListener(actionListener);
        menuFile.add(menuItem);

        menuItem = new JMenuItem(prefs.getString("lang.window.close"));
        menuItem.addActionListener(actionListener);
        menuFile.add(menuItem);

        menuFile.addSeparator();

        menuItem = new JMenuItem(prefs.getString("lang.open"));
        menuItem.setAccelerator(KeyStroke.getKeyStroke(
                                    prefs.getInteger("keyevent.open"),
                                    ActionEvent.ALT_MASK));
        menuItem.addActionListener(actionListener);
        menuFile.add(menuItem);

        saveItem = new JMenuItem(prefs.getString("lang.save"));
        saveItem.setAccelerator(KeyStroke.getKeyStroke(
                                    prefs.getInteger("keyevent.save"),
                                    ActionEvent.ALT_MASK));
        saveItem.addActionListener(actionListener);
        menuFile.add(saveItem);

        saveAsItem = new JMenuItem(prefs.getString("lang.saveas"));
        saveAsItem.setAccelerator(KeyStroke.getKeyStroke(
                                      prefs.getInteger("keyevent.saveas"),
                                      ActionEvent.ALT_MASK));
        saveAsItem.addActionListener(actionListener);
        menuFile.add(saveAsItem);

        menuFile.addSeparator();

        menuItem = new JMenuItem(prefs.getString("lang.about"));
        menuItem.addActionListener(actionListener);
        menuFile.add(menuItem);

        menuItem = new JMenuItem(prefs.getString("lang.quit"));
        menuItem.addActionListener(actionListener);
        menuFile.add(menuItem);

        /* Edit menu */
        menuEdit = new JMenu(
            prefs.getString("lang.edit"));
        menuEdit.setMnemonic(prefs.getInteger("keyevent.edit"));
        updateEditMenu();

        /* Simulator menu */
        toolBar = new JToolBar();
        menuSimulator = new JMenu(
            prefs.getString("lang.simulator"));
        menuSimulator.setMnemonic(prefs.getInteger("keyevent.simulator"));

        resetItem = new JMenuItem(prefs.getString("lang.reset"));
        resetItem.setAccelerator(KeyStroke.getKeyStroke(
                                     prefs.getInteger("keyevent.reset"),
                                     ActionEvent.ALT_MASK));
        resetItem.addActionListener(actionListener);
        resetItem.setEnabled(false);
        menuSimulator.add(resetItem);
        resetButton = new JButton(getImageIcon("reset.png",
                                               prefs.getString("lang.reset")));
        resetButton.addActionListener(actionListener);
        toolBar.add(resetButton);

        replayItem = new JMenuItem(
            prefs.getString("lang.replay"));
        replayItem.setAccelerator(KeyStroke.getKeyStroke(
                                      prefs.getInteger("keyevent.replay"),
                                      ActionEvent.ALT_MASK));
        replayItem.addActionListener(actionListener);
        replayItem.setEnabled(false);
        menuSimulator.add(replayItem);
        replayButton = new JButton(
            getImageIcon("replay.png", prefs.getString("lang.replay")));
        replayButton.addActionListener(actionListener);
        toolBar.add(replayButton);

        pauseItem = new JMenuItem(prefs.getString("lang.pause"));
        pauseItem.setAccelerator(KeyStroke.getKeyStroke(
                                     prefs.getInteger("keyevent.pause"),
                                     ActionEvent.ALT_MASK));
        pauseItem.addActionListener(actionListener);
        menuSimulator.add(pauseItem);
        pauseItem.setEnabled(false);
        pauseButton = new JButton(getImageIcon("pause.png",
                                               prefs.getString("lang.pause")));
        pauseButton.addActionListener(actionListener);
        toolBar.add(pauseButton);

        startItem = new JMenuItem(prefs.getString("lang.start"));
        startItem.setAccelerator(KeyStroke.getKeyStroke(
                                     prefs.getInteger("keyevent.start"),
                                     ActionEvent.ALT_MASK));
        startItem.addActionListener(actionListener);
        menuSimulator.add(startItem);
        startButton = new JButton(getImageIcon("start.png",
                                               prefs.getString("lang.start")));
        startButton.addActionListener(actionListener);
        toolBar.add(startButton);


        JMenuBar mainMenuBar = new JMenuBar();
        mainMenuBar.add(menuFile);
        mainMenuBar.add(menuEdit);
        mainMenuBar.add(menuSimulator);

        return mainMenuBar;
    }

    /**
     * Creates the content pane.
     *
     * @return the container
     */
    private Container createContentPane() {
        Container pane = getContentPane();
        tabbedPane = new JTabbedPane(JTabbedPane.BOTTOM,
                                     JTabbedPane.SCROLL_TAB_LAYOUT);

        tabbedPane.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent ce) {
                JTabbedPane pane = (JTabbedPane) ce.getSource();
                currentSimulator = (VSSimulator) pane.getSelectedComponent();
                if (currentSimulator != null) {
                    currentSimulator.getSimulatorCanvas().paint();
                    updateEditMenu();
                    updateSimulatorMenu();
                }
            }
        });

        pane.add(toolBar, BorderLayout.PAGE_START);
        pane.add(tabbedPane, BorderLayout.CENTER);

        return pane;
    }

    /**
     * Updates the edit menu. Called if another simulator tab has been selected
     * or if processes have been added or removed.
     */
    public void updateEditMenu() {
        menuEdit.removeAll();

        JMenuItem globalPrefsItem = new JMenuItem(
            prefs.getString("lang.prefs"));

        globalPrefsItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                VSPrefs simulatorPrefs = currentSimulator.getPrefs();
                VSSimulatorEditor.TAKEOVER_BUTTON = true;
                VSSimulatorEditor simulatorEditor = new VSSimulatorEditor(
                    simulatorPrefs, VSSimulatorFrame.this, currentSimulator);
                new VSEditorFrame(prefs, VSSimulatorFrame.this,
                                  simulatorEditor);
            }
        });

        menuEdit.add(globalPrefsItem);
        menuEdit.addSeparator();

        if (currentSimulator == null)
            return;

        String processString = prefs.getString("lang.process");
        ArrayList<VSInternalProcess> arr =
            currentSimulator.getSimulatorCanvas().getProcessesArray();

        //int numProcesses = arr.size();
        int processNum = 0;

        for (VSInternalProcess process : arr) {
            int processID = process.getProcessID();
            JMenuItem processItem = new JMenuItem(processString + " " +
                                                  processID);
            if (processNum < 9)
                processItem.setAccelerator(
                    KeyStroke.getKeyStroke(0x31+processNum,
                                           ActionEvent.ALT_MASK));
            final int finalProcessNum = processNum++;
            processItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    currentSimulator.getSimulatorCanvas().editProcess(
                        finalProcessNum);
                }
            });
            menuEdit.add(processItem);
        }
    }

    /**
     * Updates the simulator menu. Called if the simulator state has changed
     * (e.g. start/play/stop/replay etc)
     */
    public synchronized void updateSimulatorMenu() {
        VSMenuItemStates menuItemState = currentSimulator.getMenuItemStates();

        pauseItem.setEnabled(menuItemState.getPause());
        replayItem.setEnabled(menuItemState.getReplay());
        resetItem.setEnabled(menuItemState.getReset());
        startItem.setEnabled(menuItemState.getStart());

        pauseButton.setEnabled(menuItemState.getPause());
        replayButton.setEnabled(menuItemState.getReplay());
        resetButton.setEnabled(menuItemState.getReset());
        startButton.setEnabled(menuItemState.getStart());
    }

    /* (non-Javadoc)
     * @see java.awt.Window#dispose()
     */
    public void dispose() {
        synchronized (simulators) {
            for (VSSimulator simulator : simulators)
                simulator.getSimulatorCanvas().stopThread();
        }
        super.dispose();
    }

    /**
     * Adds the simulator.
     *
     * @param simulator the simulator
     */
    public void addSimulator(VSSimulator simulator) {
        simulator.setLayout(new GridLayout(1, 1, 3, 3));
        simulator.setMinimumSize(new Dimension(0, 0));
        simulator.setMaximumSize(new Dimension(0, 0));

        simulators.add(simulator);
        tabbedPane.addTab(prefs.getString("lang.simulator")
                          + " " + simulator.getSimulatorNum(), simulator);
        tabbedPane.setSelectedComponent(simulator);

        if (simulators.size() == 1) {
            menuEdit.setEnabled(true);
            //menuFile.setEnabled(true);
            closeItem.setEnabled(true);
            saveItem.setEnabled(true);
            saveAsItem.setEnabled(true);
            menuSimulator.setEnabled(true);
        }
    }

    /**
     * Removes the simulator.
     *
     * @param simulatorToRemove the simulator to remove
     */
    public void removeSimulator(VSSimulator simulatorToRemove) {
        if (simulators.size() == 1) {
            pauseButton.setEnabled(false);
            replayButton.setEnabled(false);
            resetButton.setEnabled(false);
            startButton.setEnabled(false);
            menuEdit.setEnabled(false);
            //menuFile.setEnabled(false);
            closeItem.setEnabled(false);
            saveItem.setEnabled(false);
            saveAsItem.setEnabled(false);
            menuSimulator.setEnabled(false);
        }

        simulators.remove(simulatorToRemove);
        tabbedPane.remove(simulatorToRemove);
        simulatorToRemove.getSimulatorCanvas().stopThread();
    }

    /**
     * Removes the current simulator.
     */
    private void removeCurrentSimulator() {
        removeSimulator(currentSimulator);
    }

    /**
     * Gets the current simulator.
     *
     * @return the current simulator
     */
    public VSSimulator getCurrentSimulator() {
        return currentSimulator;
    }

    /**
     * Resets the current simulator
     */
    public void resetCurrentSimulator() {
        if (currentSimulator == null)
            return;

        VSMenuItemStates menuItemState =
            currentSimulator.getMenuItemStates();
        menuItemState.setStart(true);
        menuItemState.setPause(false);
        menuItemState.setReset(false);
        menuItemState.setReplay(false);
        currentSimulator.getSimulatorCanvas().reset();
        updateSimulatorMenu();
    }

    /**
     * Starts the current simulator
     */
    public void startCurrentSimulator() {
        VSMenuItemStates menuItemState =
            currentSimulator.getMenuItemStates();
        menuItemState.setStart(false);
        menuItemState.setPause(true);
        menuItemState.setReset(false);
        menuItemState.setReplay(true);
        currentSimulator.getSimulatorCanvas().play();
        updateSimulatorMenu();
    }

    /**
     * Pauses the current simulator
     */
    public void pauseCurrentSimulator() {
        VSMenuItemStates menuItemState =
            currentSimulator.getMenuItemStates();
        menuItemState.setStart(true);
        menuItemState.setPause(false);
        menuItemState.setReset(true);
        menuItemState.setReplay(true);
        currentSimulator.getSimulatorCanvas().pause();
        updateSimulatorMenu();
    }

    /**
     * Gets the image icon.
     *
     * @param name the name
     * @param descr the descr
     *
     * @return the image icon
     */
    private ImageIcon getImageIcon(String name, String descr) {
        java.net.URL imageURL = getClass().getResource("/icons/"+name);

        if (imageURL == null)
            return new ImageIcon("icons/"+name, descr);

        return new ImageIcon(imageURL, descr);
    }

    /**
     * Gets the prefs.
     *
     * @return the prefs
     */
    public VSPrefs getPrefs() {
        return prefs;
    }
}
