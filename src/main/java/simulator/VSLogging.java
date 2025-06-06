package simulator;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JTextArea;

import utils.VSTools;

/**
 * The class VSLogging, an object of this class is responsible for the loging
 * of text messages into the simulator's loging window.
 *
 * @author Paul C. Buetow
 */
public class VSLogging {
    /** The loging area. */
    private JTextArea logingArea;

    /** The filter text. */
    private String filterText;

    /** The pause lines. Used for cacheing the loging if the loging is
     * deactivated for a while
     */
    private ArrayList<StringBuffer> pauseLines;

    /** The loging lines. */
    private ArrayList<StringBuffer> logingLines;

    /** The simulator canvas. */
    private VSSimulatorVisualization simulatorVisualization;

    /** The loging messages are filtered. */
    private boolean isFiltered;

    /** The loging is paused. */
    private boolean isPaused;

    /** The filter pattern. */
    private Pattern filterPattern;

    /**
     * Instantiates a new VSLogging object.
     */
    public VSLogging() {
        logingArea = new JTextArea(0, 0);
        logingArea.setEditable(false);
        logingArea.setLineWrap(true);
        logingArea.setWrapStyleWord(true);
        logingLines = new ArrayList<StringBuffer>();
        pauseLines = new ArrayList<StringBuffer>();
        filterText = "";
    }

    /**
     * Sets the simulator canvas.
     *
     * @param sv the simulator canvas
     */
    public void setSimulatorCanvas(VSSimulatorVisualization sv) {
        this.simulatorVisualization = sv;
    }

    /**
     * Gets the loging area.
     *
     * @return the loging area
     */
    public JTextArea getLoggingArea() {
        return logingArea;
    }

    /**
     * Loggs a message using the global time.
     *
     * @param message the message
     */
    public void log(String message) {
        if (simulatorVisualization == null)
            log(message, 0);
        else
            log(message, simulatorVisualization.getTime());
    }

    /**
     * Loggs a message using the specified time.
     *
     * @param message the message
     * @param time the time
     */
    public synchronized void log(String message, long time) {
        StringBuffer buffer = new StringBuffer();
        buffer.append(VSTools.getTimeString(time));
        buffer.append(": ");
        buffer.append(message);

        if (isPaused)
            pauseLines.add(buffer);
        else
            logFiltered(buffer);
    }

    /**
     * Sets if the loging is paused.
     *
     * @param isPaused true, if the loging is paused
     */
    public synchronized void isPaused(boolean isPaused) {
        this.isPaused = isPaused;

        if (!isPaused) {
            for (StringBuffer buffer : pauseLines)
                logFiltered(buffer);

            pauseLines.clear();
        }
    }

    /**
     * If the loging is filtered, it's using the pattern matching.
     *
     * @param buffer the loging buffer to filter
     */
    private void logFiltered(StringBuffer buffer) {
        logingLines.add(buffer);
        if (!isFiltered) {
            logingArea.append(buffer.toString()+"\n");
            logingArea.setCaretPosition(
                logingArea.getDocument().getLength());

        } else if (filterPattern != null &&
                   filterPattern.matcher(buffer).find()) {
            logingArea.append(buffer.toString()+"\n");
            logingArea.setCaretPosition(
                logingArea.getDocument().getLength());
        }
    }

    /**
     * Checks if the loging is filtered.
     *
     * @param isFiltered true, if the loging is filtered
     */
    public synchronized void isFiltered(boolean isFiltered) {
        this.isFiltered = isFiltered;

        if (!isFiltered)
            setFilterText("");
        else
            filter();
    }

    /**
     * Sets the filter text.
     *
     * @param filterText the new filter text
     */
    public synchronized void setFilterText(String filterText) {
        this.filterText = filterText;
        filter();
    }

    /**
     * Clears the loging.
     */
    public synchronized void clear() {
        logingLines.clear();
        pauseLines.clear();
        logingArea.setText("");
    }

    /**
     * Filters the loging.
     */
    private void filter() {
        try {
            filterPattern = Pattern.compile(filterText);
            StringBuffer buffer = new StringBuffer();

            for (StringBuffer line : logingLines) {
                if (isFiltered) {
                    Matcher matcher = filterPattern.matcher(line);
                    if (matcher.find()) {
                        buffer.append(line);
                        buffer.append("\n");
                    }
                } else {
                    buffer.append(line);
                    buffer.append("\n");
                }
            }
            logingArea.setText(buffer.toString());

        } catch (Exception e) {
            filterPattern = null;
            logingArea.setText("");
        }
    }
}
