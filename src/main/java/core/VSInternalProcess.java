package core;

import java.awt.Color;

import core.time.VSVectorTime;
import events.VSAbstractEvent;
import events.VSRegisteredEvents;
import events.implementations.VSProcessCrashEvent;
import events.implementations.VSVectorClockMonitor;
import prefs.VSPrefs;
import protocols.VSAbstractProtocol;
import simulator.VSLogging;
import simulator.VSSimulatorVisualization;
import utils.VSPriorityQueue;

/**
 * Internal process implementation for the distributed systems simulator.
 * This class extends {@link VSAbstractProcess} with simulation-specific features
 * including:
 * <ul>
 *   <li>Time synchronization with global simulation clock</li>
 *   <li>Clock variance simulation for realistic distributed behavior</li>
 *   <li>Visual highlighting and color management</li>
 *   <li>Vector clock monitoring for timestamp-triggered events</li>
 *   <li>Process crash simulation</li>
 *   <li>Protocol and event management</li>
 * </ul>
 * 
 * <p>Each process maintains its own local time that drifts from global time
 * based on configured clock variance, simulating realistic clock skew in
 * distributed systems.</p>
 *
 * @see VSAbstractProcess
 * @see VSSimulatorVisualization
 * @author Paul C. Buetow
 */
public class VSInternalProcess extends VSAbstractProcess {
    /** The vector clock monitor for timestamp-triggered events */
    private VSVectorClockMonitor vectorClockMonitor;
    
    /** Optional message handler for decoupled message sending */
    private simulator.messaging.MessageHandler messageHandler;
    
    /**
     * Instantiates a new process.
     *
     * @param prefs the simulator's default prefs
     * @param processNum the process num
     * @param simulatorVisualization the simulator canvas
     * @param loging the loging object
     */
    public VSInternalProcess(VSPrefs prefs, int processNum,
                             VSSimulatorVisualization simulatorVisualization,
                             VSLogging loging) {
        super(prefs, processNum, simulatorVisualization, loging);
        vectorClockMonitor = new VSVectorClockMonitor(this);
    }

    /**
     * Updates this process's configuration from its preference settings.
     * Called by {@link simulator.VSProcessEditor} after editing is complete
     * to apply any configuration changes.
     * 
     * <p>This method updates:</p>
     * <ul>
     *   <li>Clock variance for time drift simulation</li>
     *   <li>Local time value</li>
     *   <li>Process colors</li>
     *   <li>Random crash parameters</li>
     * </ul>
     */
    public synchronized void updateFromPrefs() {
        setClockVariance(getFloat("process.clock.variance"));
        setLocalTime(getLong("process.localtime"));
        crashedColor = getColor("col.process.crashed");
        createRandomCrashTask();
    }

    /**
     * Saves this process's current state to its preference settings.
     * Called by {@link simulator.VSProcessEditor} before editing begins
     * to ensure the editor shows current values.
     * 
     * <p>This method saves:</p>
     * <ul>
     *   <li>Current clock variance</li>
     *   <li>Current local time</li>
     * </ul>
     */
    public synchronized void updatePrefs() {
        setFloat("process.clock.variance", getClockVariance());
        setLong("process.localtime", getTime());
    }

    /**
     * Synchronizes this process's time with the global simulation time.
     * This method simulates realistic clock drift using the configured
     * clock variance.
     * 
     * <p>The synchronization algorithm:</p>
     * <ol>
     *   <li>Calculates time elapsed since last sync</li>
     *   <li>Advances local time by elapsed time</li>
     *   <li>Applies clock drift based on variance</li>
     *   <li>Accumulates fractional time in clockOffset</li>
     *   <li>Ensures time never goes negative</li>
     * </ol>
     * 
     * <p>Called repeatedly by {@link VSSimulatorVisualization} during simulation.</p>
     *
     * @param globalTime the current global simulation time
     */
    public synchronized void syncTime(final long globalTime) {
        final long currentGlobalTimestep = globalTime - this.globalTime;
        this.globalTime = globalTime;

        localTime += currentGlobalTimestep;
        clockOffset += currentGlobalTimestep * (double) clockVariance;

        while (clockOffset >= 1) {
            clockOffset -= 1;
            ++localTime;
        }

        while (clockOffset <= -1) {
            clockOffset += 1;
            --localTime;
        }

        /* We do not want a negative time */
        if (localTime < 0)
            localTime = 0;
    }

