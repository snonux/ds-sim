package events.internal;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;

import core.VSAbstractProcess;
import core.VSInternalProcess;
import events.VSRegisteredEvents;
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
     * Create a VSProtocolScheduleEvent object for deserialization.
     */
    public VSProtocolScheduleEvent() {
    }

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
        setShortname(createShortname(null));
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
        if (protocol == null) {
            protocol = resolveProtocolFromProcess();
        }

        if (protocol == null) {
            return;
        }

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

        /** For later backwards compatibility, to add more stuff */
        objectInputStream.readObject();

    }

    protected String createShortname(String savedShortname) {
        if (prefs == null) {
            return savedShortname != null
                ? savedShortname
                : "Protocol Schedule";
        }

        if (protocol == null || protocol.getClassname() == null) {
            return prefs.getString("lang.events.internal.VSProtocolScheduleEvent.short");
        }

        String protocolShortname =
            VSRegisteredEvents.getShortnameByClassname(protocol.getClassname());
        if (protocolShortname == null)
            protocolShortname = protocol.getClassname();

        return protocolShortname + " "
            + (isServerSchedule
               ? prefs.getString("lang.server")
               : prefs.getString("lang.client"))
            + " "
            + prefs.getString("lang.events.internal.VSProtocolScheduleEvent.short");
    }

    @SuppressWarnings("unchecked")
    private VSAbstractProtocol resolveProtocolFromProcess() {
        if (!(process instanceof VSInternalProcess internalProcess)) {
            return null;
        }

        try {
            VSAbstractProtocol raftProtocol =
                internalProcess.getProtocolObject(
                    "protocols.implementations.VSRaftProtocol");
            if (raftProtocol != null) {
                return raftProtocol;
            }

            Field field = VSAbstractProcess.class.getDeclaredField("protocolsToReset");
            field.setAccessible(true);

            ArrayList<VSAbstractProtocol> protocols =
                (ArrayList<VSAbstractProtocol>) field.get(internalProcess);
            if (protocols == null || protocols.isEmpty()) {
                return null;
            }

            VSAbstractProtocol activeProtocol = null;
            for (VSAbstractProtocol candidate : protocols) {
                if (candidate == null) {
                    continue;
                }

                if ("protocols.implementations.VSRaftProtocol".equals(
                        candidate.getClassname())) {
                    return candidate;
                }

                if (activeProtocol == null &&
                        (candidate.isServer() || candidate.isClient())) {
                    activeProtocol = candidate;
                }
            }

            if (activeProtocol != null) {
                return activeProtocol;
            }

            return protocols.get(0);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }
}
