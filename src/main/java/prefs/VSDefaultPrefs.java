package prefs;

import java.awt.Color;
import java.awt.event.KeyEvent;
import utils.VSVersionInfo;

/**
 * The class VSDefaultPrefs, makes sure that the simulator has its default
 * configuration values.
 *
 * @author Paul C. Buetow
 */
public class VSDefaultPrefs extends VSSerializablePrefs {
    /**
     * Inits a prefs object with default values.
     *
     * @return the lang.process.removeprefs
     */
    public static VSPrefs init() {
        VSDefaultPrefs prefs = new VSDefaultPrefs();
        prefs.fillWithDefaults();
        return prefs;
    }

    /**
     * Fill everything with ts defaults.
     */
    public void fillWithDefaults() {
        super.clear();
        addWithDefaults();
    }

    /**
     * Adds default values if not existent.
     */
    public void addWithDefaults() {
        fillDefaultBooleans();
        fillDefaultColors();
        fillDefaultFloats();
        fillDefaultIntegers();
        fillDefaultLongs();
        fillDefaultStrings();
    }

    /**
     * Fill with default strings.
     */
    public void fillDefaultStrings() {
        initString("lang.about", "About");
        initString("lang.about.info", "This program used to be the diploma thesis of Paul C. Buetow. Please contact vs-sim@dev.buetow.org, if you find any errors!");
        initString("lang.activate", "activate");
        initString("lang.activated", "activated");
        initString("lang.deactivated", "deactivated");
        initString("lang.actualize", "Activation");
        initString("lang.all", "All");
        initString("lang.antialiasing", "Anti-Aliasing");
        initString("lang.cancel", "Abort");
        initString("lang.client", "Client");
        initString("lang.clientrequest.start", "Start client request");
        initString("lang.close", "Close");
        initString("lang.colorchooser", "Color chooser");
        initString("lang.colorchooser2", "Please select color");
        initString("lang.copy", "Copy");
        initString("lang.crashed", "Crashed");
        initString("lang.dat", "Simulation (.dat)");
        initString("langactivate", "deactivate");
        initString("langactivated", "deactivated");
        initString("lang.default", "Defaults");
        initString("lang.edit", "Edit");
        initString("lang.editor", "Editor");
        initString("lang.event", "Event");
        initString("lang.event.add.global", "Add global event");
        initString("lang.event.add.local", "Add local event");
        initString("lang.event.add.time", "at");
        initString("lang.events", "Events");
        initString("lang.events.process", "Process events");
        initString("lang.file", "File");
        initString("lang.filter", "Filter");
        initString("lang.logging.active", "Logging");
        initString("lang.logging.clear", "Clear logs");
        initString("lang.message", "Message");
        initString("lang.message.recv", "Message received");
        initString("lang.message.sent", "Message sent");
        initString("lang.mode.expert", "Expert mode");
        initString("lang.name", VSVersionInfo.getFullVersionString());
        initString("lang.ok", "OK");
        initString("lang.open", "Open");
        initString("lang.pause", "Pause");
        initString("lang.prefs", "Preferences");
        initString("lang.prefs.color", "Color preferences");
        initString("lang.prefs.diverse", "Diverse preferences");
        initString("lang.prefs.ext", "Extended preferences");
        initString("lang.prefs.message", "Message preferences");
        initString("lang.prefs.message.defaults", "Message prefs. for new processes");
        initString("lang.prefs.more", "More preferences");
        initString("lang.prefs.process", "Process preferences");
        initString("lang.prefs.process", "Standard process preferences");
        initString("lang.prefs.process.defaults", "Preferences for new processes");
        initString("lang.prefs.process.ext", "Extended process preferences");
        initString("lang.prefs.protocols", "Protocol preferences");
        initString("lang.prefs.simulator", "Simulator preferences");
        initString("lang.process", "Process");
        initString("lang.process.add.new", "Add new process");
        initString("lang.process.crash", "Crash process");
        initString("lang.process.edit", "Edit process");
        initString("lang.process.id", "PID");
        initString("lang.process.new", "New process");
        initString("lang.process.not.selected", "No process selected");
        initString("lang.process.recover", "Recover process");
        initString("lang.process.remove", "Remove process");
        initString("lang.process.selected", "Selected process");
        initString("lang.process.time.local", "Local time");
        initString("lang.processes.all", "All processes");
        initString("lang.protocol", "Protocol");
        initString("lang.protocol.client", "Client side");
        initString("lang.protocol.editor", "Protocol editor");
        initString("lang.protocol.server", "Server side");
        initString("lang.protocol.tasks.activation", "Client-/Server protocol activation");
        initString("lang.protocol.tasks.client", "Client Task-Manager (Client request)");
        initString("lang.protocols", "Protocols");
        initString("lang.quit", "Quit");
        initString("lang.recovered", "Recovered");
        initString("lang.remove", "Remove");
        initString("lang.replay", "Repeat");
        initString("lang.reset", "Reset");
        initString("lang.save", "Save");
        initString("lang.saveas", "Save as");
        initString("lang.server", "Server");
        initString("lang.serverrequest.start", "Start server request");
        initString("lang.simulator", "Simulator");
        initString("lang.simulator.close", "Close simulation");
        initString("lang.simulator.finished", "Simulation closed");
        initString("lang.simulator.new", "New simulation");
        initString("lang.simulator.paused", "Simulation paused");
        initString("lang.simulator.resetted", "Simulation resetted");
        initString("lang.simulator.started", "Simulation started");
        initString("lang.start", "Start");
        initString("lang.stop", "Stop");
        initString("lang.takeover", "Take over");
        initString("lang.task", "Task");
        initString("lang.task.manager", "Event editor");
        initString("lang.tasks.fulfilled", "Fulfilled tasks");
        initString("lang.tasks.global", "Global tasks");
        initString("lang.tasks.local", "Local tasks");
        initString("lang.time", "Time");
        initString("lang.time.lamport", "Lamport time");
        initString("lang.time.vector", "Vector time");
        initString("lang.timed.global", "Global events");
        initString("lang.timed.local", "Local events");
        initString("lang.type", "Type");
        initString("lang.value", "Value");
        initString("lang.variable", "Variable");
        initString("lang.variables", "Variables");
        initString("lang.variables.global", "Global variables");
        initString("lang.window.close", "Close window");
        initString("lang.window.new", "New window");

        /* Protocol names */
        initString("lang.events.implementations.VSProcessCrashEvent", "Process Crash Event");
        initString("lang.events.implementations.VSProcessCrashEvent.short", "Process Crash");
        initString("lang.events.implementations.VSProcessRecoverEvent", "Process Recover Event");
        initString("lang.events.implementations.VSProcessRecoverEvent.short", "Process Recover");
        initString("lang.protocols.implementations.VSBasicMulticastProtocol", "Basic Multicast Protocol");
        initString("lang.protocols.implementations.VSBasicMulticastProtocol.short", "Basic Multicast");
        initString("lang.protocols.implementations.VSBerkelyTimeProtocol", "Berkley algorithm for internal sync.");
        initString("lang.protocols.implementations.VSBerkelyTimeProtocol.short", "Berkley Protocol");
        initString("lang.protocols.implementations.VSBroadcastProtocol", "Broadcast Protocol");
        initString("lang.protocols.implementations.VSBroadcastProtocol.short", "Broadcast");
        initString("lang.protocols.implementations.VSDummyProtocol", "Example/Dummy Protocol");
        initString("lang.protocols.implementations.VSDummyProtocol.short", "Example/Dummy");
        initString("lang.protocols.implementations.VSExternalTimeSyncProtocol", "Christians method for external sync.");
        initString("lang.protocols.implementations.VSExternalTimeSyncProtocol.short", "Christians");
        initString("lang.protocols.implementations.VSInternalTimeSyncProtocol", "Internal Synchronization Protocol");
        initString("lang.protocols.implementations.VSInternalTimeSyncProtocol.short", "Internal sync.");
        initString("lang.protocols.implementations.VSOnePhaseCommitProtocol", "One-Phase Commit Protocol");
        initString("lang.protocols.implementations.VSOnePhaseCommitProtocol.short", "1-Phase Commit");
        initString("lang.protocols.implementations.VSPingPongProtocol", "Ping-Pong Protocol");
        initString("lang.protocols.implementations.VSPingPongProtocol.short", "Ping-Pong");
        initString("lang.protocols.implementations.VSReliableMulticastProtocol", "Reliable Multicast Protocol");
        initString("lang.protocols.implementations.VSReliableMulticastProtocol.short", "Reliable Multicast");
        initString("lang.protocols.implementations.VSTwoPhaseCommitProtocol", "Two-Phase Commit");
        initString("lang.protocols.implementations.VSTwoPhaseCommitProtocol.short", "2-Phase Commit");
        initString("lang.protocols.implementations.VSRaftProtocol", "Raft Consensus Algorithm");
        initString("lang.protocols.implementations.VSRaftProtocol.short", "Raft Consensus");
        
        /* Timestamp event names */
        initString("lang.events.implementations.VSLamportTimestampEvent", "Lamport Timestamp Event");
        initString("lang.events.implementations.VSLamportTimestampEvent.short", "Lamport Trigger");
        initString("lang.events.implementations.VSVectorTimestampEvent", "Vector Timestamp Event");
        initString("lang.events.implementations.VSVectorTimestampEvent.short", "Vector Trigger");
        initString("lang.events.implementations.VSTimestampMonitorEvent", "Timestamp Monitor Event");
        initString("lang.events.implementations.VSTimestampMonitorEvent.short", "Timestamp Monitor");
        initString("lang.events.implementations.VSTimestampTriggeredEvent", "Timestamp Triggered Event");
        initString("lang.events.implementations.VSTimestampTriggeredEvent.short", "Timestamp Trigger");
        initString("lang.protocols.implementations.VSTimestampDemoProtocol", "Timestamp Demo Protocol");
        initString("lang.protocols.implementations.VSTimestampDemoProtocol.short", "Timestamp Demo");
    }

