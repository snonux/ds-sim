package constants;

/**
 * Central location for constants used throughout the DS-Sim application.
 * This class contains configuration values, limits, and other magic numbers
 * that were previously scattered throughout the codebase.
 *
 * @author Paul C. Buetow
 */
public final class VSConstants {
    
    // Prevent instantiation
    private VSConstants() {}
    
    /** Process configuration constants */
    public static final int MIN_PROCESSES = 1;
    public static final int MAX_PROCESSES = 6;
    public static final int DEFAULT_PROCESSES = 3;
    
    /** Percentage calculation */
    public static final int PERCENTAGE_RANGE = 101;
    
    /** Message timing constants (in milliseconds) */
    public static final long DEFAULT_MIN_MESSAGE_TIME = 500;
    public static final long DEFAULT_MAX_MESSAGE_TIME = 2000;
    
    /** Simulation duration constants (in seconds) */
    public static final int DEFAULT_SIMULATION_DURATION = 15;
    public static final int MIN_SIMULATION_DURATION = 5;
    public static final int MAX_SIMULATION_DURATION = 120;
    
    /** Window size defaults */
    public static final class WindowDefaults {
        public static final int PREFS_WINDOW_WIDTH = 400;
        public static final int PREFS_WINDOW_HEIGHT = 400;
        public static final int LOG_WINDOW_HEIGHT = 300;
        public static final int SPLIT_PANE_WIDTH = 320;
        public static final int MAIN_WINDOW_WIDTH = 1024;
        public static final int MAIN_WINDOW_HEIGHT = 768;
        
        // Window positioning
        public static final int X_LOCATION_OFFSET = 40;
        public static final int Y_LOCATION_OFFSET = 80;
        public static final int DEFAULT_Y_POSITION = 50;
    }
    
    /** UI Layout constants */
    public static final class UILayout {
        public static final int SPLITPANE_OFFSET = 20;
        public static final int TIME_COLUMN_WIDTH = 62;
        public static final int PID_COLUMN_WIDTH = 40;
        public static final String DEFAULT_TIME_TEXT = "0000";
    }
    
    /** Splash screen constants */
    public static final class SplashScreen {
        public static final int DISPLAY_TIME = 3000; // 3 seconds
        public static final double SPLASH_SCALE_FACTOR = 0.4;
        public static final int FALLBACK_WIDTH = 300;
        public static final int FALLBACK_HEIGHT = 100;
    }
    
    /** Language key prefixes */
    public static final class LangKeys {
        public static final String TASK_PREFIX = "lang.task";
        public static final String PROCESS_PREFIX = "lang.process";
        public static final String EVENTS_PREFIX = "lang.events";
        public static final String PROTOCOL_PREFIX = "lang.protocol";
        public static final String SERVER_PREFIX = "lang.server";
        public static final String CLIENT_PREFIX = "lang.client";
    }
    
    /** Timestamp monitoring defaults */
    public static final long DEFAULT_MONITOR_INTERVAL = 1;
}