    /**
     * Activates visual highlighting for this process.
     * The process's current color is saved and replaced with the highlight color.
     * Use {@link #highlightOff()} to restore the original color.
     * 
     * @see #highlightOff()
     */
    public synchronized void highlightOn() {
        tmpColor = currentColor;
        currentColor = getColor("col.process.highlight");
        isHighlighted = true;
    }

    /**
     * Unhighlights the process.
     */
    public synchronized void highlightOff() {
        currentColor = tmpColor;
        isHighlighted = false;
    }

    /**
     * Resets the process.
     */
    public synchronized void reset() {
        isPaused = true;
        isCrashed = false;
        hasCrashed = false;
        localTime = 0;
        globalTime = 0;
        clockOffset = 0;

        for (VSAbstractProtocol protocol : protocolsToReset)
            protocol.reset();

        setCurrentColor(getColor("col.process.default"));
        resetTimeFormats();
        
        // Clear any vector clock monitor events
        if (vectorClockMonitor != null) {
            vectorClockMonitor.clearVectorEvents();
        }
    }

    /**
     * Creates the random crash task. The crash task will be created only if
     * the process is not crashed atm. and if
     * VSInternalProcess.getARandomCrashTime() * returns a non-negative value.
     * The random crash task uses the simulaion's global time for its
     * scheduling.
     */
    public synchronized void createRandomCrashTask() {
        if (!isCrashed) {
            VSTaskManager taskManager = simulatorVisualization.getTaskManager();
            long crashTime = getARandomCrashTime();

            if (crashTime < 0)
                return;

            if (randomCrashTask != null)
                taskManager.removeTask(randomCrashTask);

            if (crashTime >= getGlobalTime())  {
                VSAbstractEvent event = new VSProcessCrashEvent();
                randomCrashTask = new VSTask(crashTime, this, event,
                                             VSTask.GLOBAL);
                taskManager.addTask(randomCrashTask);

            } else {
                randomCrashTask = null;
            }
        }
    }

    /**
     * Generates a random percentage value between 0 and 100 (inclusive).
     * Uses this process's dedicated random number generator to ensure
     * reproducible results for a given random seed.
     * 
     * <p>This method is commonly used for:</p>
     * <ul>
     *   <li>Probabilistic event triggering</li>
     *   <li>Random crash simulation</li>
     *   <li>Message loss simulation</li>
     * </ul>
     *
     * @return a random integer between 0 and 100 inclusive
     * @see utils.VSRandom
     */
    public synchronized int getRandomPercentage() {
        return random.nextInt() % constants.VSConstants.PERCENTAGE_RANGE;
    }

    /**
     * Adds to this process's clock offset, adjusting its local time drift.
     * Used by the task manager when tasks modify process time.
     * 
     * <p>The clock offset accumulates fractional time units that are
     * converted to whole time units during {@link #syncTime(long)}.</p>
     *
     * @param add the amount to add to the clock offset (can be negative)
     * @see VSTaskManager#runTasks(long)
     */
    public synchronized void addClockOffset(long add) {
        this.clockOffset += add;
    }

    /**
     * Transitions this process to the 'playing' state.
     * The process resumes normal operation and its color changes
     * to indicate running status.
     * 
     * <p>Called by the simulator when starting or resuming simulation.</p>
     * 
     * @see #pause()
     * @see #finish()
     */
    public synchronized void play() {
        isPaused = false;
        setCurrentColor(getColor("col.process.running"));
    }

