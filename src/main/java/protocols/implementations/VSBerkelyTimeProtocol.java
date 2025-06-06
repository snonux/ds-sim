package protocols.implementations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

import core.VSMessage;
import protocols.VSAbstractProtocol;

/**
 * The class VSBerkelyTimeProtocol, an implementation of the berkely time
 * protocol.
 *
 * @author Paul C. Buetow
 */
public class VSBerkelyTimeProtocol extends VSAbstractProtocol {
    /**
     * Instantiates a new berkely time protocol.
     */
    public VSBerkelyTimeProtocol() {
        super(VSAbstractProtocol.HAS_ON_SERVER_START);
        setClassname(getClass().toString());
    }

    /** Integer: Process ID, Long: Local time of the process */
    private HashMap<Integer,Long> processTimes = new HashMap<Integer,Long>();

    /** Integer: Process ID, Long: Time of receiving the response from the
     * process
     */
    private HashMap<Integer,Long> recvTimes = new HashMap<Integer,Long>();

    /** Integer: Process ID, Long: Calculated process times (using the RTT) */
    private HashMap<Integer,Long> realTimesRTT = new HashMap<Integer,Long>();

    /** Contains all process IDs of processes which want to justify their
     * time
     */
    private ArrayList<Integer> peers = new ArrayList<Integer>();

    /** Time the request/response has started */
    private long requestTime;

    /* (non-Javadoc)
     * @see events.VSAbstractProtocol#onServerInit()
     */
    public void onServerInit() {
        Vector<Integer> vec = new Vector<Integer>();
        vec.add(1);
        vec.add(3);
        initVector("pids", vec, "PIDs of participating processes");
    }

    /* (non-Javadoc)
     * @see protocols.VSAbstractProtocol#onServerReset()
     */
    public void onServerReset() {
        //System.out.println("FOOBAR");
        processTimes.clear();
        recvTimes.clear();
        realTimesRTT.clear();
        peers.clear();
    }

    /* (non-Javadoc)
     * @see protocols.VSAbstractProtocol#onServerStart()
     */
    public void onServerStart() {
        //System.out.println("FOO");
        peers.addAll(getVector("pids"));
        requestTime = process.getTime();
        VSMessage message = new VSMessage();
        message.setBoolean("isRequest", true);
        sendMessage(message);
    }

    /* (non-Javadoc)
     * @see protocols.VSAbstractProtocol#onServerRecv(core.VSMessage)
     */
    public void onServerRecv(VSMessage recvMessage) {
        /* Ignore all protocol messages which are not a response message,
           e.g. itself */
        if (!recvMessage.getBoolean("isResponse"))
            return;

        Integer processID = recvMessage.getIntegerObj("processID");

        if (peers.contains(processID))
            peers.remove(processID);
        else
            return; /* Process has been handled already or is not listed */

        Long time = Long.valueOf(recvMessage.getLong("time"));

        processTimes.put(processID, time);
        recvTimes.put(processID, Long.valueOf(process.getTime()));

        /* All peers have told their times */
        if (peers.size() == 0) {
            long avgTime = calculateAverageTime();
            /* Set the local's process time to the new avg reference time */
            process.setTime(avgTime);
            /* Tell all other processes what to do in order to justify their
               times */
            sendJustifyRequests(avgTime);
            /* Start "clean" next time */
            onServerReset();
        }
    }

    /* (non-Javadoc)
     * @see protocols.VSAbstractProtocol#onServerSchedule()
     */
    public void onServerSchedule() {
    }

    /**
     * Calculate the new average time.
     *
     * @return the long
     */
    private long calculateAverageTime() {
        long sum = 0;
        for (Integer processID : processTimes.keySet()) {
            Long localTime = processTimes.get(processID);
            Long recvTime = recvTimes.get(processID);
            long rtt = recvTime.longValue() - requestTime;
            long realProcessTime = localTime + (long) (rtt / 2);
            realTimesRTT.put(processID, Long.valueOf(realProcessTime));
            sum += realProcessTime;
        }
        /* Include the time of the local process */
        sum += process.getTime();
        return (long) sum / (getVector("pids").size() + 1);
    }

    /**
     * Sends to all clients a value to justify their local clocks.
     *
     * @param avgTime the avg time
     */
    private void sendJustifyRequests(long avgTime) {
        for (Integer processID : processTimes.keySet()) {
            long realProcessTime = realTimesRTT.get(processID).longValue();
            long diff = avgTime - realProcessTime;
            VSMessage message = new VSMessage();
            message.setBoolean("isJustify", true);
            message.setLong("timeDiff", diff);
            message.setInteger("receiverProcessID", processID);
            sendMessage(message);
        }
    }

    /* (non-Javadoc)
     * @see events.VSAbstractProtocol#onClientInit()
     */
    public void onClientInit() {
    }

    /* (non-Javadoc)
     * @see protocols.VSAbstractProtocol#onClientReset()
     */
    public void onClientReset() {
    }

    /* (non-Javadoc)
     * @see protocols.VSAbstractProtocol#onClientRecv(core.VSMessage)
     */
    public void onClientRecv(VSMessage recvMessage) {
        if (recvMessage.getBoolean("isRequest")) {
            VSMessage message = new VSMessage();
            message.setInteger("processID", process.getProcessID());
            message.setLong("time", process.getTime());
            message.setBoolean("isResponse", true);
            sendMessage(message);

        } else if (recvMessage.getBoolean("isJustify")) {
            /* Check if it's "my" justify message */
            if (recvMessage.getInteger("receiverProcessID") !=
                    process.getProcessID())
                return;

            long timeDiff = recvMessage.getLong("timeDiff");
            //long recvTime = process.getTime();
            long newTime = process.getTime() + timeDiff;
            log("New time: " + newTime);

            process.setTime(newTime);
        }
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
