package protocols.implementations;

import java.util.ArrayList;
import java.util.Vector;

import core.VSMessage;
import protocols.VSAbstractProtocol;

/**
 * The class VSOnePhaseCommitProtocol, an implementation of the one phase
 * commit protocol.
 *
 * @author Paul C. Buetow
 */
public class VSOnePhaseCommitProtocol extends VSAbstractProtocol {
    /* Server variables */
    private boolean ackSent;

    /**
     * Instantiates a one phase commit protocol.
     */
    public VSOnePhaseCommitProtocol() {
        super(VSAbstractProtocol.HAS_ON_SERVER_START);
        setClassname(getClass().toString());
    }

    /* (non-Javadoc)
     * @see events.VSAbstractProtocol#onServerInit()
     */
    public void onServerInit() {
        /* Can be changed via GUI variables editor of each process */
        Vector<Integer> vec = new Vector<Integer>();
        vec.add(1);
        vec.add(3);

        // TODO: Translate
        initVector("pids", vec, "PIDs beteiligter Prozesse");
        initLong("timeout", 2500, "Zeit bis erneute Anfrage", "ms");
    }

    /* (non-Javadoc)
     * @see protocols.VSAbstractProtocol#onServerReset()
     */
    public void onServerReset() {
        if (pids != null) {
            pids.clear();
            pids.addAll(getVector("pids"));
        }
    }

    /* (non-Javadoc)
     * @see protocols.VSAbstractProtocol#onServerStart()
     */
    public void onServerStart() {
        if (pids == null) {
            pids = new ArrayList<Integer>();
            pids.addAll(getVector("pids"));
        }

        if (pids.size() != 0) {
            long timeout = getLong("timeout") + process.getTime();
            /* Will run onServerSchedule() at the specified local time */
            scheduleAt(timeout);

            VSMessage message = new VSMessage();
            message.setBoolean("wantAck", true);
            sendMessage(message);
        }
    }

    /* (non-Javadoc)
     * @see protocols.VSAbstractProtocol#onServerRecv(core.VSMessage)
     */
    public void onServerRecv(VSMessage recvMessage) {
        if (pids.size() == 0)
            return;

        if (recvMessage.getBoolean("isAck")) {
            Integer pid = recvMessage.getIntegerObj("pid");
            if (pids.contains(pid))
                pids.remove(pid);
            else
                return;

            log("ACK from process " + pid + " received!");

            if (pids.size() == 0) {
                log("ACKs received from all participating processes! " +
                    "Committed!");

                /* Remove the active schedule which has been created in the
                   onServerStart method */
                removeSchedules();
            }
        }
    }

    /* (non-Javadoc)
     * @see protocols.VSAbstractProtocol#onServerSchedule()
     */
    public void onServerSchedule() {
        onServerStart();
    }

    /* Client variables, coordinator */
    private ArrayList<Integer> pids;

    /* (non-Javadoc)
     * @see events.VSAbstractProtocol#onClientInit()
     */
    public void onClientInit() {
    }

    /* (non-Javadoc)
     * @see protocols.VSAbstractProtocol#onClientReset()
     */
    public void onClientReset() {
        ackSent = false;
    }

    /* (non-Javadoc)
     * @see protocols.VSAbstractProtocol#onClientRecv(core.VSMessage)
     */
    public void onClientRecv(VSMessage recvMessage) {
        if (ackSent)
            return;

        VSMessage message = new VSMessage();
        message.setBoolean("isAck", true);
        message.setInteger("pid", process.getProcessID());
        sendMessage(message);
        ackSent = true;
        log("Committed");
    }

    /* (non-Javadoc)
     * @see protocols.VSAbstractProtocol#onClientSchedule()
     */
    public void onClientSchedule() {
    }

    /* (non-Javadoc)
     * @see protocols.VSAbstractProtocol#toString()
     */
    public String toString() {
        return super.toString();
    }
}
