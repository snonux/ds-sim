package core;

import core.time.VSVectorTime;
import events.VSRegisteredEvents;
import prefs.VSPrefs;

/**
 * An object of this class represents a message which is sent from one process
 * to another process in the simulator.
 *
 * @author Paul C. Buetow
 */
public class VSMessage extends VSPrefs {
    /** The constant IS_SERVER_MESSAGE. */
    public static final boolean IS_SERVER_MESSAGE = true;

    /** The constant IS_CLIENT_MESSAGE. */
    public static final boolean IS_CLIENT_MESSAGE = false;

    /** true, if the message has been sent from a server. false, if the message
     * has been sent from a client.
     */
    private boolean isServerMessage;

    /** Each message belongs to a specific protocol. This variable defined the
     * class name of the protocol being used.
     */
    private String protocolClassname;

    /** The default application preferences. */
    private VSPrefs prefs;

    /** A reference to the process who sent this message. */
    private VSInternalProcess sendingProcess;

    /** The vector time of the sending process after sending. The receiver
     * process will use this vector time in order to update the local vector
     * time.
     */
    private VSVectorTime vectorTime;

    /** The lamport time of the sending process after sending. The receiver
     * process will use this lamport time in order to update the local vector
     * time.
     */
    private long lamportTime;

    /** Each message has its own unique ID. The ID will show up in the loging
     * window of the simulator
     */
    private long messageID;

    /** This counter is used in order to generate unique message ID's. */
    private static long messageCounter;

    /**
     * The constructor of the message. Creates a new message object.
     */
    public VSMessage() {
        this.messageID = ++messageCounter;
    }

    /**
     * Initializes the message.
     *
     * @param process The sending process of this message.
     * @param protocolClassname The classname of the protocol this message.
     * @param isServerMessage Sets if the message has been sent by a server.
     */
    void init(VSInternalProcess process, String protocolClassname,
              boolean isServerMessage) {
        this.sendingProcess = process;
        this.protocolClassname = protocolClassname;
        this.isServerMessage = isServerMessage;
        this.prefs = process.getPrefs();

        lamportTime = sendingProcess.getLamportTime();
        vectorTime = sendingProcess.getVectorTime().getCopy();
    }

    /**
     * Gets the human-readable protocol name associated with this message.
     * The name is retrieved from the registered events based on the protocol classname.
     *
     * @return the localized protocol name, or null if not found
     */
    public String getName() {
        return VSRegisteredEvents.getNameByClassname(getProtocolClassname());
    }

    /**
     * Gets the fully qualified classname of the protocol that created this message.
     * This is used to identify which protocol handler should process the message.
     *
     * @return the protocol's full classname (e.g., "protocols.implementations.VSPingPongProtocol")
     */
    public String getProtocolClassname() {
        return protocolClassname;
    }

    /**
     * Gets the unique identifier for this message.
     * Each message is assigned a sequential ID when created.
     *
     * @return the unique message ID
     */
    public long getMessageID() {
        return messageID;
    }

    /**
     * Gets the process that sent this message.
     * This can be used to identify the sender and access sender information.
     *
     * @return the process that created and sent this message
     */
    public VSAbstractProcess getSendingProcess() {
        return sendingProcess;
    }

    /**
     * Gets the Lamport timestamp of the sending process at the time this message was sent.
     * This is used for logical ordering of events in the distributed system.
     *
     * @return the sender's Lamport time when the message was created
     */
    public long getLamportTime() {
        return lamportTime;
    }

    /**
     * Gets the vector clock of the sending process at the time this message was sent.
     * This is used for determining causality between events in the distributed system.
     *
     * @return a copy of the sender's vector clock when the message was created
     */
    public VSVectorTime getVectorTime() {
        return vectorTime;
    }

    /**
     * Checks whether this message was sent by a server or client protocol instance.
     * This distinction is important for protocols that have different behavior
     * for server and client roles.
     *
     * @return true if sent by a server protocol instance, false if sent by a client
     */
    public boolean isServerMessage() {
        return isServerMessage;
    }

    /* (non-Javadoc)
     * @see prefs.VSPrefs#toString()
     */
    public String toString() {
        StringBuffer buffer = new StringBuffer();

        buffer.append("ID: ");
        buffer.append(messageID);
        buffer.append("; ");
        buffer.append(prefs.getString("lang.protocol"));
        buffer.append(": ");
        buffer.append(VSRegisteredEvents.getShortnameByClassname(
                          getProtocolClassname()));

        return buffer.toString();
    }

    /**
     * Returns an extended string representation of this message.
     * This includes the basic message information plus all stored preferences/data.
     *
     * @return detailed string representation including ID, protocol, and all message data
     */
    public String toStringFull() {
        return toString() + "; " + super.toString();
    }

    /**
     * Compares this message with another message for equality.
     * Two messages are considered equal if they have the same message ID.
     *
     * @param message the message to compare with this message
     * @return true if both messages have the same ID, false otherwise
     */
    public boolean equals(VSMessage message) {
        return messageID == message.getMessageID();
    }
}

