package events.internal;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import core.VSInternalProcess;
import events.VSAbstractEvent;
import events.VSCopyableEvent;
import events.VSRegisteredEvents;
import protocols.VSAbstractProtocol;
import serialize.VSSerialize;

/**
 * The class VSProtocolEvent, this event is used if a protocol (server or
 * client part) of a process gets enabled or disabled, an object of this class
 * can be for 4 different purporses! Activation of the client protocol,
 * deactivation of the client protocol, activation of the server protocol,
 * deactivation of the server protocol.
 *
 * @author Paul C. Buetow
 */
public class VSProtocolEvent extends VSAbstractInternalEvent
    implements VSCopyableEvent {
    /** The protocol classname. */
    private String protocolClassname;

    /** The event is a client protocol if true. Else it is a server protocol */
    private boolean isClientProtocol;

    /** The event is a protocol activation if true. Else it is a deactivation */
    private boolean isProtocolActivation;

    /* (non-Javadoc)
     * @see events.VSCopyableEvent#initCopy(events.VSAbstractEvent)
     */
    public void initCopy(VSAbstractEvent copy) {
        VSProtocolEvent protocolEventCopy = (VSProtocolEvent) copy;
        protocolEventCopy.isClientProtocol(isClientProtocol);
        protocolEventCopy.isProtocolActivation(isProtocolActivation);
        protocolEventCopy.setProtocolClassname(protocolClassname);
    }

    /* (non-Javadoc)
     * @see events.VSAbstractEvent#onInit()
     */
    public void onInit() {
        setClassname(getClass().toString());
    }

    /**
     * Sets if it is a client protocol activation/deactivation.
     *
     * @param isClientProtocol the event is client protocol if true. the event
     * is a server protocol if false.
     */
    public void isClientProtocol(boolean isClientProtocol) {
        this.isClientProtocol = isClientProtocol;
    }

    /**
     * Checks if it is a client protocol activation/deactivation.
     *
     * @return the event is client protocol if true. the event
     * is a server protocol if false.
     */
    public boolean isClientProtocol() {
        return isClientProtocol;
    }

    /**
     * Sets if it is protocol activation.
     *
     * @param isProtocolActivation true, if it is a protocol activation. false,
     *	if it is a protocol deactivation.
     */
    public void isProtocolActivation(boolean isProtocolActivation) {
        this.isProtocolActivation = isProtocolActivation;
    }

    /**
     * Checks if it is protocol activation.
     *
     * @return true, if it is a protocol activation. false, if it is a protocol
     *	deactivation.
     */
    public boolean isProtocolActivation() {
        return isProtocolActivation;
    }

    /**
     * Sets the protocol classname.
     *
     * @param protocolClassname the new protocol classname
     */
    public void setProtocolClassname(String protocolClassname) {
        this.protocolClassname = protocolClassname;
    }

    /* (non-Javadoc)
     * @see events.VSAbstractEvent#onStart()
     */
    public void onStart() {
        VSInternalProcess internalProcess = (VSInternalProcess) process;
        VSAbstractProtocol protocol =
            internalProcess.getProtocolObject(protocolClassname);

        if (isClientProtocol)
            protocol.isClient(isProtocolActivation);
        else
            protocol.isServer(isProtocolActivation);

        StringBuffer buffer = new StringBuffer();
        buffer.append(VSRegisteredEvents.getShortnameByClassname(
                          protocolClassname));

        buffer.append(" ");
        buffer.append(isClientProtocol
                      ? prefs.getString("lang.client")
                      : prefs.getString("lang.server"));

        buffer.append(" ");
        buffer.append(isProtocolActivation
                      ? prefs.getString("lang.activated")
                      : prefs.getString("langactivated"));

        log(buffer.toString());
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

        objectOutputStream.writeObject(protocolClassname);
        objectOutputStream.writeObject(Boolean.valueOf(isClientProtocol));
        objectOutputStream.writeObject(Boolean.valueOf(isProtocolActivation));

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
            System.out.println("Deserializing: VSProtocolEvent");

        /** For later backwards compatibility, to add more stuff */
        objectInputStream.readObject();

        protocolClassname = (String) objectInputStream.readObject();

        isClientProtocol = ((Boolean)
                            objectInputStream.readObject()).booleanValue();;
        isProtocolActivation = ((Boolean)
                                objectInputStream.readObject()).booleanValue();;

        /** For later backwards compatibility, to add more stuff */
        objectInputStream.readObject();
    }
}
