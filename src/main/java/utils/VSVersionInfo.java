package utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Provides version information for the application.
 * This class reads version details from a properties file that is
 * populated at build time by Maven resource filtering.
 * 
 * @author Paul C. Buetow
 */
public final class VSVersionInfo {
    
    private static final String VERSION_FILE = "/version.properties";
    private static final Properties versionProps = new Properties();
    private static boolean loaded = false;
    
    /** Private constructor to prevent instantiation */
    private VSVersionInfo() {}
    
    /**
     * Loads version properties from the resource file.
     * This method is called automatically on first access.
     */
    private static synchronized void loadProperties() {
        if (loaded) {
            return;
        }
        
        try (InputStream is = VSVersionInfo.class.getResourceAsStream(VERSION_FILE)) {
            if (is != null) {
                versionProps.load(is);
                loaded = true;
            } else {
                // Fallback values if properties file not found
                versionProps.setProperty("app.version", "Unknown");
                versionProps.setProperty("app.name", "DS-Sim");
                versionProps.setProperty("app.description", "Distributed Systems Simulator");
                loaded = true;
            }
        } catch (IOException e) {
            // Fallback values on error
            versionProps.setProperty("app.version", "Unknown");
            versionProps.setProperty("app.name", "DS-Sim");
            versionProps.setProperty("app.description", "Distributed Systems Simulator");
            loaded = true;
        }
    }
    
    /**
     * Gets the application version from Maven project version.
     * 
     * @return the version string (e.g., "1.0.1-SNAPSHOT")
     */
    public static String getVersion() {
        if (!loaded) {
            loadProperties();
        }
        return versionProps.getProperty("app.version", "Unknown");
    }
    
    /**
     * Gets the application name from Maven project name.
     * 
     * @return the application name
     */
    public static String getName() {
        if (!loaded) {
            loadProperties();
        }
        return versionProps.getProperty("app.name", "DS-Sim");
    }
    
    /**
     * Gets the application description from Maven project description.
     * 
     * @return the application description
     */
    public static String getDescription() {
        if (!loaded) {
            loadProperties();
        }
        return versionProps.getProperty("app.description", "Distributed Systems Simulator");
    }
    
    /**
     * Gets the build timestamp.
     * 
     * @return the build timestamp or "Unknown" if not available
     */
    public static String getBuildTimestamp() {
        if (!loaded) {
            loadProperties();
        }
        return versionProps.getProperty("build.timestamp", "Unknown");
    }
    
    /**
     * Gets the full version string including name and version.
     * 
     * @return formatted version string (e.g., "Distributed Systems Simulator 1.0.1-SNAPSHOT")
     */
    public static String getFullVersionString() {
        return String.format("Distributed Systems Simulator %s", getVersion());
    }
}