package events;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;
import java.util.Vector;

import core.VSInternalProcess;
import prefs.VSPrefs;
import utils.VSClassLoader;

/**
 * The class VSRegisteredEvents. This class is responsible to manage all
 * events. It manages the event classnames, the event shortnames and the event
 * names. It also checks if a protocol (which is an event as well) has
 * variables which are editable through the GUI of the simulator.
 *
 * @author Paul C. Buetow
 */
public final class VSRegisteredEvents {
    /** The event classnames by eventnames. */
    private static HashMap<String,String> eventClassnamesByNames =
        new HashMap<String,String>();

    /** The event shortnames by classnames. */
    private static HashMap<String,String> eventShortnamesByClassnames =
        new HashMap<String,String>();

    /** The event names by classnames. */
    private static HashMap<String,String> eventNamesByClassnames =
        new HashMap<String,String>();

    /** The event classnames by shortnames. */
    private static HashMap<String,String> eventClassnamesByShortnames =
        new HashMap<String,String>();

    /** The editable protocols classnames. */
    private static ArrayList<String> editableProtocolsClassnames =
        new ArrayList<String>();

    private static HashMap<String,ArrayList<String>> clientVariables =
        new HashMap<String,ArrayList<String>>();

    private static HashMap<String,ArrayList<String>> serverVariables =
        new HashMap<String,ArrayList<String>>();

    private static HashMap<String,Boolean> isOnServerStartProtocol =
        new HashMap<String,Boolean>();

    /** The prefs. */
    private static VSPrefs prefs;

    /**
     * Registers available events.
     *
     * @param prefs_ the prefs_
     */
    public static void init(VSPrefs prefs_) {
        prefs = prefs_;
        registerEvent("events.implementations.VSProcessCrashEvent");
        registerEvent("events.implementations.VSProcessRecoverEvent");
        registerEvent("protocols.implementations.VSBasicMulticastProtocol");
        registerEvent("protocols.implementations.VSBerkelyTimeProtocol");
        registerEvent("protocols.implementations.VSBroadcastProtocol");
        registerEvent("protocols.implementations.VSDummyProtocol");
        registerEvent("protocols.implementations.VSExternalTimeSyncProtocol");
        registerEvent("protocols.implementations.VSInternalTimeSyncProtocol");
        registerEvent("protocols.implementations.VSOnePhaseCommitProtocol");
        registerEvent("protocols.implementations.VSPingPongProtocol");
        registerEvent("protocols.implementations.VSReliableMulticastProtocol");
        registerEvent("protocols.implementations.VSTwoPhaseCommitProtocol");

        /* Make dummy objects of each protocol, to see if they contain VSPrefs
           values to edit */
        Vector<String> protocolClassnames = getProtocolClassnames();
        VSClassLoader classLoader = new VSClassLoader();

        for (String protocolClassname : protocolClassnames) {
            Object serverObject = classLoader.newInstance(protocolClassname);
            Object clientObject = classLoader.newInstance(protocolClassname);

            if (clientObject instanceof protocols.VSAbstractProtocol &&
                    serverObject instanceof protocols.VSAbstractProtocol) {

                protocols.VSAbstractProtocol serverProtocol =
                    (protocols.VSAbstractProtocol) serverObject;
                protocols.VSAbstractProtocol clientProtocol =
                    (protocols.VSAbstractProtocol) clientObject;

                serverProtocol.onServerInit();
                clientProtocol.onClientInit();

                if (!serverProtocol.isEmpty() || !clientProtocol.isEmpty())
                    editableProtocolsClassnames.add(protocolClassname);

                if (!serverProtocol.isEmpty()) {
                    ArrayList<String> variables = new ArrayList<String>();
                    variables.addAll(serverProtocol.getAllFullKeys());
                    serverVariables.put(protocolClassname, variables);
                }

                if (!clientProtocol.isEmpty()) {
                    ArrayList<String> variables = new ArrayList<String>();
                    variables.addAll(clientProtocol.getAllFullKeys());
                    clientVariables.put(protocolClassname, variables);
                }

                if (serverProtocol.hasOnServerStart())
                    isOnServerStartProtocol.put(protocolClassname,
                                                Boolean.valueOf(true));
            }
        }
    }

    /**
     * Gets the editable protocols classnames.
     *
     * @return the editable protocols classnames
     */
    public static ArrayList<String> getEditableProtocolsClassnames() {
        return editableProtocolsClassnames;
    }

    /**
     * Gets the protocols server variable names.
     *
     * @return The variable names
     */
    public static ArrayList<String> getProtocolServerVariables(
        String protocolClassname) {
        return serverVariables.get(protocolClassname);
    }

    /**
     * Gets the protocols server variable names.
     *
     * @return The variable names
     */
    public static ArrayList<String> getProtocolClientVariables(
        String protocolClassname) {
        return clientVariables.get(protocolClassname);
    }

