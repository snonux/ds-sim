package core;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.PriorityQueue;

import prefs.VSPrefs;
import serialize.VSSerializable;
import serialize.VSSerialize;
import simulator.VSSimulatorVisualization;
import utils.VSPriorityQueue;

/**
 * The class VSTaskManager, it is responsible that all tasks will get
 * fullfilled in the correct order. Please also read the javadoc of the VSTask
 * class. It describes the difference between local and global timed tasks.
 *
 * @author Paul C. Buetow
 */
public class VSTaskManager implements VSSerializable {
    /** The simulator canvas. */
    private VSSimulatorVisualization simulatorVisualization;

    /** The global tasks. */
    private PriorityQueue<VSTask> globalTasks;

    /** The fullfilled programmed tasks. */
    private LinkedList<VSTask> fullfilledProgrammedTasks;

    /** The Constant PROGRAMMED. */
    public final static boolean PROGRAMMED = true;

    /** The Constant ONLY_ONCE. */
    public final static boolean ONLY_ONCE = false;

    /** The simulator's default prefs. */
    private VSPrefs prefs;

    /**
     * Instantiates a new task manager object.
     *
     * @param prefs the simulator's default prefs
     * @param simulatorVisualization the simulator canvas
     */
    public VSTaskManager(VSPrefs prefs,
                         VSSimulatorVisualization simulatorVisualization) {
        init(prefs, simulatorVisualization);
    }

    /**
     * Inits the task manager.
     *
     * @param prefs the simulator's default prefs
     * @param simulatorVisualization the simulator canvas
     */
    private void init(VSPrefs prefs,
                      VSSimulatorVisualization simulatorVisualization) {
        this.prefs = prefs;
        this.simulatorVisualization = simulatorVisualization;

        /* May be not null if called from deserialization */
        if (globalTasks == null)
            this.globalTasks = new PriorityQueue<VSTask>();

        this.fullfilledProgrammedTasks = new LinkedList<VSTask>();
    }

    /**
     * Run tasks. This method gets called by the simulator canvas repeatedly.
     * Almost all simulator actions take place in this method.
     *
     * @param step the step
     * @param offset the offset
     * @param lastGlobalTime the last global time
     */
    public synchronized void runTasks(long step, long offset,
                                      long lastGlobalTime) {
        VSTask task = null;
        long localTime;
        long offsetTime;
        long taskTime;
        long globalTime;
        final long globalOffsetTime = lastGlobalTime + step;
        boolean redo;
        ArrayList<VSInternalProcess> processes =
            simulatorVisualization.getProcesses();

        do {
            redo = false;

            /* Run tasks which have for its schedule the global time */
            while (globalTasks.size() != 0) {
                task = globalTasks.peek();
                VSInternalProcess process = task.getProcess();
                localTime = process.getTime();
                offsetTime = localTime + step;
                taskTime = task.getTaskTime();
                globalTime = process.getGlobalTime();

                if (globalOffsetTime < taskTime)
                    break;

                globalTasks.poll();
                redo = true;

                if (process.isCrashed() && !task.hasProcessRecoverEvent()) {
                    if (task.isProgrammed())
                        fullfilledProgrammedTasks.add(task);
                    continue;
                }

                if (globalOffsetTime == taskTime) {
                    process.setGlobalTime(globalOffsetTime);
                    process.setLocalTime(offsetTime);
                    process.timeModified(false);
                    task.run();
                    process.setGlobalTime(globalTime);
                    if (process.isCrashed())
                        process.addClockOffset(step);
                    if (process.timeModified())
                        process.addClockOffset(process.getTime()-offsetTime);
                    process.setLocalTime(localTime);

                } else { /* if (globalOffsetTime > taskTime) */
                    final long diff = globalOffsetTime - taskTime;
                    if (globalOffsetTime - diff < lastGlobalTime)
                        process.setGlobalTime(lastGlobalTime);
                    else
                        process.setGlobalTime(globalOffsetTime - diff);
                    process.setLocalTime(offsetTime - diff);
                    process.timeModified(false);
                    task.run();
                    process.setGlobalTime(globalTime);
                    if (process.isCrashed())
                        process.addClockOffset(step);
                    if (process.timeModified())
                        process.addClockOffset(process.getTime()-
                                               (offsetTime-diff));
                    process.setLocalTime(localTime);
                }

                if (task.isProgrammed())
                    fullfilledProgrammedTasks.add(task);
            }

            synchronized (processes) {
                for (VSInternalProcess process : processes) {
                    PriorityQueue<VSTask> tasks = process.getTasks();

                    /* Run tasks which have for its schedule the local
                       process times */
                    while (tasks.size() != 0) {
                        task = tasks.peek();
                        process = task.getProcess();
                        localTime = process.getTime();
                        offsetTime = localTime + step;
                        taskTime = task.getTaskTime();
                        globalTime = process.getGlobalTime();

                        if (offsetTime < taskTime)
                            break;

                        tasks.poll();
                        redo = true;

                        if (process.isCrashed() &&
                                !task.hasProcessRecoverEvent()) {
                            if (task.isProgrammed())
                                fullfilledProgrammedTasks.add(task);
                            continue;
                        }

                        if (offsetTime == taskTime) {
                            process.setGlobalTime(globalOffsetTime);
                            process.setLocalTime(offsetTime);
                            process.timeModified(false);
                            task.run();
                            process.setGlobalTime(globalTime);
                            if (process.timeModified())
                                process.addClockOffset(process.getTime()-
                                                       offsetTime);
                            process.setLocalTime(localTime);

                        } else { /* if (offsetTime > taskTime) */
                            final long diff = offsetTime - taskTime;
                            if (globalOffsetTime - diff < lastGlobalTime)
                                process.setGlobalTime(lastGlobalTime);
                            else
                                process.setGlobalTime(globalOffsetTime-
                                                      diff);
                            process.setLocalTime(offsetTime - diff);
                            process.timeModified(false);
                            task.run();
                            process.setGlobalTime(globalTime);
                            if (process.timeModified())
                                process.addClockOffset(process.getTime()-
                                                       (offsetTime-diff));
                            process.setLocalTime(localTime);
                        }

                        if (task.isProgrammed())
                            fullfilledProgrammedTasks.add(task);
                    }
                }
            }

        } while (redo);
    }

