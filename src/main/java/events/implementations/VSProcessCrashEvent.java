package events.implementations;

import events.VSAbstractEvent;
import events.VSCopyableEvent;
import simulator.VSMain;

/**
 * The class VSProcessCrashEvent. This event makes a process to crash.
 *
 * @author Paul C. Buetow
 */
public class VSProcessCrashEvent extends VSAbstractEvent
    implements VSCopyableEvent {
    /* (non-Javadoc)
     * @see events.VSCopyableEvent#initCopy(events.VSAbstractEvent)
     */
    public void initCopy(VSAbstractEvent copy) {
    }

    /* (non-Javadoc)
     * @see events.VSAbstractEvent#onInit()
     */
    public void onInit() {
        setClassname(getClass().toString());
    }

    /* (non-Javadoc)
     * @see events.VSAbstractEvent#createShortname()()
     */
    protected String createShortname(String savedShortname) {
        return VSMain.prefs.getString("lang.process.crash");
    }

    /* (non-Javadoc)
     * @see events.VSAbstractEvent#onStart()
     */
    public void onStart() {
        if (!process.isCrashed()) {
            process.isCrashed(true);
            log(prefs.getString("lang.crashed"));
        }
    }
}
