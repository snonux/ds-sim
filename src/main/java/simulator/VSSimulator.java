package simulator;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Vector;

import javax.swing.AbstractButton;
import javax.swing.AbstractCellEditor;
import javax.swing.BoxLayout;
import javax.swing.ButtonModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;

import core.VSInternalProcess;
import core.VSTask;
import core.VSTaskManager;
import events.VSRegisteredEvents;
import exceptions.VSNegativeNumberException;
import prefs.VSPrefs;
import prefs.editors.VSProcessEditor;
import serialize.VSSerializable;
import serialize.VSSerialize;

/**
 * The class VSSimulator, an object of this class represents a whole simulator.
 * It may be, that several parallel simulators exist. They are independent
 * fron each other.
 *
 * @author Paul C. Buetow
 */
public class VSSimulator extends JPanel implements VSSerializable {
    /** The serial version uid */
    private static final long serialVersionUID = 1L;

    /** The global text fields. */
    private ArrayList<String> globalTextFields;

    /** The local text fields. */
    private ArrayList<String> localTextFields;

    /** The create tasks array list. */
    private ArrayList<VSCreateTask> createTasks;

    /** The filter active check box. */
    private JCheckBox filterActiveCheckBox;

    /** The lamport active check box. */
    private JCheckBox lamportActiveCheckBox;

    /** The vector time active check box. */
    private JCheckBox vectorTimeActiveCheckBox;

    /** The global pid combo box. */
    private JComboBox<String> globalPIDComboBox;

    /** The local pid combo box. */
    private JComboBox<String> localPIDComboBox;

    /** The processes combo box. */
    private JComboBox<String> processesComboBox;

    /** The local add panel. */
    private JPanel localAddPanel;

    /** The local panel. */
    private JPanel localPanel;

    /** The loging panel. */
    private JPanel logingPanel;

    /** The split pane1. */
    private JSplitPane splitPane1;

    /** The split pane h. */
    private JSplitPane splitPaneH;

    /** The split pane v. */
    private JSplitPane splitPaneV;

    /** The tabbed pane. */
    private JTabbedPane tabbedPane;

    /** The loging area. */
    private JTextArea logingArea;

    /** The filter text field. */
    private JTextField filterTextField;

    /** The global text field. */
    private JTextField globalTextField;

    /** The local text field. */
    private JTextField localTextField;

    /** The thread. */
    private Thread thread;

    /** The loging. */
    private VSLogging loging;

    /** The menu item states. */
    private VSMenuItemStates menuItemStates;

    /** The prefs. */
    private VSPrefs prefs;

    /** The simulator canvas. */
    private VSSimulatorVisualization simulatorVisualization;

    /** The simulator frame. */
    private VSSimulatorFrame simulatorFrame;

    /** The task manager. */
    private VSTaskManager taskManager;

    /** The task manager global model. */
    private VSTaskManagerTableModel taskManagerGlobalModel;

    /** The task manager local model. */
    private VSTaskManagerTableModel taskManagerLocalModel;

    /** The task manager global editor. */
    private VSTaskManagerCellEditor taskManagerGlobalEditor;

    /** The task manager local editor. */
    private VSTaskManagerCellEditor taskManagerLocalEditor;

    /** The last selected process num. */
    private int lastSelectedProcessNum;

    /** The last expert state. */
    private boolean lastExpertState;

    /** The simulator counter. */
    private static int simulatorCounter;

    /** The simulator num. */
    private static volatile int simulatorNum;