    /**
     * Resets the task manager.
     */
    public synchronized void reset() {
        ArrayList<VSInternalProcess> processes =
            simulatorVisualization.getProcesses();
        PriorityQueue<VSTask> tmp = null;

        synchronized (processes) {
            for (VSInternalProcess process : processes) {
                tmp = process.getTasks();
                process.setTasks(new VSPriorityQueue<VSTask>());

                for (VSTask task : tmp) {
                    if (task.isProgrammed())
                        insert(task);
                }
            }
        }

        tmp = globalTasks;
        globalTasks = new PriorityQueue<VSTask>();

        while (fullfilledProgrammedTasks.size() != 0)
            insert(fullfilledProgrammedTasks.removeFirst());

        while (tmp.size() != 0) {
            VSTask task = tmp.poll();
            if (task.isProgrammed())
                insert(task);
        }
    }

    /**
     * Inserts a task. Only for internal usage. Use the add methods instead.
     * This method checks if the task to insert is a global or a local timed
     * task. And it also checks if the task's time is over already.
     *
     * @param task the task to insert
     */
    private synchronized void insert(VSTask task) {
        if (task.timeOver()) {
            if (task.isProgrammed())
                fullfilledProgrammedTasks.addLast(task);

        } else if (task.isGlobalTimed()) {
            globalTasks.add(task);

        } else {
            task.getProcess().getTasks().add(task);
        }
    }

    /**
     * Adds a task.
     *
     * @param task the task to add
     */
    public synchronized void addTask(VSTask task) {
        addTask(task, VSTaskManager.ONLY_ONCE);
    }

    /**
     * Adds a task.
     *
     * @param task the task to add
     * @param isProgrammed true, if the task is programmed
     */
    public synchronized void addTask(VSTask task, boolean isProgrammed) {
        task.isProgrammed(isProgrammed);
        insert(task);
    }

    /**
     * Removes a task.
     *
     * @param task the task to remove
     *
     * @return true, if the task has been removed with success
     */
    public synchronized boolean removeTask(VSTask task) {
        if (fullfilledProgrammedTasks.remove(task)) {
            return true;

        } else if (task.isGlobalTimed() && globalTasks.remove(task)) {
            return true;

        } else if (!task.isGlobalTimed()) {
            if (task.getProcess().getTasks().remove(task))
                return true;
        }

        return false;
    }

    /**
     * Removes several tasks.
     *
     * @param tasks the tasks to remove
     */
    public synchronized void removeAllTasks(ArrayList<VSTask> tasks) {
        for (VSTask task : tasks)
            removeTask(task);
    }

    /**
     * Removes the tasks of the specified process.
     *
     * @param process the process to remove the tasks of
     */
    public synchronized void removeTasksOf(VSInternalProcess process) {
        ArrayList<VSTask> removeThose = new ArrayList<VSTask>();

        for (VSTask task : fullfilledProgrammedTasks)
            if (task.isProcess(process))
                removeThose.add(task);

        for (VSTask task : removeThose)
            fullfilledProgrammedTasks.remove(task);

        removeThose.clear();

        for (VSTask task : globalTasks)
            if (task.isProcess(process))
                removeThose.add(task);

        for (VSTask task : removeThose)
            globalTasks.remove(task);

        process.getTasks().clear();
    }

    /**
     * Gets the local timed tasks.
     *
     * @return the local timed tasks
     */
    public synchronized ArrayList<VSTask> getLocalTasks() {
        ArrayList<VSTask> localTasks = new ArrayList<VSTask>();
        ArrayList<VSInternalProcess> processes =
            simulatorVisualization.getProcesses();

        for (VSTask task : fullfilledProgrammedTasks)
            if (!task.isGlobalTimed())
                localTasks.add(task);

        synchronized (processes) {
            for (VSInternalProcess process : processes) {
                VSPriorityQueue<VSTask> tasks = process.getTasks();
                for (VSTask task : tasks)
                    localTasks.add(task);
            }
        }

        return localTasks;
    }

