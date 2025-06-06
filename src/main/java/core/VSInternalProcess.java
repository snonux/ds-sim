package core;

import java.awt.Color;

import core.time.VSVectorTime;
import events.VSAbstractEvent;
import events.VSRegisteredEvents;
import events.implementations.VSProcessCrashEvent;
import prefs.VSPrefs;
import protocols.VSAbstractProtocol;
import simulator.VSLogging;
import simulator.VSSimulatorVisualization;
import utils.VSPriorityQueue;

/**
 * The class VSInternalProcess, an object of this class represents a process
 * of a simulator.
 *
 * @author Paul C. Buetow
 */
public class VSInternalProcess extends VSAbstractProcess {
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
    }

    /**
     * Called from the VSProcessEditor, after finishing editing! This makes
     * sure that the VSInternalProcess object is using the up to date prefs!
     */
    public synchronized void updateFromPrefs() {
        setClockVariance(getFloat("process.clock.variance"));
        setLocalTime(getLong("process.localtime"));
        crashedColor = getColor("col.process.crashed");
        createRandomCrashTask();
    }

    /**
     * Called from the VSProcessEditor, before starting editing! This makes
     * sure that the editor edits the up to date prefs of the process!
     */
    public synchronized void updatePrefs() {
        setFloat("process.clock.variance", getClockVariance());
        setLong("process.localtime", getTime());
    }

    /**
     * Syncs the process' time. This method is using the clockOffset and
     * clockVariance variables. This method is called repeatedly from the
     * VSSimulatorVisualization in order to update the process' local and global
     * time values.
     *
     * @param globalTime the global time.
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
     * Highlights the process.
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
     * Creates a random percentage 0..100 using the process' own pseudo
     * random number generator object of the VSRandom class.
     *
     * @return A random percentage 0..100.
     */
    public synchronized int getRandomPercentage() {
        return random.nextInt() % 101;
    }

    /**
     * Adds the clock offset. This method is used by the task manager. The
     * clock offset identifies if the local time of the process has changed and
     * how much..
     *
     * @param add the clock offset to add.
     */
    public synchronized void addClockOffset(long add) {
        this.clockOffset += add;
    }

    /**
     * The process' state is 'play'. Called by the simulator canvas.
     */
    public synchronized void play() {
        isPaused = false;
        setCurrentColor(getColor("col.process.running"));
    }

    /**
     * The process' state is 'pause'. Called by the simulator canvas.
     */
    public synchronized void pause() {
        isPaused = true;
        setCurrentColor(getColor("col.process.stopped"));
    }

    /**
     * The process' state is 'Finish'. Called by the simulator canvas.
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
     * Checks if the time has been modified. by a task.
     * This mehod is needed by the task manager in order to add a clock offset
     * to the process object.
     *
     * @return true, if yes
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
        simulatorVisualization.sendMessage(message);
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
}