    /**
     * The class VSTaskManagerTableModel, an object of this class handles
     * the task manager's JTable.
     */
    private class VSTaskManagerTableModel extends AbstractTableModel
        implements MouseListener {
        /** the serial version uid */
        private static final long serialVersionUID = 1l;

        /** The Constant LOCAL. */
        public static final boolean LOCAL = true;

        /** The Constant GLOBAL. */
        public static final boolean GLOBAL = false;

        /** The Constant ALL_PROCESSES. */
        // public static final boolean ALL_PROCESSES = true;

        /** The Constant ONE_PROCESS. */
        public static final boolean ONE_PROCESS = false;

        /** The tasks. */
        private ArrayList<VSTask> tasks;

        /** The column names. */
        private String columnNames[];

        /** The num columns. */
        private int numColumns;

        /** The table. */
        //private JTable table;

        /** The editor. */
        //private VSTaskManagerCellEditor editor;

        /**
         * Instantiates a new VSTaskManagerTableModel object
         *
         * @param process the process
         * @param localTask true, if this table manages the local task. false,
         *	if this table manages the global tasks.
         */
        public VSTaskManagerTableModel(VSInternalProcess process,
                                       boolean localTask) {
            tasks = new ArrayList<VSTask>();
            set(process, localTask, ONE_PROCESS);
            columnNames = new String[3];
            columnNames[0]= prefs.getString("lang.time") + " (ms)";
            columnNames[1] = prefs.getString("lang.process.id");
            columnNames[2] = prefs.getString("lang.event");
            numColumns = 3;
        }

        /**
         * Sets the table.
         *
         * @param table the table
         */
        public void setTable(JTable table) {
            /* Maybe needed for future usage */
            //this.table = table;
        }

        /**
         * Sets the editor.
         *
         * @param editor the editor
         */
        public void setEditor(VSTaskManagerCellEditor editor) {
            /* Maybe needed for future usage */
            //this.editor = editor;
        }

        /**
         * Sets new values.
         *
         * @param process the process
         * @param localTasks true, if this table manages the local tasks. false
         *	if this table manages the global tasks.
         * @param allProcesses true, if this table shows tasks of all processes.
         *	false, if this table only shows tasks of the specified process.
         */
        public void set(VSInternalProcess process, boolean localTasks,
                        boolean allProcesses) {

            if (allProcesses) {
                this.tasks = localTasks
                             ?  taskManager.getLocalTasks()
                             :  taskManager.getGlobalTasks();
            } else {
                this.tasks = localTasks
                             ?  taskManager.getProcessLocalTasks(process)
                             :  taskManager.getProcessGlobalTasks(process);
            }

            Collections.sort(tasks);
            fireTableDataChanged();
        }

        /* (non-Javadoc)
         * @see javax.swing.table.AbstractTableModel#getColumnName(int)
         */
        public String getColumnName(int col) {
            return columnNames[col];
        }

        /* (non-Javadoc)
         * @see javax.swing.table.TableModel#getRowCount()
         */
        public int getRowCount() {
            return tasks == null ? 0 : tasks.size();
        }

        /* (non-Javadoc)
         * @see javax.swing.table.TableModel#getColumnCount()
         */
        public int getColumnCount() {
            return numColumns;
        }

        /* (non-Javadoc)
         * @see javax.swing.table.TableModel#getValueAt(int, int)
         */
        public Object getValueAt(int row, int col) {
            VSTask task = tasks.get(row);

            switch (col) {
            case 0:
                return task.getTaskTime();
            case 1:
                return task.getProcess().getProcessID();
            }

            return task.getEvent().getShortname();
        }

        /* (non-Javadoc)
         * @see javax.swing.table.AbstractTableModel#isCellEditable(int, int)
         */
        public boolean isCellEditable(int row, int col) {
            if (col == 2)
                return false;

            return true;
        }

        /* (non-Javadoc)
         * @see javax.swing.table.AbstractTableModel#setValueAt(
         *	java.lang.Object, int, int)
         */
        public void setValueAt(Object value, int row, int col) {
        }

        /**
         * Adds the task.
         *
         * @param task the task
         */
        public void addTask(VSTask task) {
            tasks.add(task);
            Collections.sort(tasks);
            fireTableDataChanged();
        }

        /**
         * Removes the task at a specified row.
         *
         * @param row the row
         * @return The removed task
         */
        public VSTask removeTaskAtRow(int row) {
            VSTask task = tasks.get(row);
            tasks.remove(task);
            taskManager.removeTask(task);
            fireTableDataChanged();
            return task;
        }

        /**
         * Checks if a specific row exists
         *
         * @param row the row
         * @return True, if the row exists. False, if not
         */
        public boolean rowExists(int row) {
            if (row < 0)
                return false;

            if (tasks.size() <= row)
                return false;

            return true;
        }

        /**
         * Gets the index of a specific task
         *
         * @param task The task
         * @return The index of the task
         */
        public int getIndexOf(VSTask task) {
            return tasks.indexOf(task);
        }

        /**
         * Copies the tasks at a specified rows.
         *
         * @param rows the rows
         */
        private void copyTasksAtRows(int rows[]) {
            ArrayList<VSTask> copiedTasks = new ArrayList<VSTask>();

            for (int row : rows)
                /* Use the copy constructor */
                copiedTasks.add(new VSTask(tasks.get(row)));

            for (VSTask task : copiedTasks) {
                taskManager.addTask(task, VSTaskManager.PROGRAMMED);
                addTask(task);
            }

            fireTableDataChanged();
        }

        /* (non-Javadoc)
         * @see java.awt.event.MouseListener#mouseClicked(
         *	java.awt.event.MouseEvent)
         */
        public void mouseClicked(MouseEvent me) {
            final JTable source = (JTable) me.getSource();
            //final int row = source.rowAtPoint(me.getPoint());
            //final int col = source.columnAtPoint(me.getPoint());

            if (SwingUtilities.isRightMouseButton(me)) {
                ActionListener actionListener = new ActionListener() {
                    public void actionPerformed(ActionEvent ae) {
                        String command = ae.getActionCommand();
                        int rows[] = source.getSelectedRows();

                        if (command.equals(prefs.getString("lang.remove"))) {
                            for (int i = rows.length - 1; i >= 0; --i)
                                removeTaskAtRow(rows[i]);

                        } else if (command.equals(
                                       prefs.getString("lang.copy"))) {
                            copyTasksAtRows(rows);
                        }
                    }
                };

                JPopupMenu popup = new JPopupMenu();
                JMenuItem item = new JMenuItem(prefs.getString("lang.remove"));
                item.addActionListener(actionListener);
                popup.add(item);

                item = new JMenuItem(prefs.getString("lang.copy"));
                item.addActionListener(actionListener);
                popup.add(item);

                popup.show(me.getComponent(), me.getX(), me.getY());
            }
        }

        /* (non-Javadoc)
         * @see java.awt.event.MouseListener#mouseEntered(
         *	java.awt.event.MouseEvent)
         */
        public void mouseEntered(MouseEvent me) { }

        /* (non-Javadoc)
         * @see java.awt.event.MouseListener#mouseExited(
         *	java.awt.event.MouseEvent)
         */
        public void mouseExited(MouseEvent me) { }

        /* (non-Javadoc)
         * @see java.awt.event.MouseListener#mousePressed(
         *	java.awt.event.MouseEvent)
         */
        public void mousePressed(MouseEvent me) { }

        /* (non-Javadoc)
         * @see java.awt.event.MouseListener#mouseReleased(
         *	java.awt.event.MouseEvent)
         */
        public void mouseReleased(MouseEvent me) { }
    }

