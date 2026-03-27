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
    
    /**
     * Protocols do not increase timestamps when executed.
     * Only regular events and messages increase timestamps.
     * 
     * @return always false for protocol events
     */
    @Override
    public boolean shouldIncreaseTimestamps() {
        return false;
    }

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
     * Sends a message from this protocol to other processes.
     * This method automatically:
     * <ul>
     *   <li>Increases the process's Lamport and vector timestamps</li>
     *   <li>Wraps the message in a {@link VSMessageStub}</li>
     *   <li>Marks the message as server or client based on current context</li>
     *   <li>Delegates to the process for actual transmission</li>
     * </ul>
     *
     * @param message the message to send
     * @see VSMessageStub
     * @see VSInternalProcess#sendMessage(VSMessage)
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
     * Handles incoming message reception for this protocol.
     * This method:
     * <ul>
     *   <li>Filters out messages for other protocols</li>
     *   <li>Routes messages to the active role(s) without double-delivering
     *       to dual-role peers</li>
     * </ul>
     *
     * @param message the received message
     * @see #onServerRecv(VSMessage)
     * @see #onClientRecv(VSMessage)
     */
    public final void onMessageRecvStart(VSMessage message) {
        if (isIncorrectProtocol(message))
            return;

        if (isServer && isClient) {
            if (message.isServerMessage()) {
                currentContextIsServer(false);
                if (!isClientInitialized)
                    onInit();
                onClientRecv(message);
            } else {
                currentContextIsServer(true);
                if (!isServerInitialized)
                    onInit();
                onServerRecv(message);
            }
            return;
        }

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
     * Checks if a message is relevant for this protocol instance.
     * A message is relevant if:
     * <ul>
     *   <li>It belongs to this protocol (same class name)</li>
     *   <li>It's a server message and this is a client</li>
     *   <li>It's a client message and this is a server</li>
     * </ul>
     * 
     * <p>This ensures proper client-server communication where:
     * clients only receive server messages and servers only receive client messages.</p>
     *
     * @param message the message to check
     * @return true if the message should be processed by this protocol instance
     */
    public final boolean isRelevantMessage(VSMessage message) {
        if (isIncorrectProtocol(message))
            return false;

        return isRelevantMessageForContext(message);
    }

    /**
     * Checks whether a message is relevant for this protocol instance.
     * Subclasses can relax or specialize the default server/client routing
     * rules while keeping the protocol-name filter intact.
     *
     * @param message the message to check
     * @return true if the message should be processed by this protocol instance
     */
    protected boolean isRelevantMessageForContext(VSMessage message) {
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
     * Checks whether the protocol currently runs in server context.
     *
     * @return true if the current context is server, otherwise false
     */
    public final boolean currentContextIsServer() {
        return currentContextIsServer;
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
     * Schedules this protocol to execute at a specific local time.
     * When the scheduled time arrives, either {@link #onServerSchedule()} or
     * {@link #onClientSchedule()} will be called based on the current context.
     * 
     * <p>The schedule is tracked separately for server and client contexts,
     * allowing independent scheduling for each role.</p>
     *
     * @param time the process's local time when the schedule should execute
     * @see #onServerSchedule()
     * @see #onClientSchedule()
     * @see VSProtocolScheduleEvent
     */
    public final void scheduleAt(long time) {
        VSInternalProcess internalProcess = (VSInternalProcess) process;
        VSAbstractEvent scheduleEvent =
            new VSProtocolScheduleEvent(this, currentContextIsServer);
        scheduleEvent.init(internalProcess);
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
     * Called once when the protocol's client functionality is first initialized.
     * Subclasses should implement this to set up client-specific state,
     * initialize data structures, and prepare for client operations.
     * 
     * <p>This method is called before the first {@link #onClientStart()}
     * or {@link #onClientRecv(VSMessage)} invocation.</p>
     */
    abstract public void onClientInit();

    /**
     * Called when the client protocol should begin its main operation.
     * Only used if the protocol was created with {@code hasOnServerStart = false}.
     * 
     * <p>Default implementation does nothing. Override this method to implement
     * client-initiated protocol behavior.</p>
     */
    public void onClientStart() { };

    /**
     * Called when the protocol's client state should be reset.
     * Subclasses should implement this to clear client-specific state
     * and prepare for potential re-initialization.
     * 
     * <p>All client schedules are automatically cleared before this method is called.</p>
     */
    abstract public void onClientReset();

    /**
     * Called when a scheduled client event occurs.
     * Subclasses should implement this to handle periodic client operations
     * or delayed client actions.
     * 
     * <p>To schedule this method, use {@link #scheduleAt(long)} while in client context.</p>
     * 
     * @see #scheduleAt(long)
     */
    abstract public void onClientSchedule();

    /**
     * Called when the client receives a message from a server.
     * Subclasses should implement this to handle incoming server messages
     * and update client state accordingly.
     * 
     * <p>The message is guaranteed to be a server message for this protocol.</p>
     *
     * @param message the server message received
     * @see #sendMessage(VSMessage)
     */
    abstract public void onClientRecv(VSMessage message);

    /**
     * Called once when the protocol's server functionality is first initialized.
     * Subclasses should implement this to set up server-specific state,
     * initialize data structures, and prepare for server operations.
     * 
     * <p>This method is called before the first {@link #onServerStart()}
     * or {@link #onServerRecv(VSMessage)} invocation.</p>
     */
    abstract public void onServerInit();

    /**
     * Called when the server protocol should begin its main operation.
     * Only used if the protocol was created with {@code hasOnServerStart = true}.
     * 
     * <p>Default implementation does nothing. Override this method to implement
     * server-initiated protocol behavior.</p>
     */
    public void onServerStart() { };

    /**
     * Called when the protocol's server state should be reset.
     * Subclasses should implement this to clear server-specific state
     * and prepare for potential re-initialization.
     * 
     * <p>All server schedules are automatically cleared before this method is called.</p>
     */
    abstract public void onServerReset();

    /**
     * Called when the server receives a message from a client.
     * Subclasses should implement this to handle incoming client messages
     * and update server state accordingly.
     * 
     * <p>The message is guaranteed to be a client message for this protocol.</p>
     *
     * @param message the client message received
     * @see #sendMessage(VSMessage)
     */
    abstract public void onServerRecv(VSMessage message);

    /**
     * Called when a scheduled server event occurs.
     * Subclasses should implement this to handle periodic server operations
     * or delayed server actions.
     * 
     * <p>To schedule this method, use {@link #scheduleAt(long)} while in server context.</p>
     * 
     * @see #scheduleAt(long)
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
