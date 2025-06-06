package protocols;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

import core.VSInternalProcess;
import core.VSMessage;
import core.VSMessageStub;
import core.VSTask;
import events.VSAbstractEvent;
import events.internal.VSProtocolScheduleEvent;
import serialize.VSSerialize;
import simulator.VSSimulatorVisualization;

/**
 * The class VSAbstractProtocol, this class defined the basic framework of a
 * protocol.
 *
 * @author Paul C. Buetow
 */
abstract public class VSAbstractProtocol extends VSAbstractEvent {
    /** The protocol has an onServerStart method */
    protected static final boolean HAS_ON_SERVER_START = true;

    /** The protocol has an onClientStart method */
    protected static final boolean HAS_ON_CLIENT_START = false;

    /** True, if onServerStart is used, false if onClientStart is used */
    private boolean hasOnServerStart;

    /** The protocol object is a server. */
    private boolean isServer;

    /** The protocol object is a client. */
    private boolean isClient;

    /** The protocol object server is initialized. */
    private boolean isServerInitialized;

    /** The protocol object client is initialized. */
    private boolean isClientInitialized;

    /** The current protocol object's context is a server. */
    private boolean currentContextIsServer;

    /** The protocol's server schedules */
    private ArrayList<VSTask> serverSchedules = new ArrayList<VSTask>();

    /** The protocol's client schedules */
    private ArrayList<VSTask> clientSchedules = new ArrayList<VSTask>();

    /**
     * A simple constructor.
     *
     * @param hasOnServerStart true, if the protocol uses an onServerStart
     *	method. false, if the protocol uses an onClientStart method instead.
     */
    public VSAbstractProtocol(boolean hasOnServerStart) {
        this.hasOnServerStart = hasOnServerStart;
    }

    /**
     * Sends a message.
     *
     * @param message the message to send
     */
    public void sendMessage(VSMessage message) {
        if (process == null)
            return;

        process.increaseLamportTime();
        process.increaseVectorTime();

        VSMessageStub stub = new VSMessageStub(message);
        VSInternalProcess internalProcess = (VSInternalProcess) process;

        if (currentContextIsServer)
            stub.init(internalProcess, getClassname(),
                      VSMessage.IS_SERVER_MESSAGE);
        else
            stub.init(internalProcess, getClassname(),
                      VSMessage.IS_CLIENT_MESSAGE);

        internalProcess.sendMessage(message);
    }

    /**
     * Checks if it's the incorrect protocol
     *
     * @param message the message to check against
     *
     * @return true, if is incorrect protocol
     */
    private final boolean isIncorrectProtocol(VSMessage message) {
        return !message.getProtocolClassname().equals(getClassname());
    }

    /* (non-Javadoc)
     * @see events.VSAbstractEvent#onStart()
     */
    public final void onStart() {
        if (hasOnServerStart) {
            if (isServer) {
                currentContextIsServer(true);
                if (!isServerInitialized)
                    onInit();
                onServerStart();
            }
        } else {
            if (isClient) {
                currentContextIsServer(false);
                if (!isClientInitialized)
                    onInit();
                onClientStart();
            }
        }
    }

    /* (non-Javadoc)
     * @see events.VSAbstractEvent#onInit()
     */
    public final void onInit() {
        if (isClient) {
            currentContextIsServer(false);
            onClientInit();
            isClientInitialized = true;
        }

        if (isServer) {
            currentContextIsServer(true);
            onServerInit();
            isServerInitialized = true;
        }
    }

    /**
     * Runs a client schedule
     */
    public final void onClientScheduleStart() {
        if (isClient) {
            currentContextIsServer(false);
            onClientSchedule();
        }
    }

    /**
     * Runs a server schedule
     */
    public final void onServerScheduleStart() {
        if (isServer) {
            currentContextIsServer(true);
            onServerSchedule();
        }
    }

    /**
     * On message recv.
     *
     * @param message the message
     */
    public final void onMessageRecvStart(VSMessage message) {
        if (isIncorrectProtocol(message))
            return;

        if (isServer) {
            currentContextIsServer(true);
            if (!isServerInitialized)
                onInit();
            onServerRecv(message);
        }

        if (isClient) {
            currentContextIsServer(false);
            if (!isClientInitialized)
                onInit();
            onClientRecv(message);
        }
    }

    /**
     * Check's if its a relevant message.
     *
     * @param message the message to check
     *
     * @return true, if it's a relevant meessage. false if the protocol
     *	is wrong or if the server recv a server message/the client recv a
     * 	client message. Clients should only recv server messages and servers
     *	should only recv client messages.
     */
    public final boolean isRelevantMessage(VSMessage message) {
        if (isIncorrectProtocol(message))
            return false;

        if (message.isServerMessage()) {
            if (!isClient)
                return false;
        } else {
            if (!isServer)
                return false;
        }

        return true;
    }

    /**
     * Sets if the current context is server.
     *
     * @param currentContextIsServer the context.
     */
    public final void currentContextIsServer(boolean currentContextIsServer) {
        this.currentContextIsServer = currentContextIsServer;
    }

