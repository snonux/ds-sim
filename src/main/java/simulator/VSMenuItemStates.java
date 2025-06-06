package simulator;

/**
 * The class VSMenuItemStates. Used by the VSSimulator to update the
 * "simulator" bar of the VSSimulatorFrame.
 *
 * @author Paul C. Buetow
 */
public class VSMenuItemStates {
    /** The pause state. */
    private volatile boolean pause;

    /** The replay state. */
    private volatile boolean replay;

    /** The reset state. */
    private volatile boolean reset;

    /** The start state. */
    private volatile boolean start;

    /**
     * Instantiates a new VSMenuItemStates object.
     *
     * @param pause the pause state
     * @param replay the replay state
     * @param reset the reset state
     * @param start the start state
     */
    public VSMenuItemStates(boolean pause, boolean replay, boolean reset,
                            boolean start) {
        this.pause = pause;
        this.replay = replay;
        this.reset = reset;
        this.start = start;
    }

    /**
     * Sets the pause state.
     *
     * @param pause the new pause state
     */
    public void setPause(boolean pause) {
        this.pause = pause;
    }

    /**
     * Sets the replay state.
     *
     * @param replay the new replay state
     */
    public void setReplay(boolean replay) {
        this.replay = replay;
    }

    /**
     * Sets the reset state.
     *
     * @param reset the new reset state
     */
    public void setReset(boolean reset) {
        this.reset = reset;
    }

    /**
     * Sets the start state.
     *
     * @param start the new start state
     */
    public void setStart(boolean start) {
        this.start = start;
    }

    /**
     * Gets the pause state.
     *
     * @return the pause state
     */
    public boolean getPause() {
        return pause;
    }

    /**
     * Gets the replay state.
     *
     * @return the replay state
     */
    public boolean getReplay() {
        return replay;
    }

    /**
     * Gets the reset state.
     *
     * @return the reset state
     */
    public boolean getReset() {
        return reset;
    }

    /**
     * Gets the start state.
     *
     * @return the start state
     */
    public boolean getStart() {
        return start;
    }
}