    /**
     * Transitions this process to the 'paused' state.
     * The process stops executing and its color changes
     * to indicate stopped status.
     * 
     * <p>Called by the simulator when pausing simulation.</p>
     * 
     * @see #play()
     * @see #finish()
     */
    public synchronized void pause() {
        isPaused = true;
        setCurrentColor(getColor("col.process.stopped"));
    }

    /**
     * Transitions this process to the 'finished' state.
     * The process stops executing and returns to its default color.
     * 
     * <p>Called by the simulator when ending simulation.</p>
     * 
     * @see #play()
     * @see #pause()
     */
    public synchronized void finish() {
        isPaused = true;
        setCurrentColor(getColor("col.process.default"));
    }

    /**
     * Gets the current process' color.
     *
     * @return the current color of the process.
     */
    public synchronized Color getColor() {
        return currentColor;
    }

    /**
     * Gets the color of this process if it's crashed.
     *
     * @return the crashed color
     */
    public synchronized Color getCrashedColor() {
        return crashedColor;
    }

    /**
     * Checks if this process's time has been modified by a task.
     * The task manager uses this flag to determine if clock offset
     * adjustments are needed after task execution.
     * 
     * <p>This flag is set when tasks directly modify the process's
     * local time, requiring synchronization adjustments.</p>
     *
     * @return true if the time has been modified since last check
     * @see VSTaskManager#runTasks(long)
     */
    public synchronized boolean timeModified() {
        return timeModified;
    }

    /**
     * Sets if the time has been modified by a task.
     *
     * @param timeModified true, if it has been modified.
     */
    public synchronized void timeModified(boolean timeModified) {
        this.timeModified = timeModified;
    }

    /**
     * Sets the global time.
     *
     * @param globalTime the new global time
     */
    public synchronized void setGlobalTime(final long globalTime) {
        this.globalTime = globalTime >= 0 ? globalTime : 0;
    }

    /* Gets the duration time of a message to send.
     *
     * @return the duration time
     */
    public synchronized long getDurationTime() {
        final long maxDurationTime = getLong("message.sendingtime.max");
        final long minDurationTime = getLong("message.sendingtime.min");

        if (maxDurationTime <= minDurationTime)
            return minDurationTime;

        final int diff = (int) (maxDurationTime - minDurationTime);

        /* Integer overflow */
        if (diff <= 0)
            return minDurationTime;

        return minDurationTime + random.nextInt(diff+1);
    }

    /**
     * Gets the a random message outage time.
     *
     * @param durationTime the duration time
     *
     * @return the a random message outage time. It will be -1 if the message
     *	will not get lost at all.
     */
    public synchronized long getARandomMessageOutageTime(long durationTime,
            VSInternalProcess receiverProcess) {
        int percentage = (int) ((getInteger("message.prob.outage") +
                                 receiverProcess.getInteger(
                                     "message.prob.outage")) / 2);

        /* Check if the message will have an outage or not */
        if (getRandomPercentage() < percentage) {

            /* Calculate the random outage time! */
            long outageTime = globalTime + random.nextLong(durationTime+1) %
                              simulatorVisualization.getUntilTime();

            return outageTime;
        }

        /* No outage */
        return -1;
    }

    /**
     * Gets the random crash task.
     *
     * @return the random crash task
     */
    public synchronized VSTask getCrashTask() {
        return randomCrashTask;
    }

    /**
     * Checks if the process is paused.
     *
     * @return true, if is paused
     */
    public synchronized boolean isPaused() {
        return isPaused;
    }

    /**
     * Called by a task if the process sends a message.
     *
     * @param message the message to send.
     */
    public synchronized void sendMessage(VSMessage message) {
        StringBuffer buffer = new StringBuffer();
        buffer.append(prefs.getString("lang.message.sent"));
        buffer.append("; ");
        buffer.append(message.toStringFull());
        log(buffer.toString());
        
        // Use message handler if available (for decoupled operation)
        if (messageHandler != null) {
            messageHandler.handleMessage(message);
        } else {
            // Fallback to direct visualization call for backward compatibility
            simulatorVisualization.sendMessage(message);
        }
    }
    