    /**
     * Checks how the protocol will start
     *
     * @return true, if this protocol uses onServerStart instead of
     *	onClientStart
     */
    public final boolean hasOnServerStart() {
        return hasOnServerStart;
    }

    /**
     * Sets if is server.
     *
     * @param isServer the is server
     */
    public final void isServer(boolean isServer) {
        this.isServer = isServer;
    }

    /**
     * Checks if is server.
     *
     * @return true, if the protocol has activated the server part
     */
    public final boolean isServer() {
        return isServer;
    }

    /**
     * Sets if is client.
     *
     * @param isClient the is client
     */
    public final void isClient(boolean isClient) {
        this.isClient = isClient;
    }

    /**
     * Checks if is client.
     *
     * @return true, if the protocol has activated the client part
     */
    public final boolean isClient() {
        return isClient;
    }

    /**
     * Resets the protocol.
     */
    public void reset() {
        currentContextIsServer(true);
        isServer = false;
        onServerReset();
        serverSchedules.clear();

        currentContextIsServer(false);
        isClient = false;
        onClientReset();
        clientSchedules.clear();
    }

    /**
     * Reschedules the protocol for a new time and runs onClientSchedule or
     *	onServerSchedule
     *
     * @param time The process' local time to run the schedule at.
     */
    public final void scheduleAt(long time) {
        VSInternalProcess internalProcess = (VSInternalProcess) process;
        VSAbstractEvent scheduleEvent =
            new VSProtocolScheduleEvent(this, currentContextIsServer);
        VSTask scheduleTask =
            new VSTask(time, internalProcess, scheduleEvent, VSTask.LOCAL);

        if (currentContextIsServer)
            serverSchedules.add(scheduleTask);
        else
            clientSchedules.add(scheduleTask);

        VSSimulatorVisualization canvas = internalProcess.getSimulatorCanvas();
        canvas.getTaskManager().addTask(scheduleTask);
    }

    /**
     * Removes all schedules of the protocol (server or client)
     */
    public final void removeSchedules() {
        VSInternalProcess internalProcess = (VSInternalProcess) process;

        if (currentContextIsServer) {
            internalProcess.getSimulatorCanvas().
            getTaskManager().removeAllTasks(serverSchedules);
            serverSchedules.clear();

        } else {
            internalProcess.getSimulatorCanvas().
            getTaskManager().removeAllTasks(clientSchedules);
            clientSchedules.clear();
        }
    }

    /**
     * On client init.
     */
    abstract public void onClientInit();

    /**
     * On client start.
     */
    public void onClientStart() { };

    /**
     * On client reset.
     */
    abstract public void onClientReset();

    /**
     * On client schedule.
     */
    abstract public void onClientSchedule();

    /**
     * On client recv.
     *
     * @param message the message
     */
    abstract public void onClientRecv(VSMessage message);

    /**
     * On server init.
     */
    abstract public void onServerInit();

    /**
     * On server start.
     */
    public void onServerStart() { };

    /**
     * On server reset.
     */
    abstract public void onServerReset();

    /**
     * On server recv.
     *
     * @param message the message
     */
    abstract public void onServerRecv(VSMessage message);

    /**
     * On server schedule.
     */
    abstract public void onServerSchedule();

    /**
     * Gets the num processes.
     *
     * @return the num processes
     */
    public final int getNumProcesses() {
        if (process == null)
            return 0;

        VSInternalProcess internalProcess = (VSInternalProcess) process;
        return internalProcess.getSimulatorCanvas().getNumProcesses();
    }

    /* (non-Javadoc)
     * @see events.VSAbstractEvent#createShortname()()
     */
    protected String createShortname(String savedShortname) {
        return savedShortname;
    }

    /* (non-Javadoc)
     * @see prefs.VSPrefs#toString()
     */
    public String toString() {
        if (process == null)
            return "";

        StringBuffer buffer = new StringBuffer();

        buffer.append(prefs.getString("lang.protocol"));
        buffer.append(": ");
        buffer.append(getShortname());
        buffer.append(" ");

        if (currentContextIsServer)
            buffer.append(prefs.getString("lang.server"));
        else
            buffer.append(prefs.getString("lang.client"));

        return buffer.toString();
    }

    /* (non-Javadoc)
     * @see serialize.VSSerializable#serialize(serialize.VSSerialize,
     *	java.io.ObjectOutputStream)
     */
    public synchronized void serialize(VSSerialize serialize,
                                       ObjectOutputStream objectOutputStream)
    throws IOException {
        super.serialize(serialize, objectOutputStream);

        /** For later backwards compatibility, to add more stuff */
        objectOutputStream.writeObject(Boolean.valueOf(false));

        objectOutputStream.writeObject(Boolean.valueOf(hasOnServerStart));

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

        if (VSSerialize.DEBUG)
            System.out.println("Deserializing: VSAbstractProtocol");

        /** For later backwards compatibility, to add more stuff */
        objectInputStream.readObject();

        this.hasOnServerStart = ((Boolean) objectInputStream.readObject()).booleanValue();
        /** For later backwards compatibility, to add more stuff */
        objectInputStream.readObject();
    }
}
