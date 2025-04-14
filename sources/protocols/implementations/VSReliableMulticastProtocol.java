package protocols.implementations;

import java.util.ArrayList;
import java.util.Vector;

import core.VSMessage;
import protocols.VSAbstractProtocol;

/**
 * The class VSReliableMulticastProtocol, an implementation of the reliable
 * multicast protocol.
 *
 * @author Paul C. Buetow
 */
public class VSReliableMulticastProtocol extends VSAbstractProtocol {
    /**
     * Instantiates a two phase commit protocol object.
     */
    public VSReliableMulticastProtocol() {
        super(VSAbstractProtocol.HAS_ON_CLIENT_START);
        setClassname(getClass().toString());
    }

    /** PIDs of all processes which still have to send an ACK */
    private ArrayList<Integer> pids;

    /* (non-Javadoc)
     * @see events.VSAbstractProtocol#onClientInit()
     */
    public void onClientInit() {
        Vector<Integer> vec = new Vector<Integer>();
        vec.add(1);
        vec.add(3);

        super.initVector("pids", vec, "PIDs beteiligter Prozesse");
        super.initLong("timeout", 2500, "Zeit bis erneute Anfrage", "ms");
    }

    /* (non-Javadoc)
     * @see protocols.VSAbstractProtocol#onClientReset()
     */
    public void onClientReset() {
        if (pids != null) {
            pids.clear();
            pids.addAll(getVector("pids"));
        }
    }

    /* (non-Javadoc)
     * @see protocols.VSAbstractProtocol#onClientStart()
     */
    public void onClientStart() {
        if (pids == null) {
            pids = new ArrayList<Integer>();
            pids.addAll(getVector("pids"));
        }

        if (pids.size() != 0) {
            long timeout = getLong("timeout") + process.getTime();
            /* Will run onClientSchedule() at the specified local time */
            scheduleAt(timeout);

            VSMessage message = new VSMessage();
            message.setBoolean("isMulticast", true);
            sendMessage(message);
        }
    }

    /* (non-Javadoc)
     * @see protocols.VSAbstractProtocol#onClientRecv(core.VSMessage)
     */
    public void onClientRecv(VSMessage recvMessage) {
        if (pids.size() != 0 && recvMessage.getBoolean("isAck")) {
            Integer pid = recvMessage.getIntegerObj("pid");

            if (pids.contains(pid))
                pids.remove(pid);
            else
                return;

            log("ACK from process " + pid + " received!");


            if (pids.size() == 0) {
                log("ACKs from all involved processes received!");

                /* Remove the active schedule which has been created in the
                   onClientStart method */
                removeSchedules();
            }
        }
    }

    /* (non-Javadoc)
     * @see protocols.VSAbstractProtocol#onClientSchedule()
     */
    public void onClientSchedule() {
        onClientStart();
    }

    /** True if ACK has been sent already */
    private boolean ackSent;

    /* (non-Javadoc)
     * @see events.VSAbstractProtocol#onServerInit()
     */
    public void onServerInit() {
    }

    /* (non-Javadoc)
     * @see protocols.VSAbstractProtocol#onServerReset()
     */
    public void onServerReset() {
        ackSent = false;
    }

    /* (non-Javadoc)
     * @see protocols.VSAbstractProtocol#onServerRecv(core.VSMessage)
     */
    public void onServerRecv(VSMessage recvMessage) {
        if (recvMessage.getBoolean("isMulticast")) {
            VSMessage message = new VSMessage();
            message.setBoolean("isAck", true);
            message.setInteger("pid", process.getProcessID());
            sendMessage(message);

            if (ackSent) {
                log("ACK sent again");

            } else {
                log("ACK sent");

                ackSent = true;
            }
        }
    }

    /* (non-Javadoc)
     * @see protocols.VSAbstractProtocol#onServerSchedule()
     */
    public void onServerSchedule() {
    }
}
