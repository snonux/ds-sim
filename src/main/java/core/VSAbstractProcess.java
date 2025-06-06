package core;

import java.awt.Color;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

import core.time.VSLamportTime;
import core.time.VSTime;
import core.time.VSVectorTime;
import prefs.VSPrefs;
import prefs.VSSerializablePrefs;
import protocols.VSAbstractProtocol;
import serialize.VSSerialize;
import simulator.VSLogging;
import simulator.VSSimulatorVisualization;
import utils.VSPriorityQueue;
import utils.VSRandom;
import utils.VSTools;

/**
 * The class VSAbstractProcess, an object of this class represents a process
 * of a simulator.
 *
 * @author Paul C. Buetow
 */
public abstract class VSAbstractProcess extends VSSerializablePrefs {
    /** The data serialization id. */
    protected static final long serialVersionUID = 1L;

    /** The protocols to reset if the simulator is over or the reset
     * button has been pressed.
     */
    protected ArrayList<VSAbstractProtocol> protocolsToReset;

    /** The crash history. represents all crashes of the process using the
     * global simulator time.
     */
    protected ArrayList<Long> crashHistory;

    /** The lamport time history. */
    protected ArrayList<VSLamportTime> lamportTimeHistory;

    /** The vector time history. */
    protected ArrayList<VSVectorTime> vectorTimeHistory;

    /** The color used if the process has crashed. */
    protected Color crashedColor;;

    /** The process' current color. */
    protected Color currentColor;

    /** A temp. color. For internal usage. */
    protected Color tmpColor;

    /** The loging object. */
    protected VSLogging loging;

    /** The simulator's default prefs. */
    protected VSPrefs prefs;

    /** The random generator of the process. */
    protected VSRandom random;

    /** The simulator canvas. */
    protected VSSimulatorVisualization simulatorVisualization;

    /** The random crash task. May be null if there is no such random task. */
    protected VSTask randomCrashTask;

    /** The vector time. */
    protected VSVectorTime vectorTime;

    /** The tasks of the process. DO ONLY MANIPULATE THIS OBJECT WITHIN THE
     * VSTaskManager CLASS! OTHERWISE THE SYNCHRONIZATION IS WRONG! Use the
     * VSAbstractProcess.getTasks() method to get a reference to this object
     * within the VSTaskManager! */
    protected VSPriorityQueue<VSTask> tasks;

    /** The process has crashed. But may be working again. */
    protected boolean hasCrashed;

    /** The process has started. But may be paused or crashed.. */
    protected boolean hasStarted;

    /** The process is crashed. */
    protected boolean isCrashed;

    /** The process is highlighted. */
    protected boolean isHighlighted;

    /** The process is paused. */
    protected boolean isPaused;

    /** The time has been modified in a task. Needed by the task manager to
     * calculate correct offsets.
     */
    protected boolean timeModified;

    /** The clock offset. Used by the task manager and also by the process'
     * clock variance.
     */
    protected double clockOffset;

    /** The clock variance. */
    protected float clockVariance;

    /** The process id. */
    protected int processID;

    /** The process num. It is different to the process id. It represents the
     * array index of there the process is stored at.
      */
    protected int processNum;

    /** The global time. */
    protected long globalTime;

    /** The lamport time. */
    protected long lamportTime;

    /** The local time. */
    protected long localTime;

    /** The Constant DEFAULT_INTEGER_VALUE_KEYS.
     * This array contains all Integer prefs of the process which should show
     * up in the prefs menu! All keys which dont start with "sim." only show
     * up in the extended prefs menu!
     */
    protected static final String DEFAULT_INTEGER_VALUE_KEYS[] = {
        "process.prob.crash",
        "message.prob.outage",
    };

    /** The Constant DEFAULT_LONG_VALUE_KEYS.
     * This array contains all Long prefs of the process which should show
     * up in the prefs menu! All keys which dont start with "sim." only show
     * up in the extended prefs menu!
     */
    protected static final String DEFAULT_LONG_VALUE_KEYS[] = {
        "message.sendingtime.min",
        "message.sendingtime.max",
    };

