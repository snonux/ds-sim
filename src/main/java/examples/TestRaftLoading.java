package examples;

import events.VSRegisteredEvents;
import prefs.VSDefaultPrefs;
import java.util.Vector;

/**
 * Test if Raft protocol is properly registered and loadable
 */
public class TestRaftLoading {
    public static void main(String[] args) {
        // Initialize
        VSDefaultPrefs prefs = new VSDefaultPrefs();
        prefs.fillWithDefaults();
        VSRegisteredEvents.init(prefs);
        
        // List all registered protocols
        System.out.println("=== Registered Protocols ===");
        Vector<String> protocolNames = VSRegisteredEvents.getProtocolNames();
        for (String name : protocolNames) {
            String className = VSRegisteredEvents.getClassnameByEventname(name);
            System.out.println(name + " -> " + className);
        }
        
        System.out.println("\n=== Protocol Classnames ===");
        Vector<String> protocolClassnames = VSRegisteredEvents.getProtocolClassnames();
        for (String className : protocolClassnames) {
            String shortName = VSRegisteredEvents.getShortnameByClassname(className);
            System.out.println(className + " (short: " + shortName + ")");
        }
        
        // Check Raft specifically
        System.out.println("\n=== Raft Protocol Check ===");
        String raftClass = "protocols.implementations.VSRaftProtocol";
        String raftShortName = VSRegisteredEvents.getShortnameByClassname(raftClass);
        String raftEventName = VSRegisteredEvents.getNameByClassname(raftClass);
        
        System.out.println("Class: " + raftClass);
        System.out.println("Short name: " + raftShortName);
        System.out.println("Event name: " + raftEventName);
        
        // Try to load the class
        try {
            Class<?> clazz = Class.forName(raftClass);
            System.out.println("Class loaded successfully: " + clazz.getName());
            
            // Check if it's a protocol
            if (protocols.VSAbstractProtocol.class.isAssignableFrom(clazz)) {
                System.out.println("✓ Is a valid protocol class");
            } else {
                System.out.println("✗ NOT a protocol class!");
            }
        } catch (ClassNotFoundException e) {
            System.out.println("✗ Class not found: " + e.getMessage());
        }
    }
}