    /**
     * Gets the protocol names.
     *
     * @return the protocol names
     */
    public static Vector<String> getProtocolNames() {
        Set<String> set =  eventClassnamesByNames.keySet();
        Vector<String> vector = new Vector<String>();

        for (String eventName : set)
            if (getClassnameByEventname(eventName).startsWith(
                        "protocols.implementations"))
                vector.add(eventName);

        Collections.sort(vector);

        return vector;
    }

    /**
     * Gets the protocol classnames.
     *
     * @return the protocol classnames
     */
    public static Vector<String> getProtocolClassnames() {
        ArrayList<String> shortnames = new ArrayList<String>();
        shortnames.addAll(eventClassnamesByShortnames.keySet());
        Collections.sort(shortnames);
        Vector<String> vector = new Vector<String>();

        for (String eventShortname : shortnames) {
            String eventClassname = getClassnameByShortname(eventShortname);
            if (eventClassname.startsWith("protocols.implementations"))
                vector.add(eventClassname);
        }

        return vector;
    }

    /**
     * Gets the non protocol names.
     *
     * @return the non protocol names
     */
    public static Vector<String> getNonProtocolNames() {
        Set<String> set =  eventClassnamesByNames.keySet();
        Vector<String> vector = new Vector<String>();

        for (String eventName : set)
            if (getClassnameByEventname(eventName).startsWith(
                        "events.implementations"))
                vector.add(eventName);

        Collections.sort(vector);

        return vector;
    }

    /**
     * Gets the non protocol classnames.
     *
     * @return the non protocol classnames
     */
    public static Vector<String> getNonProtocolClassnames() {
        Set<String> set =  eventNamesByClassnames.keySet();
        Vector<String> vector = new Vector<String>();

        for (String eventClassname : set)
            if (eventClassname.startsWith("events.implementations"))
                vector.add(eventClassname);

        Collections.sort(vector);

        return vector;
    }

    /**
     * Gets the classname.
     *
     * @param eventName the event name
     *
     * @return the classname
     */
    public static String getClassnameByEventname(String eventName) {
        return eventClassnamesByNames.get(eventName);
    }

    /**
     * Gets the name.
     *
     * @param eventClassname the event classname
     *
     * @return the name
     */
    public static String getNameByClassname(String eventClassname) {
        return eventNamesByClassnames.get(eventClassname);
    }

    /**
     * Gets the shortname.
     *
     * @param eventClassname the event classname
     *
     * @return the shortname
     */
    public static String getShortnameByClassname(String eventClassname) {
        return eventShortnamesByClassnames.get(eventClassname);
    }

    /**
     * Gets the classname.
     *
     * @param eventShortname the event shortname
     *
     * @return the shortname
     */
    public static String getClassnameByShortname(String eventShortname) {
        return eventClassnamesByShortnames.get(eventShortname);
    }

    /**
     * Checks if the protocol uses onServerStart or onClientStart
     *
     * @param protocolClassname the protocol's classname
     *
     * @return true if onServerStart, false if onClientStart
     */
    public static boolean isOnServerStartProtocol(String protocolClassname) {
        if (isOnServerStartProtocol.containsKey(protocolClassname)) {
            Boolean bool = isOnServerStartProtocol.get(protocolClassname);
            return bool.booleanValue();
        }

        return false;
    }

    /**
     * Creates the event instance by classname.
     *
     * @param eventClassname the event classname
     * @param process the process
     *
     * @return An instance of the event classname, if exists. Else null.
     */
    public static VSAbstractEvent createEventInstanceByClassname(
        String eventClassname, VSInternalProcess process) {
        Object protocolObj = new VSClassLoader().newInstance(eventClassname);

        if (protocolObj instanceof VSAbstractEvent) {
            VSAbstractEvent event = (VSAbstractEvent) protocolObj;
            event.init(process);
            return event;
        }

        return null;
    }

    /**
     * Creates the event instance by name.
     *
     * @param eventName the event name
     * @param process the process
     *
     * @return An instance of the event, if exists. Else null.
     */
    public static VSAbstractEvent createEventInstanceByName(String eventName,
            VSInternalProcess process) {
        return createEventInstanceByClassname(
                   eventClassnamesByNames.get(eventName), process);
    }

    /**
     * Registers an event. Use the language settings of VSPrefs.
     *
     * @param eventClassname the event classname
     */
    private static void registerEvent(String eventClassname) {
        String eventName =
            prefs.getString("lang." + eventClassname);
        String eventShortname =
            prefs.getString("lang." + eventClassname + ".short");
        registerEvent(eventClassname, eventName, eventShortname);
    }

    /**
     * Registers an event.
     *
     * @param eventClassname the event classname
     * @param eventName the event name
     * @param eventShortname the event shortname
     */
    private static void registerEvent(String eventClassname, String eventName,
                                      String eventShortname) {
        if (eventShortname == null)
            eventShortname = eventName;

        eventNamesByClassnames.put(eventClassname, eventName);
        eventShortnamesByClassnames.put(eventClassname, eventShortname);
        eventClassnamesByNames.put(eventName, eventClassname);
        eventClassnamesByShortnames.put(eventShortname, eventClassname);
    }
}