    /**
     * Fill with default integers.
     */
    public void fillDefaultIntegers() {
        /* Simulator prefs */
        initInteger("sim.process.num", 3, "Number of processes", 1, 6);
        initInteger("message.prob.outage", 0, "Message lost prob.", 0, 100, "%");
        initInteger("process.prob.crash", 0, "Process crash prob.", 0, 100, "%");
        initInteger("sim.seconds", 15, "Simulation duration", 5, 120, "s");

        /* Internal prefs */
        initInteger("keyevent.about", KeyEvent.VK_A, null, 0, 100);
        initInteger("keyevent.cancel", KeyEvent.VK_A, null, 0, 100);
        initInteger("keyevent.close", KeyEvent.VK_C, null, 0, 100);
        initInteger("keyevent.default", KeyEvent.VK_F, null, 0, 100);
        initInteger("keyevent.edit", KeyEvent.VK_E, null, 0, 100);
        initInteger("keyevent.file", KeyEvent.VK_D, null, 0, 100);
        initInteger("keyevent.new", KeyEvent.VK_N, null, 0, 100);
        initInteger("keyevent.actualize", KeyEvent.VK_A, null, 0, 100);
        initInteger("keyevent.takeover", KeyEvent.VK_B, null, 0, 100);
        initInteger("keyevent.ok", KeyEvent.VK_O, null, 0, 100);
        initInteger("keyevent.open", KeyEvent.VK_O, null, 0, 100);
        initInteger("keyevent.pause", KeyEvent.VK_P, null, 0, 100);
        initInteger("keyevent.prefs", KeyEvent.VK_P, null, 0, 100);
        initInteger("keyevent.prefs.ext", KeyEvent.VK_E, null, 0, 100);
        initInteger("keyevent.quit", KeyEvent.VK_B, null, 0, 100);
        initInteger("keyevent.replay", KeyEvent.VK_W, null, 0, 100);
        initInteger("keyevent.reset", KeyEvent.VK_R, null, 0, 100);
        initInteger("keyevent.save", KeyEvent.VK_S, null, 0, 100);
        initInteger("keyevent.saveas", KeyEvent.VK_V, null, 0, 100);
        initInteger("keyevent.simulator", KeyEvent.VK_S, null, 0, 100);
        initInteger("keyevent.start", KeyEvent.VK_S, null, 0, 100);
        initInteger("keyevent.stop", KeyEvent.VK_P, null, 0, 100);

        initInteger("div.window.prefs.xsize", 400, "Configuration window X-Axis", 550, 3200, "px");
        initInteger("div.window.prefs.ysize", 400, "Configuration window Y-Axis", 640, 2400, "px");
        initInteger("div.window.logsize", 300, "Log window Y-Axis", 100, 1000, "px");
        initInteger("div.window.splitsize", 320, "Toolbar X-Axis", 100, 1000, "px");
        initInteger("div.window.xsize", 1024, "Main window X-Axis", 750, 3200, "px");
        initInteger("div.window.ysize", 768, "Main window Y-Axis", 600, 2400, "px");
    }