    /**
     * The class VSTaskManagerCellEditor, an object of this class handles
     * the task manager's JTable editor
     */
    private class VSTaskManagerCellEditor extends AbstractCellEditor
        implements TableCellEditor {
        /** the serial version uid */
        private static final long serialVersionUID = 1L;

        /** The JTable model */
        private VSTaskManagerTableModel model;

        /**
         * Instantiates a new VSTaskManagerCellEditor object.
         *
         * @param model the model
         */
        public VSTaskManagerCellEditor(VSTaskManagerTableModel model) {
            this.model = model;
            model.setEditor(this);
        }

        /**
         * Stops editing
         */
        public void stopEditing() {
            fireEditingStopped();
        }

        /**
        /* (non-Javadoc)
         * @see javax.swing.table.TableCellEditor#getTableCellEditorComponent(
         *      javax.swing.JTable, java.lang.Object, boolean, int, int)
         */
        public Component getTableCellEditorComponent(final JTable table,
                Object object,
                boolean isSelected,
                final int row,
                final int col) {
            switch (col) {
            case 0:
                Long val = (Long) model.getValueAt(row, col);
                final JTextField valField = new JTextField(val.toString());
                valField.setBackground(Color.WHITE);
                valField.setBorder(null);
                valField.addActionListener(new ActionListener() {
                    private boolean isRed = false;
                    public void actionPerformed(ActionEvent ae) {
                        try {
                            Long val = Long.valueOf(valField.getText());
                            if (val.longValue() < 0)
                                throw new VSNegativeNumberException();
                            VSTask task = model.removeTaskAtRow(row);
                            task.setTaskTime(val.longValue());
                            taskManager.addTask(task, VSTaskManager.PROGRAMMED);
                            model.addTask(task);
                            if (isRed) {
                                valField.setBackground(Color.WHITE);
                                isRed = false;
                            }
                            int index = model.getIndexOf(task);
                            ListSelectionModel selectionModel =
                                table.getSelectionModel();
                            selectionModel.setSelectionInterval(index, index);
                            fireEditingStopped();

                        } catch (NumberFormatException exc) {
                            valField.setBackground(Color.RED);
                            isRed = true;

                        } catch (VSNegativeNumberException exc) {
                            valField.setBackground(Color.RED);
                            isRed = true;
                        }
                    }
                });
                return valField;
            case 1:
                Integer current[] = { (Integer) model.getValueAt(row, col) };
                final JComboBox<Integer> comboBox = new JComboBox<>(current);

                Integer pids[] = simulatorVisualization.getProcessIDs();
                for (Integer pid : pids)
                    comboBox.addItem(pid);

                comboBox.setSelectedIndex(0);
                comboBox.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent ae) {
                        int index = comboBox.getSelectedIndex() - 1;
                        if (model.rowExists(row)) {
                            VSTask task = model.removeTaskAtRow(row);
                            VSInternalProcess process =
                                simulatorVisualization.getProcess(index);
                            task.setProcess(process);
                            taskManager.addTask(task, VSTaskManager.PROGRAMMED);
                            if (allProcessesAreSelected())
                                model.addTask(task);
                        }

                        fireEditingStopped();
                    }
                });

