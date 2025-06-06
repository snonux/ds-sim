package events.internal;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import protocols.VSAbstractProtocol;
import serialize.VSNotSerializable;
import serialize.VSSerialize;

/**
 * The class VSProtocolScheduleEvent, this event is used if a protocol (which
 * is a subclass of VSAbstractProtocol) reschedules itself to run again on a
 * specific time.
 *
 * @author Paul C. Buetow
 */
public class VSProtocolScheduleEvent extends VSAbstractInternalEvent
    implements VSNotSerializable {
    /** The event is a server protocol schedule. */
    private boolean isServerSchedule; /* true = server, false = client */

    /** The reference to the protocol object to schedule. */
    private VSAbstractProtocol protocol;

    /**
     * Create a VSProtocolScheduleEvent object
     *
     * @param protocol the protocol
     * @param isServerSchedule the event is a client protocol schedule if
     *	false, else server schedule
     */
    public VSProtocolScheduleEvent(VSAbstractProtocol protocol,
                                   boolean isServerSchedule) {
        this.protocol = protocol;
        this.isServerSchedule = isServerSchedule;
    }

    /* (non-Javadoc)
     * @see events.VSAbstractEvent#onInit()
     */
    public void onInit() {
        setClassname(getClass().toString());
    }

    /**
     * Sets if it is client protocol schedule.
     *
     * @param isServerSchedule false, if the event is a client protocol
     * schedule. true, if server.
     */
    public void isServerSchedule(boolean isServerSchedule) {
        this.isServerSchedule = isServerSchedule;
    }

    /**
     * Sets if it is client protocol schedule.
     *
     * @return false, if the event is a client protocol schedule. true, if
     *	server.
     */
    public boolean isServerSchedule() {
        return isServerSchedule;
    }

    /**
     * Sets the protocol.
     *
     * @param protocol the protocol
     */
    public void setProtocol(VSAbstractProtocol protocol) {
        this.protocol = protocol;
    }

    /**
     * Gets the protocol.
     *
     * @return the protocol
     */
    public VSAbstractProtocol getProtocol() {
        return protocol;
    }

    /* (non-Javadoc)
     * @see events.VSAbstractEvent#onStart()
     */
    public void onStart() {
        if (isServerSchedule)
            protocol.onServerScheduleStart();
        else
            protocol.onClientScheduleStart();
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

    }
}
