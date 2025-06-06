package simulator;

import core.VSInternalProcess;
import core.VSTask;
import events.VSAbstractEvent;
import events.VSRegisteredEvents;
import events.internal.VSProtocolEvent;

/**
 * The class VSCreateTask, an object of this class represents how new
 * VSTask objects are to be created using JComboBox selections of the
 * GUI editor..
 *
 * @author Paul C. Buetow
 */
public class VSCreateTask {
    /** The event classname. */
    private String eventClassname;

    /** The create task menu string. */
    private String menuText;

    /** The protocol classname. */
    private String protocolClassname;

    /** The shortname. */
    private String shortname;

    /** The task is a protocol activation. */
    private boolean isProtocolActivation;

    /** The task is a protocol deactivation. */
    private boolean isProtocolDeactivation;

    /** The task is a client protocol. */
    private boolean isClientProtocol;

    /** True, if the task is a client request. false, if the task is a
     * server request
     */
    private boolean isRequest;

    /**
     * Instantiates a new VSCreateTask object.
     *
     * @param menuText the menu text
     * @param eventClassname the event classname
     */
    public VSCreateTask(String menuText, String eventClassname) {
        this.menuText = menuText;
        this.eventClassname = eventClassname;
    }

    /**
     * Instantiates a new VSCreateTask dummy object.
     *
     * @param menuText the menu text
     */
    public VSCreateTask(String menuText) {
        this.menuText = menuText;
        this.eventClassname = null;
    }

    /**
     * Sets if it is  a protocol activation task.
     *
     * @param isProtocolActivation true, if it is a protocol activation
     *	task.
     */
    public void isProtocolActivation(boolean isProtocolActivation) {
        this.isProtocolActivation = isProtocolActivation;

        if (isProtocolActivation)
            isProtocolDeactivation(false);
    }

    /**
     * Sets if it is  a protocol deactivation task.
     *
     * @param isProtocolDeactivation true, if it is a protocol deactivation
     *	task.
     */
    public void isProtocolDeactivation(boolean isProtocolDeactivation) {
        this.isProtocolDeactivation = isProtocolDeactivation;

        if (isProtocolDeactivation)
            isProtocolActivation(false);
    }

    /**
     * Sets if it is a client protocol.
     *
     * @param isClientProtocol the is client protocol
     */
    public void isClientProtocol(boolean isClientProtocol) {
        this.isClientProtocol = isClientProtocol;
    }

    /**
     * Sets if it is a client request.
     *
     * @param isRequest the is client request
     */
    public void isRequest(boolean isRequest) {
        this.isRequest = isRequest;
    }

    /**
     * Checks if it is a dummy object..
     *
     * @return true, if dummy
     */
    public boolean isDummy() {
        return eventClassname == null;
    }

    /**
     * Sets the protocol classname.
     *
     * @param protocolClassname the protocol classname
     */
    public void setProtocolClassname(String protocolClassname) {
        this.protocolClassname = protocolClassname;
    }

    /**
     * Sets the shortname.
     *
     * @param shortname the shortname
     */
    public void setShortname(String shortname) {
        this.shortname = shortname;
    }

    /**
     * Gets the create tasks menu text.
     *
     * @return The text
     */
    public String getMenuText() {
        return menuText;
    }

    /**
     * Creates the task.
     *
     * @param process the process
     * @param time the time
     * @param localTimedTask the local timed task
     *
     * @return the new task
     */
    public VSTask createTask(VSInternalProcess process, long time,
                             boolean localTimedTask) {
        VSAbstractEvent event = null;

        if (isRequest) {
            event = process.getProtocolObject(eventClassname);

        } else {
            event = VSRegisteredEvents.createEventInstanceByClassname(
                        eventClassname, process);
        }

        event.init(process);
        if (shortname != null)
            event.setShortname(shortname);

        if (isProtocolActivation || isProtocolDeactivation) {
            VSProtocolEvent protocolEvent = (VSProtocolEvent) event;
            protocolEvent.setProtocolClassname(protocolClassname);
            protocolEvent.isProtocolActivation(isProtocolActivation);
            protocolEvent.isClientProtocol(isClientProtocol);
        }

        return new VSTask(time, process, event, localTimedTask);
    }
}

