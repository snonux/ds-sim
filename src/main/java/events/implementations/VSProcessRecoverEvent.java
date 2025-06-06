package events.implementations;

import events.VSAbstractEvent;
import events.VSCopyableEvent;
import simulator.VSMain;

/**
 * The class VSProcessRecoverEvent. This event makes a process to recover if
 * it is crashed.
 *
 * @author Paul C. Buetow
 */
public class VSProcessRecoverEvent extends VSAbstractEvent
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
        return VSMain.prefs.getString("lang.process.recover");
    }

    /* (non-Javadoc)
     * @see events.VSAbstractEvent#onStart()
     */
    public void onStart() {
        if (process.isCrashed()) {
            process.isCrashed(false);
            log(prefs.getString("lang.recovered"));
        }
    }
}