    /** The Constant DEFAULT_FLOAT_VALUE_KEYS.
     * This array contains all Float prefs of the process which should show
     * up in the prefs menu! All keys which dont start with "sim." only show
     * up in the extended prefs menu!
     */
    protected static final String DEFAULT_FLOAT_VALUE_KEYS[] = {
        "process.clock.variance",
    };

    /** The Constant DEFAULT_COLOR_VALUE_KEYS.
     * This array contains all Color prefs of the process which should show
     * up in the prefs menu! All keys which dont start with "sim." only show
     * up in the extended prefs menu!
     */
    protected static final String DEFAULT_COLOR_VALUE_KEYS[] = {
        "col.process.default",
        "col.process.running",
        "col.process.stopped",
        "col.process.highlight",
        "col.process.crashed",
    };

    /** The Constant DEFAULT_STRING_VALUE_KEYS.
     * This array contains all String prefs of the process which should show
     * up in the prefs menu! All keys which dont start with "sim." only show
     * up in the extended prefs menu!
     */
    protected static final String DEFAULT_STRING_VALUE_KEYS[] = {
    };

    /**
     * Instantiates a new process.
     *
     * @param prefs the simulator's default prefs
     * @param processNum the process num
     * @param simulatorVisualization the simulator canvas
     * @param loging the loging object
     */
    public VSAbstractProcess(VSPrefs prefs, int processNum,
                             VSSimulatorVisualization simulatorVisualization,
                             VSLogging loging) {
        init(prefs, processNum, simulatorVisualization, loging);
    }

    /**
     * Inits a the process.
     *
     * @param prefs the simulator's default prefs
     * @param processNum the process num
     * @param simulatorVisualization the simulator canvas
     * @param loging the loging object
     */
    protected void init(VSPrefs prefs, int processNum,
                        VSSimulatorVisualization simulatorVisualization,
                        VSLogging loging) {
        /* May be not null if called from deserialization */
        if (this.protocolsToReset == null)
            this.protocolsToReset = new ArrayList<VSAbstractProtocol>();

        this.processNum = processNum;
        this.prefs = prefs;
        this.simulatorVisualization = simulatorVisualization;
        this.loging = loging;

        processID = simulatorVisualization.processIDCount();
        random = new VSRandom(processID*processNum+processID+processNum);
        tasks = new VSPriorityQueue<VSTask>();

        initTimeFormats();

        isPaused = true;

        /* Create the super.VSPrefs with it's default prefs */
        fillWithDefaults();

        /* Make local copys in order to have more performance */
        clockVariance = getFloat("process.clock.variance");
        currentColor = getColor("col.process.default");
        crashedColor = getColor("col.process.crashed");

        /* Make additional process settings editable through GUI */
        initLong("process.localtime", localTime,
                 prefs.getString("lang.process.time.local"), "ms");

        createRandomCrashTask_();
    }

    /**
     * Inits the time formats. E.g. lamport and vector time stamps.
     */
    protected void initTimeFormats() {
        lamportTime = 0;
        lamportTimeHistory = new ArrayList<VSLamportTime>();

        vectorTime = new VSVectorTime(0);
        vectorTimeHistory = new ArrayList<VSVectorTime>();
        crashHistory = new ArrayList<Long>();

        final int numProcesses = simulatorVisualization.getNumProcesses();
        for (int i = 0; i < numProcesses; ++i)
            vectorTime.add(Long.valueOf(0));
    }

