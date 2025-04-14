package events;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import core.VSAbstractProcess;
import core.VSInternalProcess;
import exceptions.VSEventNotCopyableException;
import prefs.VSPrefs;
import prefs.VSSerializablePrefs;
import serialize.VSSerialize;

/**
 * The class VSAbstractEvent. This abstract class defines the basic framework
 * of each event. an event is used to fullfill a specific task. An event object
 * will get stored in a VSTask object.
 *
 * @author Paul C. Buetow
 */
abstract public class VSAbstractEvent extends VSSerializablePrefs {
    /** The prefs. */
    public VSPrefs prefs;

    /** The process. */
    public VSAbstractProcess process;

    /** The event shortname. */
    private String eventShortname;

    /** The event classname. */
    private String eventClassname;

    /**
     * Creates a copy of the event and using a new process.
     *
     * @param theProcess The new process
     * @return The copy
     */
    final public VSAbstractEvent getCopy(VSInternalProcess theProcess)
    throws VSEventNotCopyableException {

        if (theProcess == null)
            theProcess = (VSInternalProcess) process;

        if (!(this instanceof VSCopyableEvent))
            throw new VSEventNotCopyableException(
                eventShortname + " (" + eventClassname + ")");

        VSAbstractEvent copy =
            VSRegisteredEvents.createEventInstanceByClassname(
                eventClassname, theProcess);

        ((VSCopyableEvent) this).initCopy(copy);
        copy.setShortname(eventShortname);

        return copy;
    }

    /**
     * Creates a copy of the event.
     *
     * @return The copy
     */
    final public VSAbstractEvent getCopy() throws VSEventNotCopyableException {
        return getCopy(null);
    }

    /**
     * Inits the event.
     *
     * @param process the process
     */
    public void init(VSInternalProcess process) {
        if (this.process == null) {
            this.process = process;
            this.prefs = process.getPrefs();
            init();
        }
    }

    /**
     * Inits the event without setting the processes and prefs variables
     * of the object.
     */
    public void init() {
        onInit();
    }

    /**
     * Sets the classname.
     *
     * @param eventClassname the new classname
     */
    public final void setClassname(String eventClassname) {
        if (eventClassname.startsWith("class "))
            eventClassname = eventClassname.substring(6);

        this.eventClassname = eventClassname;
    }

    /**
     * Gets the classname.
     *
     * @return the classname
     */
    public String getClassname() {
        return eventClassname;
    }

    /**
     * Gets the name.
     *
     * @return the name
     */
    public String getName() {
        return VSRegisteredEvents.getNameByClassname(eventClassname);
    }

    /**
     * Sets the shortname.
     *
     * @param eventShortname the new shortname
     */
    public void setShortname(String eventShortname) {
        this.eventShortname = eventShortname;
    }

    /**
     * Gets the shortname.
     *
     * @return the shortname
     */
    public String getShortname() {
        if (eventShortname == null)
            return VSRegisteredEvents.getShortnameByClassname(eventClassname);

        return eventShortname;
    }

    /**
     * Gets the process.
     *
     * @return the process
     */
    public VSAbstractProcess getProcess() {
        return process;
    }

    /**
     * Logg a specific message.
     *
     * @param message the loging message
     */
    public void log(String message) {
        process.log(/*toString() + "; " + */message);
    }

    /**
     * Checks if the event equals to another event..
     *
     * @param event the event to compare against.
     *
     * @return true, if the events are the same (have the same event id)
     */
    public boolean equals(VSAbstractEvent event) {
        return super.getID() == event.getID();
    }

    /**
     * Every event has its own initialize method.
     */
    abstract public void onInit();

    /**
     * Every event can get started. This method get's executed if the event
     * takes place.
     */
    abstract public void onStart();

    /**
     * Every event has to be able to set its own shortname
     *
     * @param shortName The saved short name. May be overwritten due wrong lang
     *
     * @return The event's shortname
     */
    abstract protected String createShortname(String savedShortname);

    /* (non-Javadoc)
     * @see serialize.VSSerializable#serialize(serialize.VSSerialize,
     *	java.io.ObjectOutputStream)
     */
    public synchronized void serialize(VSSerialize serialize,
                                       ObjectOutputStream objectOutputStream)
    throws IOException {
        super.serialize(serialize, objectOutputStream);

        if (VSSerialize.DEBUG)
            System.out.println("Serializing: VSAbstractEvent; id="+getID());

        /** For later backwards compatibility, to add more stuff */
        objectOutputStream.writeObject(Boolean.valueOf(false));

        objectOutputStream.writeObject(Integer.valueOf(super.getID()));
        objectOutputStream.writeObject(eventShortname);
        objectOutputStream.writeObject(eventClassname);

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
            System.out.print("Deserializing: VSAbstractEvent ");

        /** For later backwards compatibility, to add more stuff */
        objectInputStream.readObject();

        int id = ((Integer) objectInputStream.readObject()).intValue();
        String savedEventShortname = (String) objectInputStream.readObject();
        this.eventClassname = (String) objectInputStream.readObject();
        this.eventShortname = createShortname(savedEventShortname);

        if (VSSerialize.DEBUG) {
            System.out.println("eventClassname: " + eventClassname);
            System.out.println("eventShortname: " + eventShortname);
        }

        serialize.setObject(id, "event", this);

        /** For later backwards compatibility, to add more stuff */
        objectInputStream.readObject();
    }
}