                return comboBox;
            case 2:
                break;
            }

            return null;
        }

        /* (non-Javadoc)
         * @see javax.swing.CellEditor#getCellEditorValue()
         */
        public Object getCellEditorValue() {
            return new String("");
        }
    }


    /**
     * Instantiates a new VSSimulator object.
     *
     * @param prefs the prefs
     * @param simulatorFrame the simulator frame
     */
    public VSSimulator(VSPrefs prefs, VSSimulatorFrame simulatorFrame) {
        init(prefs, simulatorFrame);
    }

    /**
     * inits the VSSimulator object.
     *
     * @param prefs the prefs
     * @param simulatorFrame the simulator frame
     */
    private void init(VSPrefs prefs, VSSimulatorFrame simulatorFrame) {
        this.prefs = prefs;
        this.simulatorFrame = simulatorFrame;
        simulatorNum = ++simulatorCounter;
        this.menuItemStates = new VSMenuItemStates(false, false, false, true);
        this.localTextFields = new ArrayList<String>();
        this.globalTextFields = new ArrayList<String>();

        /* Not null if init has been called from the deserialization */
        if (this.loging == null)
            this.loging = new VSLogging();

        loging.log(prefs.getString("lang.simulator.new"));

        fillContentPane();
        updateFromPrefs();

        splitPaneH.setDividerLocation(
            prefs.getInteger("div.window.splitsize"));

        splitPaneV.setDividerLocation(
            prefs.getInteger("div.window.ysize")
            - prefs.getInteger("div.window.logsize"));

        splitPane1.setDividerLocation((int) (getPaintSize()/2) - 20);

        int numProcesses = simulatorVisualization.getNumProcesses();
        for (int i = 0; i <= numProcesses; ++i) {
            localTextFields.add("0000");
            globalTextFields.add("0000");
        }

        processesComboBox.setSelectedIndex(0);
        localPIDComboBox.setSelectedIndex(0);
        globalPIDComboBox.setSelectedIndex(0);

        thread = new Thread(simulatorVisualization);
        thread.start();
    }

    /**
     * Fills the content pane.
     */
    private void fillContentPane() {
        logingArea = loging.getLoggingArea();

        splitPaneH = new JSplitPane();
        splitPaneV = new JSplitPane();

        /* Not null if init has been called from the deserialization */
        if (this.simulatorVisualization == null)
            simulatorVisualization = new VSSimulatorVisualization(
                prefs, this, loging);

        taskManager = simulatorVisualization.getTaskManager();
        loging.setSimulatorCanvas(simulatorVisualization);

        JPanel canvasPanel = new JPanel();
        canvasPanel.setLayout(new GridLayout(1, 1, 3, 3));
        canvasPanel.add(simulatorVisualization);
        canvasPanel.setMinimumSize(new Dimension(0, 0));
        canvasPanel.setMaximumSize(new Dimension(0, 0));

        logingPanel = new JPanel(new BorderLayout());
        logingPanel.add(new JScrollPane(logingArea), BorderLayout.CENTER);
        logingPanel.add(createToolsPanel(), BorderLayout.SOUTH);
        logingPanel.setPreferredSize(new Dimension(200, 1));

        splitPaneH.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
        splitPaneH.setLeftComponent(createProcessPanel());
        splitPaneH.setRightComponent(canvasPanel);
        splitPaneH.setContinuousLayout(true);
        splitPaneH.setOneTouchExpandable(true);

        splitPaneV.setOrientation(JSplitPane.VERTICAL_SPLIT);
        splitPaneV.setTopComponent(splitPaneH);
        splitPaneV.setBottomComponent(logingPanel);
        splitPaneV.setContinuousLayout(true);

        this.add(splitPaneV);
    }

    /**
     * Creates the tools panel.
     *
     * @return the panel
     */
    private JPanel createToolsPanel() {
        JPanel toolsPanel = new JPanel();
        boolean expertMode = prefs.getBoolean("sim.mode.expert");

        toolsPanel.setLayout(new BoxLayout(toolsPanel, BoxLayout.X_AXIS));
        JCheckBox expertActiveCheckBox =
            new JCheckBox(prefs.getString("lang.mode.expert"));

        expertActiveCheckBox.setSelected(expertMode);
        expertActiveCheckBox.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent ce) {
                AbstractButton abstractButton =
                    (AbstractButton) ce.getSource();
                ButtonModel buttonModel = abstractButton.getModel();
                boolean newState = buttonModel.isSelected();
                if (lastExpertState != newState) {
                    lastExpertState = newState;
                    prefs.setBoolean("sim.mode.expert", newState);
                    fireExpertModeChanged();
                }
            }
        });
        toolsPanel.add(expertActiveCheckBox);

        if (expertMode) {
            lamportActiveCheckBox = new JCheckBox(
                prefs.getString("lang.time.lamport"));
            lamportActiveCheckBox.setSelected(false);
            lamportActiveCheckBox.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent ce) {
                    AbstractButton abstractButton =
                        (AbstractButton) ce.getSource();
                    ButtonModel buttonModel = abstractButton.getModel();
                    simulatorVisualization.showLamport(
                        buttonModel.isSelected());
                    if (buttonModel.isSelected())
                        vectorTimeActiveCheckBox.setSelected(false);
                }
            });
            toolsPanel.add(lamportActiveCheckBox);

            vectorTimeActiveCheckBox = new JCheckBox(
                prefs.getString("lang.time.vector"));
            vectorTimeActiveCheckBox.setSelected(false);
            vectorTimeActiveCheckBox.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent ce) {
                    AbstractButton abstractButton =
                        (AbstractButton) ce.getSource();
                    ButtonModel buttonModel = abstractButton.getModel();
                    simulatorVisualization.showVectorTime(
                        buttonModel.isSelected());
                    if (buttonModel.isSelected())
                        lamportActiveCheckBox.setSelected(false);
                }
            });
            toolsPanel.add(vectorTimeActiveCheckBox);

            JCheckBox antiAliasing = new JCheckBox(
                prefs.getString("lang.antialiasing"));
            antiAliasing.setSelected(false);
            antiAliasing.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent ce) {
                    AbstractButton abstractButton =
                        (AbstractButton) ce.getSource();
                    ButtonModel buttonModel = abstractButton.getModel();
                    simulatorVisualization.isAntiAliased(
                        buttonModel.isSelected());
                }
            });
            toolsPanel.add(antiAliasing);
        }

        JCheckBox logingActiveCheckBox = new JCheckBox(
            prefs.getString("lang.loging.active"));
        logingActiveCheckBox.setSelected(true);
        logingActiveCheckBox.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent ce) {
                AbstractButton abstractButton =
                    (AbstractButton) ce.getSource();
                ButtonModel buttonModel = abstractButton.getModel();
                loging.isPaused(!buttonModel.isSelected());
            }
        });
        toolsPanel.add(logingActiveCheckBox);

        if (expertMode) {
            filterActiveCheckBox = new JCheckBox(
                prefs.getString("lang.filter"));
            filterActiveCheckBox.setSelected(false);
            filterActiveCheckBox.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent ce) {
                    AbstractButton abstractButton =
                        (AbstractButton) ce.getSource();
                    ButtonModel buttonModel = abstractButton.getModel();
                    loging.isFiltered(buttonModel.isSelected());
                    if (buttonModel.isSelected())
                        loging.setFilterText(filterTextField.getText());
                }
            });
            toolsPanel.add(filterActiveCheckBox);

            filterTextField = new JTextField();
            filterTextField.getDocument().addDocumentListener(
            new DocumentListener() {
                public void insertUpdate(DocumentEvent de) {
                    loging.setFilterText(filterTextField.getText());
                }
                public void removeUpdate(DocumentEvent de) {
                    loging.setFilterText(filterTextField.getText());
                }
                public void changedUpdate(DocumentEvent de) {
                    loging.setFilterText(filterTextField.getText());
                }
            });
            toolsPanel.add(filterTextField);

            JButton clearButton = new JButton(
                prefs.getString("lang.loging.clear"));
            clearButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    String command = ae.getActionCommand();
                    if (command.equals(
                                prefs.getString("lang.loging.clear"))) {
                        loging.clear();
                    }
                }
            });
            toolsPanel.add(clearButton);
        }

        return toolsPanel;
    }

    /**
     * Creates the process panel.
     *
     * @return the panel
     */
    private JPanel createProcessPanel() {
        JPanel editPanel = new JPanel(new GridBagLayout());
        boolean expertMode = prefs.getBoolean("sim.mode.expert");
        editPanel.setLayout(new BoxLayout(editPanel, BoxLayout.Y_AXIS));

        processesComboBox = new JComboBox<>();
        localPIDComboBox = new JComboBox<>();
        globalPIDComboBox = new JComboBox<>();

        lastSelectedProcessNum = 0;
        int numProcesses = simulatorVisualization.getNumProcesses();
        String processString = prefs.getString("lang.process");

        for (int i = 0; i < numProcesses; ++i) {
            int pid = simulatorVisualization.getProcess(i).getProcessID();
            processesComboBox.addItem(processString + " " + pid);
            localPIDComboBox.addItem("PID: " + pid);
            globalPIDComboBox.addItem("PID: " + pid);
        }

        processesComboBox.addItem(prefs.getString("lang.processes.all"));
        localPIDComboBox.addItem(prefs.getString("lang.all"));
        globalPIDComboBox.addItem(prefs.getString("lang.all"));

        tabbedPane = new JTabbedPane(JTabbedPane.TOP,
                                     JTabbedPane.WRAP_TAB_LAYOUT);
        localPanel = createTaskLabel(VSTaskManagerTableModel.LOCAL);
        JPanel globalPanel = createTaskLabel(VSTaskManagerTableModel.GLOBAL);

        splitPane1 = new JSplitPane();
        splitPane1.setOrientation(JSplitPane.VERTICAL_SPLIT);
        splitPane1.setTopComponent(localPanel);
        splitPane1.setBottomComponent(globalPanel);
        splitPane1.setOneTouchExpandable(true);

        if (expertMode)
            tabbedPane.addTab(prefs.getString("lang.events"), splitPane1);

        else
            tabbedPane.addTab(prefs.getString("lang.events"), localPanel);

        processesComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                localTextFields.set(lastSelectedProcessNum,
                                    localTextField.getText());
                globalTextFields.set(lastSelectedProcessNum,
                                     globalTextField.getText());
                updateTaskManagerTable();

                int processNum = getSelectedProcessNum();
                localTextField.setText(localTextFields.get(processNum));
                globalTextField.setText(globalTextFields.get(processNum));
                localTextField.setBackground(Color.WHITE);
                globalTextField.setBackground(Color.WHITE);
                lastSelectedProcessNum = processNum;

                localPIDComboBox.setSelectedIndex(processNum);
                globalPIDComboBox.setSelectedIndex(processNum);

                if (processNum == simulatorVisualization.getNumProcesses()) {
                    tabbedPane.setEnabledAt(1, false);
                    if (tabbedPane.getSelectedIndex() == 1)
                        tabbedPane.setSelectedIndex(0);

                } else if (!tabbedPane.isEnabledAt(1)) {
                    tabbedPane.setEnabledAt(1, true);
                }

                if (processNum != simulatorVisualization.getNumProcesses()) {
                    VSInternalProcess process = getSelectedProcess();
                    VSProcessEditor processEditor =
                        new VSProcessEditor(prefs, process);
                    tabbedPane.setComponentAt(1,
                                              processEditor.getContentPane());
                }
            }
        });

        tabbedPane.add(prefs.getString("lang.variables"), null);

        editPanel.add(processesComboBox);
        editPanel.add(tabbedPane);

        return editPanel;
    }

    /**
     * Creates the label panel.
     *
     * @param text the text
     *
     * @return the panel
     */
    private JPanel createLabelPanel(String text) {
        JPanel panel = new JPanel();
        JLabel label = new JLabel(text);
        panel.add(label);

        return panel;
    }

    /**
     * Creates the task label.
     *
     * @param localTasks true, if the local task label has to get created.
     *	false, if the global task label has to get created.
     *
     * @return the panel
     */
    private JPanel createTaskLabel(boolean localTasks) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        if (localTasks)
            panel.add(createLabelPanel(prefs.getString("lang.timed.local")));
        else
            panel.add(createLabelPanel(prefs.getString("lang.timed.global")));

        JScrollPane scrollPane = new JScrollPane(createTaskTable(localTasks));
        panel.add(scrollPane);

        if (localTasks)
            localAddPanel = initAddPanel(panel, localTasks);
        else
            /*globalAddPanel = */ initAddPanel(panel, localTasks);

        return panel;
    }

    /**
     * Creates the task table.
     *
     * @param localTasks true, if the local task label has to get created.
     *	false, if the global task label has to get created.
     *
     * @return the table
     */
    private JTable createTaskTable(boolean localTasks) {
        VSInternalProcess process = getSelectedProcess();
        VSTaskManagerTableModel model =
            new VSTaskManagerTableModel(process, localTasks);
        VSTaskManagerCellEditor cellEditor =
            new VSTaskManagerCellEditor(model);

        if (localTasks) {
            taskManagerLocalModel = model;
            taskManagerLocalEditor = cellEditor;
        } else {
            taskManagerGlobalModel = model;
            taskManagerGlobalEditor = cellEditor;
        }

        JTable table = new JTable(model);
        table.setDefaultEditor(Object.class, cellEditor);
        model.setTable(table);

        table.addMouseListener(model);

        TableColumn col = table.getColumnModel().getColumn(0);
        col.setMaxWidth(62);
        col.setResizable(false);

        col = table.getColumnModel().getColumn(1);
        col.setMaxWidth(40);
        col.setResizable(false);

        col = table.getColumnModel().getColumn(2);
        col.sizeWidthToFit();
        table.setBackground(Color.WHITE);

        return table;
    }

    /**
     * Inits the add panel.
     *
     * @param panel the panel
     * @param localTasks true, if the local task label has to get created.
     *	false, if the global task label has to get created.
     *
     * @return the panel
     */
    private JPanel initAddPanel(JPanel panel, final boolean localTasks) {
        JPanel addPanel = new JPanel();
        addPanel.setLayout(new BoxLayout(addPanel, BoxLayout.X_AXIS));
        boolean expertMode = prefs.getBoolean("sim.mode.expert");

        final JTextField textField = new JTextField();
        if (localTasks)
            localTextField = textField;
        else
            globalTextField = textField;

        textField.setText("0000");
        textField.setBackground(Color.WHITE);
        addPanel.add(textField);

        addPanel.add(new JLabel(" ms "));

        if (localTasks) {
            if (expertMode)
                addPanel.add(localPIDComboBox);
        } else {
            addPanel.add(globalPIDComboBox);
        }

        final JComboBox<String> comboBox = new JComboBox<>();
        JButton takeoverButton = new JButton(prefs.getString("lang.takeover"));
        takeoverButton.setMnemonic(prefs.getInteger("keyevent.takeover"));
        takeoverButton.addActionListener(new ActionListener() {
            private boolean isRed;
            public void actionPerformed(ActionEvent ae) {
                String textValue = textField.getText();
                Long longValue = null;

                try {
                    longValue = Long.valueOf(textValue);

                    if (longValue.longValue() < 0) {
                        makeRed();
                        return;
                    }

                    if (isRed) {
                        makeWhite();
                    }

                } catch (NumberFormatException e) {
                    makeRed();
                }

                if (longValue == null)
                    return;

                if (takeover(longValue.longValue())) {
                    if (isRed)
                        makeWhite();

                } else {
                    makeRed();
                }
            }

            private void makeWhite() {
                textField.setBackground(Color.WHITE);
                isRed = false;
            }

            private void makeRed() {
                textField.setBackground(Color.RED);
                isRed = true;
            }

            private boolean takeover(long time) {
                VSInternalProcess selectedProcess = getSelectedProcess();
                int index = comboBox.getSelectedIndex();
                VSCreateTask createTask = createTasks.get(index);

                if (createTask.isDummy())
                    return false;

                ArrayList<VSInternalProcess> processes =
                    getConcernedProcesses(localTasks);

                for (VSInternalProcess process : processes) {
                    VSTask task = createTask.createTask(process, time,
                                                        localTasks);
                    taskManager.addTask(task, VSTaskManager.PROGRAMMED);

                    if (selectedProcess == null ||
                            process.equals(selectedProcess)) {
                        if (localTasks)
                            taskManagerLocalModel.addTask(task);
                        else
                            taskManagerGlobalModel.addTask(task);
                    }
                }

                return true;
            }
        });

        addPanel.add(takeoverButton);

        boolean createTaskFlag = createTasks == null;
        if (createTaskFlag) createTasks = new ArrayList<VSCreateTask>();

        Vector<String> eventClassnames =
            VSRegisteredEvents.getNonProtocolClassnames();

        comboBox.setMaximumRowCount(20);
        String menuText = prefs.getString("lang.events.process");
        comboBox.addItem("----- " + menuText + " -----");

        if (createTaskFlag)
            createTasks.add(new VSCreateTask(menuText));

        for (String eventClassname : eventClassnames) {
            String eventShortname =
                VSRegisteredEvents.getShortnameByClassname(eventClassname);
            menuText = eventShortname;
            comboBox.addItem(menuText);
            if (createTaskFlag)
                createTasks.add(new VSCreateTask(menuText, eventClassname));
        }

        String activate = prefs.getString("lang.activate");
        String client = prefs.getString("lang.client");
        String clientRequest = prefs.getString("lang.clientrequest.start");
        String deactivate = prefs.getString("langactivate");
        String server = prefs.getString("lang.server");
        String serverRequest = prefs.getString("lang.serverrequest.start");
        String protocol = prefs.getString("lang.protocol");

        String protocolEventClassname = "events.internal.VSProtocolEvent";
        eventClassnames = VSRegisteredEvents.getProtocolClassnames();

        for (String eventClassname : eventClassnames) {
            String eventShortname_ =
                VSRegisteredEvents.getShortnameByClassname(eventClassname);
            String eventShortname = null;

            menuText = eventShortname_ + " " + protocol;
            comboBox.addItem("----- " + menuText + " -----");

            if (createTaskFlag)
                createTasks.add(new VSCreateTask(menuText));

            if (VSRegisteredEvents.isOnServerStartProtocol(eventClassname))
                eventShortname = eventShortname_ + " " + serverRequest;
            else
                eventShortname = eventShortname_ + " " + clientRequest;

            menuText = eventShortname;
            comboBox.addItem(menuText);
            if (createTaskFlag) {
                VSCreateTask createTask = new VSCreateTask(menuText,
                        eventClassname);
                createTask.setShortname(eventShortname);
                createTask.isRequest(true);
                createTasks.add(createTask);
            }

            eventShortname = eventShortname_ + " " + client + " " + activate;
            menuText = eventShortname;
            comboBox.addItem(menuText);
            if (createTaskFlag) {
                VSCreateTask createTask =
                    new VSCreateTask(menuText, protocolEventClassname);
                createTask.isProtocolActivation(true);
                createTask.isClientProtocol(true);
                createTask.setProtocolClassname(eventClassname);
                createTask.setShortname(eventShortname);
                createTasks.add(createTask);
            }

            eventShortname = eventShortname_ + " " + client + " " + deactivate;
            menuText = eventShortname;
            comboBox.addItem(menuText);
            if (createTaskFlag) {
                VSCreateTask createTask =
                    new VSCreateTask(menuText, protocolEventClassname);
                createTask.isProtocolDeactivation(true);
                createTask.isClientProtocol(true);
                createTask.setProtocolClassname(eventClassname);
                createTask.setShortname(eventShortname);
                createTasks.add(createTask);
            }

            eventShortname = eventShortname_ + " " + server + " " + activate;
            menuText = eventShortname;
            comboBox.addItem(menuText);
            if (createTaskFlag) {
                VSCreateTask createTask =
                    new VSCreateTask(menuText, protocolEventClassname);
                createTask.isProtocolActivation(true);
                createTask.isClientProtocol(false);
                createTask.setProtocolClassname(eventClassname);
                createTask.setShortname(eventShortname);
                createTasks.add(createTask);
            }

            eventShortname = eventShortname_ + " " + server + " " + deactivate;
            menuText = eventShortname;
            comboBox.addItem(menuText);
            if (createTaskFlag) {
                VSCreateTask createTask =
                    new VSCreateTask(menuText, protocolEventClassname);
                createTask.isProtocolDeactivation(true);
                createTask.isClientProtocol(false);
                createTask.setProtocolClassname(eventClassname);
                createTask.setShortname(eventShortname);
                createTasks.add(createTask);
            }
        }

        panel.add(comboBox);
        panel.add(addPanel);

        return addPanel;
    }

    /**
     * Gets the split size.
     *
     * @return the split size
     */
    public synchronized int getSplitSize() {
        return splitPaneH.getDividerLocation();
    }

    /**
     * Gets the paint size.
     *
     * @return the paint size
     */
    public synchronized int getPaintSize() {
        return splitPaneV.getDividerLocation();
    }

    /**
     * Gets the selected process num.
     *
     * @return the selected process num
     */
    private int getSelectedProcessNum() {
        return processesComboBox.getSelectedIndex();
    }

    /**
     * Checks if 'all processes' is selected
     *
     * @return True, if 'all processes' are selected, else false
     */
    private boolean allProcessesAreSelected() {
        return processesComboBox.getSelectedIndex() + 1
               == processesComboBox.getItemCount();
    }

    /**
     * Gets the selected process.
     *
     * @return the selected process
     */
    private VSInternalProcess getSelectedProcess() {
        int processNum = getSelectedProcessNum();
        return simulatorVisualization.getProcess(processNum);
    }

    /**
     * Gets the concerned processes.
     *
     * @param localTasks true, if this table manages the local tasks. false
     *	if this table manages the global tasks.
     *
     * @return the concerned processes
     */
    private ArrayList<VSInternalProcess> getConcernedProcesses(
        boolean localTasks) {
        int processNum = localTasks
                         ? localPIDComboBox.getSelectedIndex()
                         : globalPIDComboBox.getSelectedIndex();

        if (processNum == simulatorVisualization.getNumProcesses())
            return simulatorVisualization.getProcessesArray();

        ArrayList<VSInternalProcess> arr = new ArrayList<VSInternalProcess>();
        arr.add(simulatorVisualization.getProcess(processNum));

        return arr;
    }

    /**
     * Update task manager table.
     */
    public synchronized void updateTaskManagerTable() {
        VSInternalProcess process = getSelectedProcess();
        boolean allProcesses = process == null;

        taskManagerLocalEditor.stopEditing();
        taskManagerGlobalEditor.stopEditing();

        taskManagerLocalModel.set(process,
                                  VSTaskManagerTableModel.LOCAL,
                                  allProcesses);

        taskManagerGlobalModel.set(process,
                                   VSTaskManagerTableModel.GLOBAL,
                                   allProcesses);
    }

    /**
     * Update the processes combo box
     */
    private void updateProcessesComboBox() {
        int numProcesses = simulatorVisualization.getNumProcesses();
        String processString = prefs.getString("lang.process");

        for (int i = 0; i < numProcesses; ++i) {
            int processID = simulatorVisualization.getProcess(i).getProcessID();

            processesComboBox.removeItemAt(i);
            localPIDComboBox.removeItemAt(i);
            globalPIDComboBox.removeItemAt(i);

            processesComboBox.insertItemAt(processString + " " + processID, i);
            localPIDComboBox.insertItemAt("PID: " + processID, i);
            globalPIDComboBox.insertItemAt("PID: " + processID, i);
        }
    }

    /**
     * The simulator has finished.
     */
    public synchronized void finish() {
        menuItemStates.setStart(false);
        menuItemStates.setPause(false);
        menuItemStates.setReset(true);
        menuItemStates.setReplay(true);
        simulatorFrame.updateSimulatorMenu();
    }

    /**
     * Gets the simulator num.
     *
     * @return the simulator num
     */
    public synchronized int getSimulatorNum() {
        return simulatorNum;
    }

    /**
     * Gets the menu item states.
     *
     * @return the menu item states
     */
    public synchronized VSMenuItemStates getMenuItemStates() {
        return menuItemStates;
    }

    /**
     * Gets the simulator canvas.
     *
     * @return the simulator canvas
     */
    public synchronized VSSimulatorVisualization getSimulatorCanvas() {
        return simulatorVisualization;
    }

    /**
     * Gets the simulator frame.
     *
     * @return the simulator frame
     */
    public synchronized VSSimulatorFrame getSimulatorFrame() {
        return simulatorFrame;
    }

    /**
     * Update from prefs.
     */
    public synchronized void updateFromPrefs() {
        simulatorVisualization.setBackground(prefs.getColor("col.background"));
        simulatorVisualization.updateFromPrefs();
    }

    /**
     * Removes the process at a specified index.
     *
     * @param index the index
     */
    public synchronized void removedAProcessAtIndex(int index) {
        if (lastSelectedProcessNum > index)
            --lastSelectedProcessNum;

        globalTextFields.remove(index);
        localTextFields.remove(index);

        globalPIDComboBox.removeItemAt(index);
        localPIDComboBox.removeItemAt(index);

        processesComboBox.removeItemAt(index);
        simulatorFrame.updateEditMenu();

        updateTaskManagerTable();
    }

    /**
     * Adds the process at a specified index.
     *
     * @param index the index
     */
    public synchronized void addProcessAtIndex(int index) {
        int processID = simulatorVisualization.getProcess(index).getProcessID();
        String processString = prefs.getString("lang.process");

        localTextFields.add(index, "0000");
        globalTextFields.add(index, "0000");

        localPIDComboBox.insertItemAt("PID: " + processID, index);
        globalPIDComboBox.insertItemAt("PID: " + processID, index);

        processesComboBox.insertItemAt(processString + " " + processID, index);
        simulatorFrame.updateEditMenu();
    }

    /**
     * Fire expert mode changed. Tell, that the expert mode has changed.
     */
    public synchronized void fireExpertModeChanged() {
        boolean expertMode = prefs.getBoolean("sim.mode.expert");

        /* Update the Task Manager GUI */
        int selectedIndex = tabbedPane.getSelectedIndex();

        if (expertMode) {
            tabbedPane.remove(localPanel);
            tabbedPane.insertTab(prefs.getString("lang.events"), null,
                                 splitPane1, null, 0);
            splitPane1.setTopComponent(localPanel);
            //splitPane1.setDividerLocation((int) (getPaintSize()/2) - 20);

            /* addPanel */
            localAddPanel.add(localPIDComboBox, 2);

        } else {
            tabbedPane.remove(splitPane1);
            tabbedPane.insertTab(prefs.getString("lang.events"), null,
                                 localPanel, null, 0);

            /* addPanel */
            localAddPanel.remove(2);
        }

        tabbedPane.setSelectedIndex(selectedIndex);

        /* Update the 'Variables tab' */
        if (getSelectedProcessNum() !=
                simulatorVisualization.getNumProcesses()) {
            VSInternalProcess process = getSelectedProcess();
            VSProcessEditor editor = new VSProcessEditor(prefs, process);
            tabbedPane.setComponentAt(1, editor.getContentPane());
        }

        /* Update the tools panel */
        logingPanel.remove(1);
        logingPanel.add(createToolsPanel(), BorderLayout.SOUTH);
        updateUI();
    }

    /**
     * Gets the prefs.
     *
     * @return the prefs
     */
    public synchronized VSPrefs getPrefs() {
        return prefs;
    }

    /**
     * Gets the create tasks objects. Those objects are for creating new tasks
     * via the task manager GUI or via right click on the paint area of the
     * simulator canvas!
     *
     * @return The create tasks objects
     */
    ArrayList<VSCreateTask> getCreateTaskObjects() {
        return createTasks;
    }

    /* (non-Javadoc)
     * @see serialize.VSSerializable#serialize(serialize.VSSerialize,
     *	java.io.ObjectOutputStream)
     */
    public synchronized void serialize(VSSerialize serialize,
                                       ObjectOutputStream objectOutputStream)
    throws IOException {
        /** For later backwards compatibility, to add more stuff */
        objectOutputStream.writeObject(Boolean.valueOf(false));

        simulatorVisualization.serialize(serialize, objectOutputStream);

        /** For later backwards compatibility, to add more stuff */
        objectOutputStream.writeObject(Boolean.valueOf(false));

    }

    /* (non-Javadoc)
     * @see serialize.VSSerializable#deserialize(serialize.VSSerialize,
     *	java.io.ObjectInputStream)
     */
    public synchronized void deserialize(VSSerialize serialize,
                                         ObjectInputStream objectInputStream)
    throws IOException, ClassNotFoundException {
        if (VSSerialize.DEBUG)
            System.out.println("Deserializing: VSSimulator");

        serialize.setObject("simulator", this);
        serialize.setObject("loging", loging);

        /** For later backwards compatibility, to add more stuff */
        objectInputStream.readObject();

        simulatorVisualization.deserialize(serialize, objectInputStream);

        /** For later backwards compatibility, to add more stuff */
        objectInputStream.readObject();

        updateFromPrefs();
        updateTaskManagerTable();
        updateProcessesComboBox();
        processesComboBox.setSelectedIndex(processesComboBox.getItemCount()-1);
    }
}