    /**
     * Gets the global timed tasks.
     *
     * @return the global timed tasks
     */
    public synchronized ArrayList<VSTask> getGlobalTasks() {
        ArrayList<VSTask> globalTasks = new ArrayList<VSTask>();

        for (VSTask task : fullfilledProgrammedTasks)
            if (task.isGlobalTimed())
                globalTasks.add(task);

        for (VSTask task : this.globalTasks)
            if (task.isProgrammed())
                globalTasks.add(task);

        return globalTasks;
    }

    /**
     * Gets the local timed tasks of a specific process.
     *
     * @param process the process to get the local timed tasks of
     *
     * @return the local tasks of the specified process
     */
    public synchronized ArrayList<VSTask> getProcessLocalTasks(
        VSInternalProcess process) {
        ArrayList<VSTask> processTasks = new ArrayList<VSTask>();
        VSPriorityQueue<VSTask> tasks = process.getTasks();

        for (VSTask task : fullfilledProgrammedTasks)
            if (!task.isGlobalTimed() && task.isProcess(process) &&
                    task.isProgrammed())
                processTasks.add(task);

        for (VSTask task : tasks)
            if (task.isProgrammed())
                processTasks.add(task);

        return processTasks;
    }

    /**
     * Gets the global timed tasks of a specific process.
     *
     * @param process the process to get the local timed tasks of
     *
     * @return the global timed tasks of the specified process
     */
    public synchronized ArrayList<VSTask> getProcessGlobalTasks(
        VSInternalProcess process) {
        ArrayList<VSTask> processTasks = new ArrayList<VSTask>();

        for (VSTask task : fullfilledProgrammedTasks)
            if (task.isGlobalTimed() && task.isProcess(process) &&
                    task.isProgrammed())
                processTasks.add(task);

        for (VSTask task : globalTasks)
            if (task.isProcess(process) && task.isProgrammed())
                processTasks.add(task);

        return processTasks;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public synchronized String toString() {
        StringBuffer buffer = new StringBuffer();

        buffer.append(prefs.getString("lang.task.manager"));
        buffer.append(" (");
        buffer.append(prefs.getString("lang.tasks.fullfilled"));
        buffer.append(": ");

        for (VSTask task : fullfilledProgrammedTasks) {
            buffer.append(task);
            buffer.append("; ");
        }

        buffer.append(prefs.getString("lang.tasks.global"));
        buffer.append(": ");

        for (VSTask task : globalTasks) {
            buffer.append(task);
            buffer.append("; ");
        }

        buffer.append(prefs.getString("lang.tasks.local"));
        buffer.append(": ");

        ArrayList<VSInternalProcess> processes =
            simulatorVisualization.getProcesses();
        synchronized (processes) {
            for (VSInternalProcess process : processes) {
                VSPriorityQueue<VSTask> tasks = process.getTasks();
                for (VSTask task : tasks) {
                    buffer.append(task);
                    buffer.append("; ");
                }
            }
        }

        String descr = buffer.toString();

        if (descr.endsWith("; "))
            return descr.substring(0, descr.length()-2) + ")";

        return descr + ")";
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

        ArrayList<VSTask> serializeThoseTasks = new ArrayList<VSTask>();

        for (VSTask task : fullfilledProgrammedTasks) {
            if (!task.hasNotSerializableEvent())
                serializeThoseTasks.add(task);
        }

        for (VSTask task : globalTasks) {
            if (!task.hasNotSerializableEvent())
                serializeThoseTasks.add(task);
        }

        ArrayList<VSInternalProcess> processes =
            simulatorVisualization.getProcesses();

        synchronized (processes) {
            for (VSInternalProcess process : processes) {
                VSPriorityQueue<VSTask> localTasks = process.getTasks();
                for (VSTask task : localTasks) {
                    if (!task.hasNotSerializableEvent())
                        serializeThoseTasks.add(task);
                }
            }
        }

        objectOutputStream.writeObject(
            Integer.valueOf(serializeThoseTasks.size()));
        for (VSTask task : serializeThoseTasks)
            task.serialize(serialize, objectOutputStream);

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
            System.out.println("Deserializing: VSTaskManager");

        /** For later backwards compatibility, to add more stuff */
        objectInputStream.readObject();

        globalTasks.clear();

        ArrayList<VSInternalProcess> processes =
            simulatorVisualization.getProcesses();
        synchronized (processes) {
            for (VSInternalProcess process : processes)
                process.getTasks().clear();
        }

        int numTasks = ((Integer) objectInputStream.readObject()).intValue();

        if (VSSerialize.DEBUG)
            System.out.println("Num of tasks: " + numTasks);

        for (int i = 0; i < numTasks; ++i) {
            VSTask task = new VSTask(serialize, objectInputStream);
            addTask(task, task.isProgrammed());
        }

        /** For later backwards compatibility, to add more stuff */
        objectInputStream.readObject();
    }
}