    /**
     * Fill with default floats.
     */
    public void fillDefaultFloats() {
        /* Simulator prefs */
        initFloat("process.clock.variance", 0, "Clock variance");
        initFloat("sim.clock.speed", 0.5f, "Simulation play speed");
    }

    /**
     * Fill default longs.
     */
    public void fillDefaultLongs() {
        /* Simulator prefs */
        initLong("message.sendingtime.min", 500, "Max transmission time", "ms");
        initLong("message.sendingtime.max", 2000, "Min transmission time", "ms");
    }

    /**
     * Fill with default colors.
     */
    public void fillDefaultColors() {
        /* Internal prefs */
        initColor("col.background", new Color(0xFF, 0xFF, 0xFF));
        initColor("col.process.default", new Color(0x00, 0x00, 0x00));
        initColor("col.process.running", new Color(0x0D, 0xD8, 0x09));
        initColor("col.process.crashed", new Color(0xff, 0x00, 0x00));
        initColor("col.process.highlight", new Color(0xff, 0xA5, 0x00));
        initColor("col.process.line", new Color(0x00, 0x00, 0x00));
        initColor("col.process.secondline", new Color(0xAA, 0xAA, 0xAA));
        initColor("col.process.sepline", new Color(0xff, 0x00, 0x00));
        initColor("col.process.stopped", new Color(0x00, 0x00, 0x00));
        initColor("col.message.arrived", new Color(0x00, 0x85, 0xD2));
        initColor("col.message.sending", new Color(0x0D, 0xD8, 0x09));
        initColor("col.message.lost", new Color(0xFF, 0x00, 0x00));
    }

    /**
     * Fill with default booleans.
     */
    public void fillDefaultBooleans() {
        initBoolean("sim.mode.expert", false, "Expert mode");
        initBoolean("sim.message.own.recv", false, "Processes receive own messages");
        initBoolean("sim.message.prob.mean", true, "Use mean value of message lost prob.");
        initBoolean("sim.message.sendingtime.mean", true, "Use mean value of transmission times");
        initBoolean("sim.messages.relevant", true, "Only show relevant messages");
        initBoolean("sim.periodic", false, "Repeat simulation periodically");
        initBoolean("sim.update.lamporttime.all", false, "Lamport timestamps affect all events");
        initBoolean("sim.update.vectortime.all", false, "Vector timestamps affect all events");
    }
}
