package simulator;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.HierarchyBoundsListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferStrategy;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.LinkedList;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import core.VSInternalProcess;
import core.VSMessage;
import core.VSTask;
import core.VSTaskManager;
import core.time.VSTime;
import events.VSAbstractEvent;
import events.implementations.VSProcessCrashEvent;
import events.implementations.VSProcessRecoverEvent;
import events.internal.VSMessageReceiveEvent;
import prefs.VSPrefs;
import prefs.editors.VSEditorFrame;
import prefs.editors.VSProcessEditor;
import serialize.VSSerializable;
import serialize.VSSerialize;

/**
 * The class VSSimulatorVisualization. An instance of this object represents the
 * graphical paint area of a simulator. It contains all graphic calculations.
 * Also the simulator thread takes place in this class in a loop! This class
 * is probably the most cryptic of the whole simulator source code. This is
 * this way in order to gain more performance of the painting area!
 *
 * @author Paul C. Buetow
 */
public class VSSimulatorVisualization extends Canvas
    implements Runnable, VSSerializable {

    /** The serial version uid */
    private static final long serialVersionUID = 1L;

    /** The highlighted process. */
    private VSInternalProcess highlightedProcess;

    /** The simulator. */
    private VSSimulator simulator;

    /** The prefs. */
    private VSPrefs prefs;

    /** The loging. */
    private VSLogging loging;

    /** The num processes. */
    private volatile int numProcesses;

    /** The seconds spaceing. */
    private int secondsSpaceing;

    /** The thread sleep. */
    private int threadSleep;

    /** The until time. Until then goes the simulator? */
    private long untilTime;

    /** The simulator is paused. */
    private volatile boolean isPaused = true;

    /** The simulator thread is stopped. */
    private volatile boolean hasThreadStopped = false;

    /** The simulator is finished. */
    private volatile boolean hasFinished = false;

    /** The simulator is resetted. */
    private volatile boolean isResetted = false;

    /** The simulator is anti aliased. */
    private volatile boolean isAntiAliased = false;

    /** The simulator's anti aliasing has changed. */
    private volatile boolean isAntiAliasedChanged = false;

    /** The simulator shows the lamport time. */
    private volatile boolean showLamport = false;

    /** The simulator shows the vector time. */
    private volatile boolean showVectorTime = false;

    /** The pause time. */
    private volatile long pauseTime;

    /** The start time. */
    private volatile long startTime;

    /** The global time. */
    private volatile long time;

    /** The last global time. */
    private volatile long lastTime;

    /** The task manager. */
    private VSTaskManager taskManager;

    /** The message lines. */
    private LinkedList<VSMessageLine> messageLines;

    /** The message lines to remove. */
    private LinkedList<VSMessageLine> messageLinesToRemove;

    /** The processes. */
    private ArrayList<VSInternalProcess> processes;

    /** The clock speed. */
    private double clockSpeed;

    /** The clock offset. */
    private double clockOffset;

    /** The simulator time. */
    private long simulatorTime;

    /** The x paint size. */
    double xPaintSize;

    /** The paint size. */
    double paintSize;

    /** The y distance. */
    double yDistance;

    /** The global time x position. */
    double globalTimeXPosition;

    /** The xoffset_plus_xpaintsize. */
    int xoffset_plus_xpaintsize;

    /** The xpaintsize_dividedby_untiltime. */
    double xpaintsize_dividedby_untiltime;

    /** The paint processes offset. */
    int paintProcessesOffset;

    /** The paint secondlines seconds. */
    int paintSecondlinesSeconds;

    /** The paint secondlines line. */
    int paintSecondlinesLine[] = new int[4];

    /** The paint secondlines y string pos1. */
    int paintSecondlinesYStringPos1;

    /** The paint secondlines y string pos2. */
    int paintSecondlinesYStringPos2;

    /** The paint global time y position. */
    int paintGlobalTimeYPosition;

    /* GFX buffering */
    /** The strategy for buffering. */
    private BufferStrategy strategy;

    /** The graphics object to paint at. */
    private Graphics2D g;

    /** The Constant LINE_WIDTH. */
    private static final int LINE_WIDTH = 5;

    /** The Constant SEPLINE_WIDTH. */
    private static final int SEPLINE_WIDTH = 2;

    /** The Constant XOFFSET. */
    private static final int XOFFSET = 50;

    /** The Constant YOFFSET. */
    private static final int YOFFSET = 30;

    /** The Constant YOUTER_SPACEING. */
    private static final int YOUTER_SPACEING = 15;

    /** The Constant YSEPLINE_SPACEING. */
    private static final int YSEPLINE_SPACEING = 20;

    /** The Constant TEXT_SPACEING. */
    private static final int TEXT_SPACEING = 10;

    /** The Constant ROW_HEIGHT. */
    private static final int ROW_HEIGHT = 14;

    /* Constats, which have to get calculated once after start */
    /** The processline color. */
    private Color processlineColor;

    /** The process secondline color. */
    private Color processSecondlineColor;

    /** The process sepline color. */
    private Color processSeplineColor;

    /** The message arrived color. */
    private Color messageArrivedColor;

    /** The message sending color. */
    private Color messageSendingColor;

    /** The message lost color. */
    private Color messageLostColor;

    /** The background color. */
    private Color backgroundColor;

    /** The message line counter. */
    private long messageLineCounter;

    /** The process counter. Needed for the unique process id's. */
    private int processCounter;

    /**
     * The class VSMessageLine, an object of this class represents a message
     * line drawn into the painting area.
     *
     * @author Paul C. Buetow
     */
    public class VSMessageLine {
        /** The receiver process. */
        private VSInternalProcess receiverProcess;

        /** The color. */
        private Color color;

        /** The send time. */
        private long sendTime;

        /** The recv time. */
        private long recvTime;

        /** The sender num. */
        private int senderNum;

        /** The receiver num. */
        private int receiverNum;

        /** The offset1. */
        private int offset1;

        /** The offset2. */
        private int offset2;

        /** The message has arrived. */
        private boolean isArrived;

        /** The message is lost. */
        private boolean isLost;

        /** The x1. */
        private double x1;

        /** The y1. */
        private double y1;

        /** The x2. */
        private double x2;

        /** The y2. */
        private double y2;

        /** The x. */
        private double x;

        /** The y. */
        private double y;

        /** The outage time. */
        private long outageTime;

        /** The z. */
        private long z;

        /** The message line num. */
        private long messageLineNum;

        /** The task. */
        private VSTask task;

        /**
         * Instantiates a new VSMessageLine object.
         *
         * @param receiverProcess the receiver process
         * @param sendTime the send time
         * @param recvTime the recv time
         * @param outageTime the outage time
         * @param senderNum the sender num
         * @param receiverNum the receiver num
         * @param task the task
         */
        public VSMessageLine(VSInternalProcess receiverProcess, long sendTime,
                             long recvTime, long outageTime, int senderNum,
                             int receiverNum, VSTask task) {
            this.receiverProcess = receiverProcess;
            this.sendTime = sendTime;
            this.recvTime = recvTime;
            this.outageTime = outageTime;
            this.senderNum = senderNum;
            this.receiverNum = receiverNum;
            this.isArrived = false;
            this.isLost = false;
            this.messageLineNum = ++messageLineCounter;
            this.task = task;

            if (senderNum > receiverNum) {
                //offset1 = 1;
                offset2 = LINE_WIDTH;
            } else {
                offset1 = LINE_WIDTH - 1;
                //offset2 = 1;
            }

            /* Needed if the message gets lost after 0ms */
            this.x = getTimeXPosition(sendTime);
            this.y = getProcessYPosition(senderNum+1) + offset1;

            recalcOnChange();
            paint();
        }

        /**
         * Recalc on change.
         */
        public void recalcOnChange() {
            x1 = getTimeXPosition(sendTime);
            y1 = getProcessYPosition(senderNum+1) + offset1;
            x2 = getTimeXPosition(recvTime);
            y2 = getProcessYPosition(receiverNum+1) + offset2;

            if (isLost) {
                x = getTimeXPosition(z);
                y = y1 + ( ( (y2-y1) / (x2-x1)) * (x-x1));
            }

        }

        /**
         * Draws the message line.
         *
         * @param g the grpahics object to draw at
         * @param globalTime the global time
         */
        public void draw(final Graphics2D g, final long globalTime) {
            if (isArrived) {
                g.setColor(color);
                g.drawLine((int) x1, (int) y1, (int) x2, (int) y2);

            } else if (isLost) {
                g.setColor(messageLostColor);
                g.drawLine((int) x1, (int) y1, (int) x, (int) y);

            } else if (globalTime >= recvTime) {
                checkIfMessageIsRelevant();
                isArrived = true;

                if (receiverProcess.isCrashed())
                    color = messageLostColor;
                else
                    color = messageArrivedColor;

                draw(g, globalTime);

            } else if (outageTime >= 0 && outageTime <= globalTime) {
                checkIfMessageIsRelevant();
                isLost = true;
                draw(g, globalTime);;

            } else {
                z = globalTime;
                x = globalTimeXPosition;
                y = y1 + ( ( (y2-y1) / (x2-x1)) * (x-x1));
                g.setColor(messageSendingColor);
                g.drawLine((int) x1, (int) y1, (int) x, (int) y);
            }
        }

        /**
         * Checks if the message is relevant. If it's not relevant, then it will
         * get removed.
         */
        private void checkIfMessageIsRelevant() {
            if (prefs.getBoolean("sim.messages.relevant")) {
                VSMessageReceiveEvent event =
                    (VSMessageReceiveEvent) task.getEvent();
                event.init(receiverProcess);
                if (!event.isRelevantMessage())
                    removeMessageLine(this);
            }
        }

        /**
         * Called if a process within the simulator has been removed at a
         * specified index.
         *
         * @param index the index
         *
         * @return true, if the sender or the receiver of the message has been
         *	removed from the simulator. Else false is returned.
         */
        public boolean removedAProcessAtIndex(int index) {
            if (index == receiverNum || index == senderNum)
                return true;

            if (index < receiverNum)
                --receiverNum;

            if (index < senderNum)
                --senderNum;

            recalcOnChange();

            return false;
        }

        /**
         * Gets the message line num.
         *
         * @return the message line num
         */
        public long getMessageLineNum() {
            return messageLineNum;
        }

        /**
         * Checks one line to another if they equal (have the same message line
         * id)
         *
         * @param line the line to compare against
         *
         * @return true, if they equal
         */
        public boolean equals(VSMessageLine line) {
            return messageLineNum == line.getMessageLineNum();
        }

        /**
         * Gets the task.
         *
         * @return the task
         */
        public VSTask getTask() {
            return task;
        }
    }

    /**
     * Instantiates a new VSSimulatorVisualization object.
     *
     * @param prefs the prefs
     * @param simulator the simulator
     * @param loging the loging
     */
    public VSSimulatorVisualization(VSPrefs prefs, VSSimulator simulator,
                                    VSLogging loging) {
        init(prefs, simulator, loging);
    }

    /**
     * Instantiates inits the VSSimulatorVisualization object.
     *
     * @param prefs the prefs
     * @param simulator the simulator
     * @param loging the loging
     */
    private void init(VSPrefs prefs, VSSimulator simulator,
                      VSLogging loging) {
        this.prefs = prefs;
        this.simulator = simulator;
        this.loging = loging;
        this.messageLines = new LinkedList<VSMessageLine>();
        this.messageLinesToRemove = new LinkedList<VSMessageLine>();

        /* May be not null if called from deserialization */
        if (this.taskManager == null)
            this.taskManager = new VSTaskManager(prefs, this);

        /* May be not null if called from deserialization */
        if (this.processes == null) {
            this.processes = new ArrayList<VSInternalProcess>();

            numProcesses = prefs.getInteger("sim.process.num");
            for (int i = 0; i < numProcesses; ++i)
                processes.add(createProcess(i));
        }

        updateFromPrefs();

        final VSPrefs finalPrefs = prefs;
        final VSSimulator finalSimulator = simulator;

        addMouseListener(new MouseListener() {
            public void mouseClicked(MouseEvent me) {
                final VSInternalProcess process = getProcessAtYPos(me.getY());

                if (SwingUtilities.isRightMouseButton(me)) {
                    ActionListener actionListener = new ActionListener() {
                        public void actionPerformed(ActionEvent ae) {
                            String command = ae.getActionCommand();
                            if (command.equals(
                                        finalPrefs.getString("lang.process.edit"))) {
                                editProcess(process);

                            } else if (command.equals(
                                           finalPrefs.getString("lang.process.crash"))) {
                                VSAbstractEvent event =
                                    new VSProcessCrashEvent();

                                taskManager.addTask(new VSTask(
                                                        process.getGlobalTime(),
                                                        process, event,
                                                        VSTask.GLOBAL));

                            } else if (command.equals(
                                           finalPrefs.getString("lang.process.recover"))) {
                                VSAbstractEvent event =
                                    new VSProcessRecoverEvent();

                                taskManager.addTask(new VSTask(
                                                        process.getGlobalTime(),
                                                        process, event,
                                                        VSTask.GLOBAL));

                            } else if (command.equals(
                                           finalPrefs.getString("lang.process.remove"))) {
                                removeProcess(process);

                            } else if (command.equals(
                                           finalPrefs.getString("lang.process.add.new"))) {
                                addProcess();
                            }
                        }
                    };

                    JPopupMenu popup = new JPopupMenu();
                    JMenuItem item = null;

                    if (process != null)
                        item = new JMenuItem(
                            finalPrefs.getString("lang.process.selected") +
                            ": " + process.getProcessID());
                    else
                        item = new JMenuItem(
                            finalPrefs.getString("lang.process.not.selected"));

                    item.setEnabled(false);
                    popup.add(item);
                    popup.addSeparator();

                    item = new JMenuItem(
                        finalPrefs.getString("lang.process.edit"));
                    if (process == null)
                        item.setEnabled(false);
                    else
                        item.addActionListener(actionListener);
                    popup.add(item);

                    item = new JMenuItem(
                        finalPrefs.getString("lang.process.crash"));

                    if (process == null || process.isCrashed() || isPaused ||
                            time == 0 || hasFinished)
                        item.setEnabled(false);
                    else
                        item.addActionListener(actionListener);
                    popup.add(item);

                    item = new JMenuItem(
                        finalPrefs.getString("lang.process.recover"));

                    if (process == null || !process.isCrashed() || isPaused ||
                            time == 0 || hasFinished)
                        item.setEnabled(false);
                    else
                        item.addActionListener(actionListener);
                    popup.add(item);

                    item = new JMenuItem(
                        finalPrefs.getString("lang.process.remove"));

                    if (process == null)
                        item.setEnabled(false);
                    else
                        item.addActionListener(actionListener);
                    popup.add(item);

                    popup.addSeparator();

                    final long xPosTime = getXPositionTime(me.getX());
                    String timeString = finalPrefs.getString(
                                            "lang.event.add.time") +
                                        " " + xPosTime + "ms";

                    JMenu subMenu = new JMenu(
                        finalPrefs.getString("lang.event.add.local")
                        + " " + timeString);

                    ArrayList<VSCreateTask> createTasks =
                        finalSimulator.getCreateTaskObjects();

                    if (process == null) {
                        subMenu.setEnabled(false);
                    } else {
                        JMenu subSubMenu = null;
                        for (final VSCreateTask createTask : createTasks) {
                            if (createTask.isDummy()) {
                                if (subSubMenu != null)
                                    subMenu.add(subSubMenu);
                                subSubMenu = new JMenu(
                                    createTask.getMenuText());
                            } else {
                                item = new JMenuItem(createTask.getMenuText());
                                item.addActionListener(new ActionListener() {
                                    public void actionPerformed(ActionEvent e) {
                                        VSTask task =
                                            createTask.createTask(process,
                                                                  xPosTime,
                                                                  true);
                                        taskManager.addTask(
                                            task, VSTaskManager.PROGRAMMED);
                                        finalSimulator.updateTaskManagerTable();
                                    }
                                });
                                subSubMenu.add(item);
                            }
                        }
                    }

                    popup.add(subMenu);

                    subMenu = new JMenu(
                        finalPrefs.getString("lang.event.add.global")
                        + " " + timeString);
                    if (process == null) {
                        subMenu.setEnabled(false);
                    } else {
                        JMenu subSubMenu = null;
                        for (final VSCreateTask createTask : createTasks) {
                            if (createTask.isDummy()) {
                                if (subSubMenu != null)
                                    subMenu.add(subSubMenu);
                                subSubMenu = new JMenu(
                                    createTask.getMenuText());
                            } else {
                                item = new JMenuItem(createTask.getMenuText());
                                item.addActionListener(new ActionListener() {
                                    public void actionPerformed(ActionEvent e) {
                                        VSTask task =
                                            createTask.createTask(process,
                                                                  xPosTime,
                                                                  false);
                                        taskManager.addTask(
                                            task, VSTaskManager.PROGRAMMED);
                                        finalSimulator.updateTaskManagerTable();
                                    }
                                });
                                subSubMenu.add(item);
                            }
                        }
                    }

                    if (finalPrefs.getBoolean("sim.mode.expert"))
                        popup.add(subMenu);

                    popup.addSeparator();

                    item = new JMenuItem(
                        finalPrefs.getString("lang.process.add.new"));

                    item.addActionListener(actionListener);
                    popup.add(item);


                    popup.show(me.getComponent(), me.getX(), me.getY());

                } else {
                    editProcess(process);
                }
            }

            public void mouseExited(MouseEvent e) {
                if (highlightedProcess != null) {
                    highlightedProcess.highlightOff();
                    highlightedProcess = null;
                    paint();
                }
            }

            public void mouseEntered(MouseEvent e) { }

            public void mousePressed(MouseEvent e) { }

            public void mouseReleased(MouseEvent e) { }

        });
        addMouseMotionListener(new MouseMotionListener() {
            public void mouseDragged(MouseEvent e) { }

            public void mouseMoved(MouseEvent e) {
                VSInternalProcess p = getProcessAtYPos(e.getY());

                if (p == null) {
                    if (highlightedProcess != null) {
                        highlightedProcess.highlightOff();
                        highlightedProcess = null;
                    }

                    if (isPaused)
                        paint();

                    return;
                }

                if (highlightedProcess != null) {
                    if (highlightedProcess.getProcessID() != p.getProcessID()) {
                        highlightedProcess.highlightOff();
                        highlightedProcess = p;
                        p.highlightOn();
                    }
                } else {
                    highlightedProcess = p;
                    p.highlightOn();
                }

                if (isPaused)
                    paint();
            }

        });

        addHierarchyBoundsListener(new HierarchyBoundsListener() {
            public void ancestorMoved(HierarchyEvent e) { }

            public void ancestorResized(HierarchyEvent e) {
                recalcOnChange();
            }
        });
    }

    /**
     * This method gets called if the window border of the simulator canvas
     * has changed. This method contains very ugly code. But this has to be in
     * order to gain performance!
     */
    private void recalcOnChange() {
        synchronized (processes) {
            if (processes.size() == 0)
                return;
        }

        processlineColor = prefs.getColor("col.process.line");
        processSecondlineColor = prefs.getColor("col.process.secondline");
        processSeplineColor = prefs.getColor("col.process.sepline");
        messageArrivedColor = prefs.getColor("col.message.arrived");
        messageSendingColor = prefs.getColor("col.message.sending");
        messageLostColor = prefs.getColor("col.message.lost");
        backgroundColor = prefs.getColor("col.background");

        paintSize = simulator.getPaintSize();
        xPaintSize = simulator.getWidth() -
                     (3 * XOFFSET + simulator.getSplitSize());
        yDistance = (simulator.getPaintSize() -
                     2 * (YOFFSET + YOUTER_SPACEING))/ numProcesses;
        xpaintsize_dividedby_untiltime = xPaintSize / (double) untilTime;


        synchronized (messageLines) {
            for (VSMessageLine messageLine : messageLines)
                messageLine.recalcOnChange();
        }

        /* paintProcesses optimization, precalc things */
        {
            xoffset_plus_xpaintsize = XOFFSET + (int) xPaintSize;
            if (numProcesses > 1)
                paintProcessesOffset =
                    (int) ((paintSize-2* (YOFFSET+
                                          YOUTER_SPACEING+YSEPLINE_SPACEING))/
                           (numProcesses-1));
            else
                paintProcessesOffset =
                    (int) ((paintSize-2*(YOFFSET+
                                         YOUTER_SPACEING+YSEPLINE_SPACEING)));
        }

        /* paintSecondlines optimization, precalc things */
        {
            int yMax = YOFFSET + YOUTER_SPACEING +
                       (int) (numProcesses * yDistance);
            paintSecondlinesSeconds = (int) untilTime / 1000;
            paintSecondlinesLine[1] = YOFFSET;
            paintSecondlinesLine[3] = yMax;
            paintSecondlinesYStringPos1 = paintSecondlinesLine[1] - 5;
            paintSecondlinesYStringPos2 = paintSecondlinesLine[3] + 15;
        }

        /* paitnGlobalTime optimization, precalc things */
        {
            paintGlobalTimeYPosition = YOFFSET + YOUTER_SPACEING +
                                       (int) (numProcesses * yDistance);
        }

        if (strategy != null) {
            synchronized (strategy) {
                g = (Graphics2D) strategy.getDrawGraphics();
                g.setColor(backgroundColor);
                if (isAntiAliased)
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                       RenderingHints.VALUE_ANTIALIAS_ON);
            }
        }
    }

    /**
     * Updates the simulator.
     *
     * @param globalTime the global time
     * @param lastGlobalTime the last global time
     */
    private void updateSimulator(long globalTime, long lastGlobalTime) {
        if (isPaused || hasFinished)
            return;

        long lastSimulatorTime = simulatorTime;
        long offset = globalTime - lastGlobalTime;

        clockOffset += offset * clockSpeed;

        while (clockOffset >= 1) {
            --clockOffset;
            ++simulatorTime;
        }

        if (simulatorTime > untilTime)
            simulatorTime = untilTime;

        offset = simulatorTime - lastSimulatorTime;

        for (long l = 0; l < offset; ++l)
            taskManager.runTasks(l, offset, lastSimulatorTime);

        synchronized (processes) {
            for (VSInternalProcess process : processes)
                process.syncTime(simulatorTime);
        }
    }

    /**
     * Paints the simulator.
     */
    public void paint() {
        while (getBufferStrategy() == null) {
            createBufferStrategy(3);
            strategy = getBufferStrategy();

            if (strategy != null) {
                g = (Graphics2D) strategy.getDrawGraphics();
                g.setColor(backgroundColor);
            }
        }

        synchronized (strategy) {
            if (isAntiAliasedChanged) {
                if (isAntiAliased)
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                       RenderingHints.VALUE_ANTIALIAS_ON);
                else
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                       RenderingHints.VALUE_ANTIALIAS_OFF);
                isAntiAliasedChanged = false;
            }

            g.fillRect(0, 0, getWidth(), getHeight());
            long globalTime = simulatorTime;

            globalTimeXPosition = getTimeXPosition(globalTime);
            paintSecondlines(g);
            paintProcesses(g, globalTime);
            paintGlobalTime(g, globalTime);

            synchronized (messageLines) {
                synchronized (messageLinesToRemove) {
                    if (messageLinesToRemove.size() > 0) {
                        for (VSMessageLine removeThis : messageLinesToRemove)
                            messageLines.remove(removeThis);
                        messageLinesToRemove.clear();
                    }
                }

                for (VSMessageLine line : messageLines)
                    line.draw(g, globalTime);
            }

            g.setColor(backgroundColor);
            strategy.show();
        }
    }

    /**
     * Paints the processes.
     *
     * @param g the graphics object
     * @param globalTime the global time
     */
    private void paintProcesses(Graphics2D g, long globalTime) {
        /* First paint the horizontal process timelines
         * Second paint the processes
         */
        final int yOffset = YOFFSET + YOUTER_SPACEING + YSEPLINE_SPACEING;
        final int xPoints[] = { XOFFSET, xoffset_plus_xpaintsize,
                                xoffset_plus_xpaintsize, XOFFSET, XOFFSET
                              };
        final int yPoints[] = { yOffset, yOffset, yOffset + LINE_WIDTH,
                                yOffset + LINE_WIDTH, yOffset
                              };

        synchronized (processes) {
            for (VSInternalProcess process : processes) {
                final long localTime = process.getTime();

                g.setColor(process.getColor());
                g.fillPolygon(xPoints, yPoints, 5);

                if (process.hasCrashed()) {
                    g.setColor(process.getCrashedColor());
                    final Long crashHistory[] = process.getCrashHistoryArray();
                    final int length = crashHistory.length;

                    for (int i = 0; i < length; i += 2) {
                        final int crashStartPos =
                            (int) getTimeXPosition(
                                crashHistory[i].longValue());
                        int crashEndPos;

                        if (i == length - 1)
                            crashEndPos = xoffset_plus_xpaintsize;
                        else
                            crashEndPos = (int) getTimeXPosition(
                                              crashHistory[i+1].longValue());

                        final int xPointsCrashed[] = {
                            crashStartPos, crashEndPos,
                            crashEndPos, crashStartPos, crashStartPos
                        };
                        g.fillPolygon(xPointsCrashed, yPoints, 5);
                    }
                }

                g.setColor(process.getColor());
                g.drawString("P" + process.getProcessID() + ":", XOFFSET - 30,
                             yPoints[0] + LINE_WIDTH);

                final long tmp = localTime > untilTime ? untilTime : localTime;
                final int xPos = 1 + (int) getTimeXPosition(tmp);
                final int yStart = yPoints[0] - 14;
                final int yEnd = yPoints[0];

                g.setColor(processlineColor);
                g.drawLine(xPos, yStart, xPos, yEnd);
                g.drawString(localTime+"ms", xPos + 2, yStart + TEXT_SPACEING);

                if (showLamport)
                    paintTime(g, process.getLamportTimeArray(), process,
                              yStart, 25);
                else if (showVectorTime)
                    paintTime(g, process.getVectorTimeArray(), process,
                              yStart, 20 * numProcesses);

                for (int i = 0; i < 5; ++i)
                    yPoints[i] += paintProcessesOffset;
            }
        }
    }

    /**
     * Paints the time. (e.g. lamport time or vector time)
     *
     * @param g the graphics object
     * @param times the times
     * @param process the process
     * @param yStart the y start
     * @param distance the distance
     */
    private void paintTime(final Graphics2D g, final VSTime times[],
                           final VSInternalProcess process, final int yStart,
                           final int distance) {

        final int lastPos[] = { -1, -1, -1, -1 };

        for (VSTime time : times) {
            int xPos = (int) getTimeXPosition(time.getGlobalTime());
            int bestRow[] = { -1, -1 };

            for (int i = 0; i < 4; ++i) {
                if (lastPos[i] != -1) {
                    int diff = xPos - lastPos[i];
                    if (diff > distance) {
                        bestRow[0] = i;
                        bestRow[1] = -1;
                        break;
                    } else if (bestRow[0] == -1) {
                        bestRow[0] = i;
                        bestRow[1] = diff;
                    } else if (diff > bestRow[1]) {
                        bestRow[0] = i;
                        bestRow[1] = diff;
                    }
                } else {
                    bestRow[0] = i;
                    bestRow[1] = -1;
                    break;
                }
            }

            final int row = bestRow[0];
            if (bestRow[1] != -1)
                xPos += distance - bestRow[1];

            g.drawString(time.toString(), xPos + 2, yStart + 3 *
                         TEXT_SPACEING + row * ROW_HEIGHT);
            lastPos[row] = xPos;
        }
    }

    /**
     * Paint the second lines.
     *
     * @param g the graphics object
     */
    private void paintSecondlines(Graphics2D g) {
        g.setColor(processSecondlineColor);

        int i;
        for (i = 0; i <= paintSecondlinesSeconds; i += secondsSpaceing) {
            paintSecondlinesLine[0] = paintSecondlinesLine[2] =
                                          (int) getTimeXPosition(i*1000);
            g.drawLine(paintSecondlinesLine[0], paintSecondlinesLine[1],
                       paintSecondlinesLine[2], paintSecondlinesLine[3]);

            final int xStringPos = paintSecondlinesLine[0] - 5;
            g.drawString(i+"s", xStringPos, paintSecondlinesYStringPos1);
            if (!showVectorTime && !showLamport)
                g.drawString(i+"s", xStringPos, paintSecondlinesYStringPos2);
        }

        if (i > paintSecondlinesSeconds) {
            paintSecondlinesLine[0] = paintSecondlinesLine[2] =
                                          (int) getTimeXPosition(untilTime);
            g.drawLine(paintSecondlinesLine[0], paintSecondlinesLine[1],
                       paintSecondlinesLine[2], paintSecondlinesLine[3]);
        }
    }

    /**
     * Paints the global time.
     *
     * @param g the graphics object
     * @param globalTime the global time
     */
    private void paintGlobalTime(Graphics2D g, long globalTime) {
        g.setColor(processSeplineColor);
        final int xOffset = (int) globalTimeXPosition;

        final int xPoints[] = {
            xOffset, xOffset + SEPLINE_WIDTH,
            xOffset + SEPLINE_WIDTH, xOffset, xOffset
        };
        final int yOffset = YOFFSET - 8;
        final int yPoints[] = {
            yOffset, yOffset, paintGlobalTimeYPosition,
            paintGlobalTimeYPosition, yOffset
        };

        g.fillPolygon(xPoints, yPoints, 5);
        g.drawString(globalTime+"ms", xPoints[1]+1, yPoints[0]+TEXT_SPACEING);
    }

    /**
     * Gets the process at a specified y pos.
     *
     * @param yPos the y pos
     *
     * @return the process at y pos
     */
    private VSInternalProcess getProcessAtYPos(int yPos) {
        final int reachDistance = (int) (yDistance/3);
        int y = YOFFSET + YOUTER_SPACEING + YSEPLINE_SPACEING;

        int yOffset = numProcesses > 1
                      ?  (int) ((paintSize-2*
                                 (YOFFSET+YOUTER_SPACEING+YSEPLINE_SPACEING))/
                                (numProcesses-1))
                      :  (int) ((paintSize-2*
                                 (YOFFSET+YOUTER_SPACEING+YSEPLINE_SPACEING)));

        for (int i = 0; i < numProcesses; ++i) {
            if (yPos < y + reachDistance && yPos > y - reachDistance -
                    LINE_WIDTH) {
                VSInternalProcess process = null;
                synchronized (processes) {
                    process = processes.get(i);
                }
                return process;
            }
            y += yOffset;
        }

        return null;
    }

    /**
     * Gets the x position of the given time.
     *
     * @param time the time
     *
     * @return the time x position
     */
    private double getTimeXPosition(long time) {
        return XOFFSET + xpaintsize_dividedby_untiltime * time;
    }

    /**
     * Gets the time of a given x position
     *
     * @param xPos the x position
     *
     * @return the time
     */
    private long getXPositionTime(double xPos) {
        xPos -= XOFFSET;

        if (xPos <= 0)
            return 0;

        else if (xPos >= xPaintSize)
            return untilTime;

        return (long) ((untilTime/xPaintSize) * xPos);
    }

    /**
     * Gets the process y position.
     *
     * @param i the process num
     *
     * @return the process y position
     */
    private int getProcessYPosition(int i) {
        int y;

        if (numProcesses > 1)
            y = (int) ((paintSize -
                        2 * (YOFFSET + YOUTER_SPACEING + YSEPLINE_SPACEING))/
                       (numProcesses-1));
        else
            y = (int) ((paintSize -
                        2 * (YOFFSET + YOUTER_SPACEING + YSEPLINE_SPACEING)));

        return y * (i - 1) + YOFFSET + YOUTER_SPACEING + YSEPLINE_SPACEING;
    }

    /**
     * Gets the time.
     *
     * @return the time
     */
    public long getTime() {
        return simulatorTime;
    }

    /**
     * Gets the until time.
     *
     * @return the until time
     */
    public long getUntilTime() {
        return untilTime;
    }

    /**
     * Gets the start time.
     *
     * @return the start time
     */
    public long getStartTime() {
        return startTime;
    }

    /**
     * Gets the next process id.
     *
     * @return the next process id
     */
    public int processIDCount() {
        return ++processCounter;
    }

    /**
     * Gets the task manager.
     *
     * @return the task manager
     */
    public VSTaskManager getTaskManager() {
        return taskManager;
    }

    /**
     * Gets the num of processes.
     *
     * @return the num of processes
     */
    public int getNumProcesses() {
        return numProcesses;
    }

    /**
     * Gets the specified process.
     *
     * @param processNum the process num to get the process of
     *
     * @return the process
     */
    public VSInternalProcess getProcess(int processNum) {
        synchronized (processes) {
            if (processNum >= processes.size())
                return null;

            return processes.get(processNum);
        }
    }

    /* (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    public void run() {
        while (true) {
            while (!hasThreadStopped && (isPaused || hasFinished ||
                                         isResetted)) {
                try {
                    Thread.sleep(100);
                    paint();

                } catch (Exception e) {
                    System.out.println(e);
                }
            }

            if (hasThreadStopped)
                break; /* Exit the thread */

            while (!isPaused && !hasThreadStopped) {
                try {
                    Thread.sleep(threadSleep);
                } catch (Exception e) {
                    System.out.println(e);
                }

                updateSimulator(time, lastTime);

                if (simulatorTime == untilTime) {
                    finish();
                    break;
                }

                paint();
                lastTime = time;
                time = System.currentTimeMillis() - startTime;
            }

            updateSimulator(time, lastTime);
            paint();
        }
    }

    /**
     * Starts/plays the simulator.
     */
    public void play() {
        loging.log(prefs.getString("lang.simulator.started"));
        final long currentTime = System.currentTimeMillis();

        synchronized (processes) {
            for (VSInternalProcess p : processes)
                p.play();
        }

        if (isResetted)
            isResetted = false;

        else if (hasFinished)
            hasFinished = false;

        if (isPaused) {
            isPaused = false;
            startTime += currentTime - pauseTime;
            time = currentTime - startTime;

        } else {
            startTime = currentTime;
            time = 0;
        }

        paint();
    }

    /**
     * Called if the simulator has finished.
     */
    public void finish() {
        synchronized (processes) {
            for (VSInternalProcess p : processes)
                p.finish();
        }

        simulator.finish();
        hasFinished = true;
        loging.log(prefs.getString("lang.simulator.finished"));
        paint();

        if (prefs.getBoolean("sim.periodic")) {
            VSSimulatorFrame simulatorFrame = simulator.getSimulatorFrame();
            simulatorFrame.resetCurrentSimulator();
            simulatorFrame.startCurrentSimulator();
        }
    }

    /**
     * Call this, in order to pause the simulator.
     */
    public void pause() {
        isPaused = true;
        synchronized (processes) {
            for (VSInternalProcess p : processes)
                p.pause();
        }

        pauseTime = System.currentTimeMillis();
        loging.log(prefs.getString("lang.simulator.paused"));
        paint();
    }

    /**
     * Call this, in order to reset the simulator.
     */
    public void reset() {
        if (!isResetted) {
            loging.log(prefs.getString("lang.simulator.resetted"));

            isResetted = true;
            isPaused = false;
            hasFinished = false;
            startTime = System.currentTimeMillis();
            time = 0;
            lastTime = 0;
            clockOffset = 0;
            simulatorTime = 0;

            synchronized (processes) {
                for (VSInternalProcess process : processes)
                    process.reset();
            }

            /* Reset the task manager AFTER the processes, for the programmed
               tasks */
            taskManager.reset();

            synchronized (processes) {
                for (VSInternalProcess process : processes)
                    process.createRandomCrashTask();
            }

            synchronized (messageLines) {
                messageLines.clear();
            }

            synchronized (messageLinesToRemove) {
                messageLinesToRemove.clear();
            }

            paint();
            loging.clear();
        }
    }

    /**
     * Stops the thread of the simulator.
     */
    public void stopThread() {
        hasThreadStopped = true;
    }

    /**
     * Checks if the thread has been stopped.
     *
     * @return true, if is thread has stopped
     */
    public boolean hasThreadStopped() {
        return hasThreadStopped;
    }

    /**
     * Sets, if the the lamport time should be shown. It implicitly disables
     * the vector time.
     *
     * @param showLamport true, if the lamport time should be shown
     */
    public void showLamport(boolean showLamport) {
        this.showLamport = showLamport;
        if (isPaused)
            paint();
    }

    /**
     * Sets, if the vector time should be shown. It implicitly disables the
     * lamport time.
     *
     * @param showVectorTime true, if the vector time should be shown
     */
    public void showVectorTime(boolean showVectorTime) {
        this.showVectorTime = showVectorTime;
        if (isPaused)
            paint();
    }

    /**
     * Sets if the simulator graphics are anti aliased.
     *
     * @param isAntiAliased true, if the simulator is anti aliased
     */
    public void isAntiAliased(boolean isAntiAliased) {
        this.isAntiAliased = isAntiAliased;
        this.isAntiAliasedChanged = true;
        if (isPaused)
            paint();
    }

    /**
     * Sends a message.
     *
     * @param message the message
     */
    public void sendMessage(VSMessage message) {
        VSTask task = null;
        VSAbstractEvent receiveEvent = null;
        VSInternalProcess sendingProcess = (VSInternalProcess)
                                           message.getSendingProcess();
        long deliverTime, outageTime, durationTime;
        boolean recvOwn = prefs.getBoolean("sim.message.own.recv");

        synchronized (processes) {
            for (VSInternalProcess receiverProcess : processes) {
                if (receiverProcess.equals(sendingProcess)) {
                    if (recvOwn) {
                        deliverTime = sendingProcess.getGlobalTime();
                        receiveEvent = new VSMessageReceiveEvent(message);
                        task = new VSTask(deliverTime, receiverProcess,
                                          receiveEvent, VSTask.GLOBAL);
                        taskManager.addTask(task);
                    }

                } else {
                    durationTime = sendingProcess.getDurationTime();

                    if (prefs.getBoolean("sim.message.sendingtime.mean")) {
                        durationTime += receiverProcess.getDurationTime();
                        durationTime /= 2;
                    }

                    deliverTime = sendingProcess.getGlobalTime() +
                                  durationTime;

                    if (prefs.getBoolean("sim.message.prob.mean"))
                        outageTime = sendingProcess.getARandomMessageOutageTime(
                                         durationTime, receiverProcess);
                    else
                        outageTime = sendingProcess.getARandomMessageOutageTime(
                                         durationTime, null);

                    receiveEvent = new VSMessageReceiveEvent(message);
                    task = new VSTask(deliverTime, receiverProcess,
                                      receiveEvent, VSTask.GLOBAL);

                    /* Only add a 'receiving message' task if the message will
                       not get lost! */
                    if (outageTime == -1)
                        taskManager.addTask(task);

                    synchronized (messageLines) {
                        messageLines.add(
                            new VSMessageLine(receiverProcess,
                                              sendingProcess.getGlobalTime(),
                                              deliverTime, outageTime,
                                              sendingProcess.getProcessNum(),
                                              receiverProcess.getProcessNum(),
                                              task));
                    }
                }
            }
        }
    }

    /**
     * Edits the process.
     *
     * @param processNum the process num
     */
    public void editProcess(int processNum) {
        synchronized (processes) {
            VSInternalProcess process = processes.get(processNum);
            /* May be null if another thread changed the processes arraylist
               before this process actually called editProcess */
            if (process != null)
                editProcess(process);
        }
    }

    /**
     * Edits the process.
     *
     * @param process the process
     */
    public void editProcess(VSInternalProcess process) {
        if (process != null) {
            process.updatePrefs();
            new VSEditorFrame(prefs, simulator.getSimulatorFrame(),
                              new VSProcessEditor(prefs, process));
        }
    }

    /**
     * Gets the processes array.
     *
     * @return the processes array
     */
    public ArrayList<VSInternalProcess> getProcessesArray() {
        ArrayList<VSInternalProcess> arr = new ArrayList<VSInternalProcess>();

        synchronized (processes) {
            for (VSInternalProcess process : processes)
                arr.add(process);
        }

        return arr;
    }

    /**
     * Gets the processes IDs.
     *
     * @return the processes IDs
     */
    public Integer[] getProcessIDs() {
        Integer pids[] = null;

        synchronized (processes) {
            pids = new Integer[numProcesses];
            for (int i = 0; i < numProcesses; ++i)
                pids[i] = Integer.valueOf(processes.get(i).getProcessID());
        }

        return pids;
    }

    /**
     * Gets the processes.
     *
     * @return the processes
     */
    public ArrayList<VSInternalProcess> getProcesses() {
        return processes;
    }

    /**
     * Updates from the prefs. Called by the VSSimulatorEditor if values
     * have been saved.
     */
    public void updateFromPrefs() {
        untilTime = prefs.getInteger("sim.seconds") * 1000;
        clockSpeed = prefs.getFloat("sim.clock.speed");

        secondsSpaceing = (int) (untilTime / 15000);
        if (secondsSpaceing == 0)
            secondsSpaceing = 1;

        threadSleep = (int) (untilTime / 7500);
        if (threadSleep == 0)
            threadSleep = 1;

        recalcOnChange();
    }

    /**
     * Removes a specific message line from the painting area.
     *
     * @param messageLine the message line to remove
     */
    private void removeMessageLine(VSMessageLine messageLine) {
        synchronized (messageLinesToRemove) {
            messageLinesToRemove.add(messageLine);
        }
    }

    /**
     * Removes a process from the simulator.
     *
     * @param process the process
     */
    private void removeProcess(VSInternalProcess process) {
        if (numProcesses == 1) {
            simulator.getSimulatorFrame().removeSimulator(simulator);

        } else {
            int index = 0;
            synchronized (processes) {
                index = processes.indexOf(process);
                processes.remove(index);

                for (VSInternalProcess p : processes)
                    p.removedAProcessAtIndex(index);

                numProcesses = processes.size();
            }

            taskManager.removeTasksOf(process);
            simulator.removedAProcessAtIndex(index);
            recalcOnChange();

            ArrayList<VSMessageLine> removeThose =
                new ArrayList<VSMessageLine>();

            synchronized (messageLines) {
                for (VSMessageLine line : messageLines)
                    if (line.removedAProcessAtIndex(index))
                        removeThose.add(line);

                for (VSMessageLine line : removeThose) {
                    VSTask deliverTask = line.getTask();

                    if (deliverTask != null)
                        taskManager.removeTask(deliverTask);

                    messageLines.remove(line);
                }
            }
        }
    }

    /**
     * Creates a process with the specified num.
     *
     * @param processNum the process num
     *
     * @return the new process
     */
    private VSInternalProcess createProcess(int processNum) {
        VSInternalProcess process =
            new VSInternalProcess(prefs, processNum, this, loging);
        loging.log(prefs.getString("lang.process.new") + "; " + process);
        return process;
    }

    /**
     * Adds a new process to the simulator.
     *
     * @return The process which has been added
     */
    private VSInternalProcess addProcess() {
        VSInternalProcess newProcess = null;
        //int foo = -1;
        //System.out.println("ADD " + ++foo);
        synchronized (processes) {
            //System.out.println("ADD " + ++foo);
            numProcesses = processes.size() + 1;
            //System.out.println("ADD " + ++foo);
            newProcess = createProcess(processes.size());
            //System.out.println("ADD " + ++foo);
            //System.out.println("ADD " + ++foo);
        }

        //System.out.println("ADD " + ++foo);
        addProcess(newProcess);
        //System.out.println("ADD " + ++foo);
        return newProcess;
    }

    /**
     * Adds a the given process to the simulator.
     *
     * @newProcess The process to add
     */
    private void addProcess(VSInternalProcess newProcess) {
        //int foo = -1;
        //System.out.println("ADD_ " + ++foo);
        synchronized (processes) {
            //System.out.println("ADD_ " + ++foo);
            processes.add(newProcess);

            for (VSInternalProcess process : processes)
                if (!process.equals(newProcess))
                    process.addedAProcess();
            //System.out.println("ADD_ " + ++foo);
        }

        //System.out.println("ADD_ " + ++foo);
        recalcOnChange();
        //System.out.println("ADD_ " + ++foo);
        simulator.addProcessAtIndex(processes.size()-1);
        //System.out.println("ADD_ " + ++foo);
    }

    /**
     * Checks if the simulation is paused.
     *
     * @return true, if the simulation is paused
     */
    boolean isPaused() {
        return isPaused;
    }

    /**
     * Checks if the simulation is resetted.
     *
     * @return true, if the simulation is resetted
     */
    boolean isResetted() {
        return isResetted;
    }

    /**
     * Checks if the simulation has finished
     *
     * @return true, if the simulation has finished
     */
    boolean hasFinished() {
        return hasFinished;
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

        objectOutputStream.writeObject(Integer.valueOf(processCounter));

        synchronized (processes) {
            objectOutputStream.writeObject(Integer.valueOf(numProcesses));
            for (VSInternalProcess process : processes)
                process.serialize(serialize, objectOutputStream);
        }

        taskManager.serialize(serialize, objectOutputStream);

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
            System.out.println("Deserializing: VSSimulatorVisualization");

        /** For later backwards compatibility, to add more stuff */
        objectInputStream.readObject();

        processCounter = ((Integer) objectInputStream.readObject()).intValue();

        int num = ((Integer) objectInputStream.readObject()).intValue();
        loging.clear();

        if (num > numProcesses) {
            for (int i = numProcesses; i < num; ++i)
                addProcess();
        } else {
            int oldNum = numProcesses;
            for (int i = num; i < oldNum; ++i)
                removeProcess(getProcess(0));
        }

        for (int i = 0; i < num; ++i)
            processes.get(i).deserialize(serialize, objectInputStream);

        taskManager.deserialize(serialize, objectInputStream);

        /** For later backwards compatibility, to add more stuff */
        objectInputStream.readObject();
    }
}