    /**
     * Reset time formats. E.g. lamport and vector time stamps.
     */
    protected void resetTimeFormats() {
        lamportTime = 0;
        lamportTimeHistory.clear();

        vectorTime = new VSVectorTime(0);
        vectorTimeHistory.clear();
        crashHistory.clear();

        final int numProcesses = simulatorVisualization.getNumProcesses();
        for (int i = numProcesses; i > 0; --i)
            vectorTime.add(Long.valueOf(0));
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
    protected synchronized void addClockOffset(long add) {
        this.clockOffset += add;
    }

    /**
     * Gets the process id.
     *
     * @return the process id
     */
    public synchronized int getProcessID() {
        return processID;
    }

    /**
     * Gets the process num.  The num is different to the process id. It
     * represents the array index of there the process is stored at.
     *
     * @return the process num
     */
    public synchronized int getProcessNum() {
        return processNum;
    }

    /**
     * Sets the process id.
     *
     * @param processID the new process id
     */
    public synchronized void setProcessID(int processID) {
        this.processID = processID;
    }

    /**
     * Gets the process' local time.
     *
     * @return the process' local time
     */
    public synchronized long getTime() {
        return localTime;
    }

    /**
     * Sets the process' local time.
     *
     * @param time the new local time of the process.
     */
    public synchronized void setTime(final long time) {
        if (time >= 0)
            this.localTime = time;
        else
            this.localTime = 0;

        this.timeModified = true;
    }

    /**
     * Checks if the process is crashed.
     *
     * @return true, if is crashed
     */
    public synchronized boolean isCrashed() {
        return isCrashed;
    }

    /**
     * Sets if the process is crashed.
     *
     * @param isCrashed true if the process is crashed.
     */
    public synchronized void isCrashed(boolean isCrashed) {
        this.isCrashed = isCrashed;
        crashHistory.add(Long.valueOf(globalTime));
        if (!hasCrashed)
            hasCrashed = true;
    }

    /**
     * Checks if the process has crashed. The difference to isCrashed is,
     * that the process may be fully functional again after crashing. This
     * method is needed by the simulator canvas in order to see if it should
     * paint 'crashed areas' using the crash history of this process.
     *
     * @return true, if yes
     */
    public synchronized boolean hasCrashed() {
        return hasCrashed;
    }

    /**
     * Gets the global time.
     *
     * @return the global time
     */
    public synchronized long getGlobalTime() {
        return globalTime;
    }

    /**
     * Gets the clock variance.
     *
     * @return the clock variance
     */
    public synchronized float getClockVariance() {
        return clockVariance;
    }

    /**
     * Sets the clock variance.
     *
     * @param clockVariance the new clock variance
     */
    public synchronized void setClockVariance(float clockVariance) {
        /* If negative, only allow < 1 prefs */
        if (clockVariance < 0) {
            int part = (int) -clockVariance;
            if (part > 0) {
                this.clockVariance = 0;
                return;
            }
        }

        this.clockVariance = clockVariance;
    }

    /**
     * Gets the a random crash time.
     *
     * @return the a random crash time. It will be -1 if the process will not
     *	crash at all randomly!
     */
    protected long getARandomCrashTime() {
        /* Check if the process will crash or not */
        if (getRandomPercentage() < getInteger("process.prob.crash")) {
            /* Calculate the random crash time! */
            final long crashTime =
                random.nextLong(simulatorVisualization.getUntilTime()+1) %
                simulatorVisualization.getUntilTime();
            return crashTime;
        }

        /* No crash */
        return -1;
    }

    /**
     * Increases the process' lamport time.
     */
    public synchronized void increaseLamportTime() {
        setLamportTime(getLamportTime()+1);
    }

    /**
     * Updates the process' lamport time.
     *
     * @param time the lamport time to use as its update reference.
     */
    public synchronized void updateLamportTime(long time) {
        final long lamportTime = getLamportTime() + 1;

        if (time > lamportTime)
            setLamportTime(time);
        else
            setLamportTime(lamportTime);
    }

    /**
     * Gets the lamport time.
     *
     * @return the lamport time.
     */
    public synchronized long getLamportTime() {
        return lamportTime;
    }

    /**
     * Sets the lamport time.
     *
     * @param lamportTime the new lamport time
     */
    public synchronized void setLamportTime(long lamportTime) {
        this.lamportTime = lamportTime;
        lamportTimeHistory.add(new VSLamportTime(globalTime, lamportTime));
    }

    /**
     * Gets the lamport time history as an array.
     *
     * @return the lamport time history array
     */
    public synchronized VSTime[] getLamportTimeArray() {
        final int size = lamportTimeHistory.size();
        final VSTime[] arr = new VSLamportTime[size];

        for (int i = 0; i < size; ++i)
            arr[i] = (VSTime) lamportTimeHistory.get(i);

        return arr;
    }

    /**
     * Increases the vector and the lamport time by 1 each if
     * sim.update.vectortime.all/sim.update.lamporttime.all are set
     * to true.
     */
    public void increaseVectorAndLamportTimeIfAll() {
        if (prefs.getBoolean("sim.update.lamporttime.all"))
            increaseLamportTime();

        if (prefs.getBoolean("sim.update.vectortime.all"))
            increaseVectorTime();
    }

    /**
     * Increases the vector time by 1.
     */
    public synchronized void increaseVectorTime() {
        vectorTime.set(processNum,
                       Long.valueOf(vectorTime.get(processNum).longValue()+1));
        vectorTime.setGlobalTime(globalTime);
        vectorTimeHistory.add(vectorTime.getCopy());
    }

    /**
     * Updates the vector time.
     *
     * @param vectorTimeUpdate the vector time of the other process to use for
     *	the update
     */
    public synchronized void updateVectorTime(VSVectorTime vectorTimeUpdate) {
        final int size = vectorTime.size();

        for (int i = 0; i < size; ++i) {
            if (i == processNum)
                vectorTime.set(i, Long.valueOf(vectorTime.get(i).longValue()+1));
            else if (vectorTimeUpdate.get(i) > vectorTime.get(i))
                vectorTime.set(i, vectorTimeUpdate.get(i));
        }

        vectorTime.setGlobalTime(globalTime);
        vectorTimeHistory.add(vectorTime.getCopy());
    }

    /**
     * Gets the vector time.
     *
     * @return the vector time
     */
    public synchronized VSVectorTime getVectorTime() {
        return vectorTime;
    }

    /**
     * Gets the vector time history as an array.
     *
     * @return the vector time history array
     */
    public synchronized VSTime[] getVectorTimeArray() {
        final int size = vectorTimeHistory.size();
        final VSTime[] arr = new VSTime[size];

        for (int i = 0; i < size; ++i)
            arr[i] = (VSTime) vectorTimeHistory.get(i);

        return arr;
    }

    /**
     * Gets the crash history array.
     *
     * @return the crash history array
     */
    public synchronized Long[] getCrashHistoryArray() {
        final int size = crashHistory.size();
        final Long[] arr = new Long[size];

        for (int i = 0; i < size; ++i)
            arr[i] = crashHistory.get(i);

        return arr;
    }

    /**
     * Logg a message to the loging area.
     *
     * @param message the message to log
     */
    public void log(String message) {
        loging.log(toString() + "; " + message, globalTime);
    }

    /* (non-Javadoc)
     * @see prefs.VSPrefs#fillWithDefaults()
     */
    public void fillWithDefaults() {
        prefs.copyIntegers(this, DEFAULT_INTEGER_VALUE_KEYS);
        prefs.copyLongs(this, DEFAULT_LONG_VALUE_KEYS);
        prefs.copyFloats(this, DEFAULT_FLOAT_VALUE_KEYS);
        prefs.copyColors(this, DEFAULT_COLOR_VALUE_KEYS);
        prefs.copyStrings(this, DEFAULT_STRING_VALUE_KEYS);
    }

    /* (non-Javadoc)
     * @see prefs.VSPrefs#toString()
     */
    public synchronized String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append(prefs.getString("lang.process.id"));
        buffer.append(": ");
        buffer.append(getProcessID());
        buffer.append("; ");
        buffer.append(prefs.getString("lang.process.time.local"));
        buffer.append(": ");
        buffer.append(VSTools.getTimeString(getTime()));
        buffer.append("; ");
        buffer.append(prefs.getString("lang.time.lamport"));
        buffer.append(": ");
        buffer.append(lamportTime);
        buffer.append("; ");
        buffer.append(prefs.getString("lang.time.vector"));
        buffer.append(": ");
        buffer.append(vectorTime);
        return buffer.toString();
    }