    /**
     * Sets the message handler for decoupled message sending.
     * @param handler the message handler to use
     */
    public void setMessageHandler(simulator.messaging.MessageHandler handler) {
        this.messageHandler = handler;
    }

    /**
     * Gets the simulator canvas.
     *
     * @return the simulator canvas
     */
    public VSSimulatorVisualization getSimulatorCanvas() {
        return simulatorVisualization;
    }

    /**
     * Removes the process at the specified index. Called by the simulator
     * canvas if a process has been removed from the simulator. Needed in
     * order to update the vector time and the local processNum.
     *
     * @param index the index the process has to get removed.
     */
    public synchronized void removedAProcessAtIndex(int index) {
        if (index < processNum)
            --processNum;

        vectorTime.remove(index);
        for (VSVectorTime vectorTime : vectorTimeHistory)
            vectorTime.remove(index);
    }

    /**
     * Added a process. Needed in order to update the vector time's size.
     * Called by the simulator canvas if a process has been added to the
     * simulator.
     */
    public synchronized void addedAProcess() {
        vectorTime.add(Long.valueOf(0));
        for (VSVectorTime vectorTime : vectorTimeHistory)
            vectorTime.add(Long.valueOf(0));
    }

    /**
     * Gets the tasks of the process.
     *
     * @return The tasks
     */
    public VSPriorityQueue<VSTask> getTasks() {
        return tasks;
    }

    /**
     * Sets the tasks of the process.
     *
     * @param tasks The tasks
     */
    public void setTasks(VSPriorityQueue<VSTask> tasks) {
        this.tasks = tasks;
    }

    /**
     * Gets the protocol object.
     *
     * @param protocolClassname the protocol classname
     *
     * @return the protocol object
     */
    public synchronized VSAbstractProtocol getProtocolObject(
        String protocolClassname) {
        VSAbstractProtocol protocol = null;

        if (!objectExists(protocolClassname)) {
            protocol = (VSAbstractProtocol)
                       VSRegisteredEvents.createEventInstanceByClassname(
                           protocolClassname, this);

            setObject(protocolClassname, protocol);
            protocolsToReset.add(protocol);

        } else {
            protocol = (VSAbstractProtocol) getObject(protocolClassname);
        }

        return protocol;
    }

    /**
     * Sets the local time.
     *
     * @param localTime the new local time.
     */
    public synchronized void setLocalTime(final long localTime) {
        if (localTime >= 0)
            this.localTime = localTime;
        else
            this.localTime = 0;
    }

    /* (non-Javadoc)
     * @see core.VSInternalMessage#updateFromPrefs()
     */
    protected void updateFromPrefs_() {
        updateFromPrefs();
    }

    /* (non-Javadoc)
     * @see core.VSInternalMessage#createRandomCrashTask()
     */
    protected void createRandomCrashTask_() {
        createRandomCrashTask();
    }

    /* (non-Javadoc)
     * @see core.VSInternalMessage#getProtocolObjekt(java.util.String)
     */
    protected VSAbstractProtocol getProtocolObject_(String protocolClassname) {
        return getProtocolObject(protocolClassname);
    }
    
    /**
     * Override to trigger vector clock monitor when vector time is increased
     */
    @Override
    public synchronized void increaseVectorTime() {
        super.increaseVectorTime();
        if (vectorClockMonitor != null) {
            vectorClockMonitor.checkVectorEvents();
        }
    }
    
    /**
     * Override to trigger vector clock monitor when vector time is updated
     */
    @Override
    public synchronized void updateVectorTime(VSVectorTime vectorTimeUpdate) {
        super.updateVectorTime(vectorTimeUpdate);
        if (vectorClockMonitor != null) {
            vectorClockMonitor.checkVectorEvents();
        }
    }
    
    /**
     * Get the vector clock monitor for adding timestamp events
     */
    public VSVectorClockMonitor getVectorClockMonitor() {
        return vectorClockMonitor;
    }
}
