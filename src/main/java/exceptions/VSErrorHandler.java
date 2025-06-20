package exceptions;

import javax.swing.JOptionPane;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Centralized error handling utility for the DS-Sim application.
 * Provides consistent error logging and user notification.
 *
 * @author Paul C. Buetow
 */
public final class VSErrorHandler {
    
    private static final Logger LOGGER = Logger.getLogger(VSErrorHandler.class.getName());
    
    /** Private constructor to prevent instantiation */
    private VSErrorHandler() {}
    
    /**
     * Handle an exception with logging and optional user notification.
     *
     * @param exception the exception to handle
     * @param showDialog whether to show a dialog to the user
     */
    public static void handle(Exception exception, boolean showDialog) {
        // Log the exception
        LOGGER.log(Level.SEVERE, exception.getMessage(), exception);
        
        // Show dialog if requested
        if (showDialog) {
            showErrorDialog(exception);
        }
    }
    
    /**
     * Handle an exception with logging only (no user notification).
     *
     * @param exception the exception to handle
     */
    public static void handle(Exception exception) {
        handle(exception, false);
    }
    
    /**
     * Handle an exception with a custom message.
     *
     * @param message custom message to log and display
     * @param exception the exception to handle
     * @param showDialog whether to show a dialog to the user
     */
    public static void handle(String message, Exception exception, boolean showDialog) {
        // Log with custom message
        LOGGER.log(Level.SEVERE, message, exception);
        
        // Show dialog if requested
        if (showDialog) {
            showErrorDialog(message, exception);
        }
    }
    
    /**
     * Log a warning without throwing an exception.
     *
     * @param message the warning message
     */
    public static void warning(String message) {
        LOGGER.log(Level.WARNING, message);
    }
    
    /**
     * Log a warning with an associated exception.
     *
     * @param message the warning message
     * @param exception the associated exception
     */
    public static void warning(String message, Exception exception) {
        LOGGER.log(Level.WARNING, message, exception);
    }
    
    /**
     * Show an error dialog to the user.
     *
     * @param exception the exception to display
     */
    private static void showErrorDialog(Exception exception) {
        String title = "Simulator Error";
        String message = exception.getMessage();
        
        if (exception instanceof VSSimulatorException) {
            // Use more specific title for our exceptions
            if (exception instanceof VSConfigurationException) {
                title = "Configuration Error";
            } else if (exception instanceof VSProcessException) {
                title = "Process Error";
            } else if (exception instanceof VSProtocolException) {
                title = "Protocol Error";
            } else if (exception instanceof VSSerializationException) {
                title = "Save/Load Error";
            }
        }
        
        if (message == null || message.isEmpty()) {
            message = "An unexpected error occurred: " + exception.getClass().getSimpleName();
        }
        
        JOptionPane.showMessageDialog(null, message, title, JOptionPane.ERROR_MESSAGE);
    }
    
    /**
     * Show an error dialog with a custom message.
     *
     * @param message the custom message
     * @param exception the exception that occurred
     */
    private static void showErrorDialog(String message, Exception exception) {
        String details = exception.getMessage();
        if (details != null && !details.isEmpty()) {
            message = message + "\n\nDetails: " + details;
        }
        
        JOptionPane.showMessageDialog(null, message, "Simulator Error", JOptionPane.ERROR_MESSAGE);
    }
    
    /**
     * Convert a generic exception to a simulator exception if possible.
     *
     * @param e the exception to convert
     * @param context additional context about where the error occurred
     * @return a VSSimulatorException wrapping the original exception
     */
    public static VSSimulatorException wrap(Exception e, String context) {
        if (e instanceof VSSimulatorException) {
            return (VSSimulatorException) e;
        }
        
        // Wrap common exceptions with more specific types
        if (e instanceof java.io.IOException) {
            return new VSSerializationException(context, e);
        } else if (e instanceof NumberFormatException) {
            return new VSConfigurationException("Invalid number format in " + context, e);
        } else if (e instanceof NullPointerException) {
            return new VSSimulatorException("Null value encountered in " + context, e);
        } else {
            return new VSSimulatorException(context + ": " + e.getMessage(), e);
        }
    }
}