    /**
     * The extended string representation of the process object.
     *
     * @return the extended string representation
     */
    public synchronized String toStringFull() {
        StringBuffer buffer = new StringBuffer();
        buffer.append(toString());
        buffer.append("; paused: ");
        buffer.append(isPaused);
        buffer.append("; crashed: ");
        buffer.append(isCrashed);
        buffer.append("; crashTask: ");
        buffer.append(randomCrashTask);
        return buffer.toString();
    }

    /**
     * Equals. Checks, if both processes have the same process num.
     *
     * @param process the process to compare to
     *
     * @return true, if both processes are the same (same processNum).
     */
    public boolean equals(VSAbstractProcess process) {
        return process.getProcessNum() == processNum;
    }

    /**
     * Gets the simulator's default prefs.
     *
     * @return the default prefs
     */
    public VSPrefs getPrefs() {
        return prefs;
    }

    /* (non-Javadoc)
     * @see serialize.VSSerializable#serialize(serialize.VSSerialize,
     *	java.io.ObjectOutputStream)
     */
    public synchronized void serialize(VSSerialize serialize,
                                       ObjectOutputStream objectOutputStream)
    throws IOException {
        super.serialize(serialize, objectOutputStream);

        if (VSSerialize.DEBUG)
            System.out.println("Serializing: VSAbstractProcess (num: "
                               + processNum
                               + "; id: " + processID + ")");

        /** For later backwards compatibility, to add more stuff */
        objectOutputStream.writeObject(Boolean.valueOf(false));

        objectOutputStream.writeObject(Integer.valueOf(processID));
        objectOutputStream.writeObject(Integer.valueOf(protocolsToReset.size()));
        for (VSAbstractProtocol protocol : protocolsToReset) {
            objectOutputStream.writeObject(protocol.getClassname());
            protocol.serialize(serialize, objectOutputStream);
        }

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
        super.deserialize(serialize, objectInputStream);

        /* Bugfix, being compatible with old versions */
        super.deleteLong("process.localTime");

        updateFromPrefs_();

        if (VSSerialize.DEBUG)
            System.out.println("Deserializing: VSAbstractProcess");

        /** For later backwards compatibility, to add more stuff */
        objectInputStream.readObject();

        this.processID = ((Integer)
                          objectInputStream.readObject()).intValue();
        int numProtocols = ((Integer)
                            objectInputStream.readObject()).intValue();

        for (int i = 0; i < numProtocols; ++i) {
            String protocolClassname = (String) objectInputStream.readObject();
            VSAbstractProtocol protocol = getProtocolObject_(protocolClassname);
            protocol.deserialize(serialize, objectInputStream);
        }

        localTime = 0;
        setLong("process.localtime", localTime);

        /** For later backwards compatibility, to add more stuff */
        objectInputStream.readObject();

        serialize.setObject(processNum, "process", this);
    }

    /**
     * Sets the current color.
     *
     * @param newColor the new current color
     */
    protected void setCurrentColor(Color newColor) {
        if (isHighlighted)
            tmpColor = newColor;
        else
            currentColor = newColor;
    }

    /* (non-Javadoc)
     * @see core.VSInternalMessage#updateFromPrefs()
     */
    protected abstract void updateFromPrefs_();

    /* (non-Javadoc)
     * @see core.VSInternalMessage#createRandomCrashTask()
     */
    protected abstract void createRandomCrashTask_();

    /* (non-Javadoc)
     * @see core.VSInternalMessage#getProtocolObjekt(java.util.String)
     */
    protected abstract VSAbstractProtocol getProtocolObject_(
        String protocolClassname);
}
