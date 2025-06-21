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
    /** Event priority constants for task ordering */
    public static final int PRIORITY_HIGHEST = -3;  // Process recover events
    public static final int PRIORITY_HIGH = -2;     // Process crash events  
    public static final int PRIORITY_MEDIUM = -1;   // Protocol events
    public static final int PRIORITY_NORMAL = 0;    // All other events
    
    /** Class name prefix used by Java reflection */
    private static final String CLASS_PREFIX = "class ";
    private static final int CLASS_PREFIX_LENGTH = 6;
    
    /** The prefs. */
    public VSPrefs prefs;

    /** The process. */
    public VSAbstractProcess process;

    /** The event shortname. */
    private String eventShortname;

    /** The event classname. */
    private String eventClassname;

    /**
     * Checks if this event is an internal event.
     * Internal events are system events that don't directly correspond to user actions.
     * 
     * @return true if this is an internal event, false otherwise
     */
    public boolean isInternalEvent() {
        return false;
    }
    
    /**
     * Checks if this event can be serialized for saving/loading simulations.
     * Most events are serializable, but some runtime-only events may not be.
     * 
     * @return true if this event can be serialized, false otherwise
     */
    public boolean isSerializable() {
        return true;
    }
    
    /**
     * Checks if this event represents receiving a message.
     * Message receive events are triggered when a process receives a message.
     * 
     * @return true if this is a message receive event, false otherwise
     */
    public boolean isMessageReceiveEvent() {
        return false;
    }
    
    /**
     * Checks if this event represents a process recovery.
     * Process recover events restore a crashed process to operational state.
     * 
     * @return true if this is a process recover event, false otherwise
     */
    public boolean isProcessRecoverEvent() {
        return false;
    }
    
    /**
     * Checks if this event represents a process crash.
     * Process crash events simulate process failures in the distributed system.
     * 
     * @return true if this is a process crash event, false otherwise
     */
    public boolean isProcessCrashEvent() {
        return false;
    }
    
    /**
     * Checks if this event is a protocol-related event.
     * Protocol events manage protocol activation/deactivation.
     * 
     * @return true if this is a protocol event, false otherwise
     */
    public boolean isProtocolEvent() {
        return false;
    }
    
    /**
     * Determines if executing this event should increase the process's timestamps.
     * Most events increase timestamps, but some internal events may not.
     * This affects both Lamport and vector clocks based on preferences.
     * 
     * @return true if timestamps should be increased when this event executes
     */
    public boolean shouldIncreaseTimestamps() {
        return true;
    }
    
    /**
     * Get the priority of this event for ordering in VSTask comparisons.
     * Lower values have higher priority.
     * 
     * @return the event priority
     */
    public int getEventPriority() {
        return PRIORITY_NORMAL;
    }
    
    /**
     * Check if this event is copyable.
     * 
     * @return true if this event can be copied
     */
    public boolean isCopyable() {
        return this instanceof VSCopyableEvent;
    }

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

        if (!isCopyable())
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
        if (eventClassname.startsWith(CLASS_PREFIX))
            eventClassname = eventClassname.substring(CLASS_PREFIX_LENGTH);

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
        
        // Always use current localization strings
        this.eventShortname = VSRegisteredEvents.getShortnameByClassname(eventClassname);
        if (this.eventShortname == null) {
            this.eventShortname = createShortname(savedEventShortname);
        }

        if (VSSerialize.DEBUG) {
            System.out.println("eventClassname: " + eventClassname);
            // Note: eventShortname might be null here for internal events
            // that set their shortname in their own deserialize method
            if (eventShortname != null) {
                System.out.println("eventShortname: " + eventShortname);
            }
        }

        serialize.setObject(id, "event", this);

        /** For later backwards compatibility, to add more stuff */
        objectInputStream.readObject();
    }
}
