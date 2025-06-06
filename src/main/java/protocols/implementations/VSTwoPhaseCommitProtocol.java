package protocols.implementations;

import java.util.ArrayList;
import java.util.Vector;

import core.VSMessage;
import protocols.VSAbstractProtocol;

/**
 * The class VSTwoPhaseCommitProtocol, an implementation of the two phase
 * commit protocol.
 *
 * @author Paul C. Buetow
 */
public class VSTwoPhaseCommitProtocol extends VSAbstractProtocol {
    /**
     * Instantiates a two phase commit protocol object.
     */
    public VSTwoPhaseCommitProtocol() {
        super(VSAbstractProtocol.HAS_ON_SERVER_START);
        setClassname(getClass().toString());
    }

    /** PIDs of all processes which still have to vote */
    private ArrayList<Integer> votePids;

    /** PIDs of all processes which have to acknowledge that they recv the
     * global vote result
     */
    private ArrayList<Integer> ackPids;

    /** The gloal vote result */
    private boolean voteResult;

    /* (non-Javadoc)
     * @see events.VSAbstractProtocol#onServerInit()
     */
    public void onServerInit() {
        Vector<Integer> vec = new Vector<Integer>();
        vec.add(2);
        vec.add(3);

        initVector("pids", vec, "PIDs beteiligter Prozesse");
        initLong("timeout", 2500, "Zeit bis erneute Anfrage", "ms");
    }

    /* (non-Javadoc)
     * @see protocols.VSAbstractProtocol#onServerReset()
     */
    public void onServerReset() {
        if (votePids != null) {
            voteResult = true;
            votePids.clear();
            votePids.addAll(getVector("pids"));
            ackPids.clear();
            ackPids.addAll(getVector("pids"));
        }
    }

    /* (non-Javadoc)
     * @see protocols.VSAbstractProtocol#onServerStart()
     */
    public void onServerStart() {
        if (votePids == null) {
            voteResult = true;
            votePids = new ArrayList<Integer>();
            votePids.addAll(getVector("pids"));
            ackPids = new ArrayList<Integer>();
            ackPids.addAll(getVector("pids"));
        }

        if (votePids.size() != 0) {
            long timeout = getLong("timeout") + process.getTime();
            /* Will run onServerSchedule() at the specified local time */
            scheduleAt(timeout);

            VSMessage message = new VSMessage();
            message.setBoolean("wantVote", true);
            sendMessage(message);

        } else if (ackPids.size() != 0) {
            long timeout = getLong("timeout") + process.getTime();
            /* Will run onServerSchedule() at the specified local time */
            scheduleAt(timeout);

            VSMessage message = new VSMessage();
            message.setBoolean("isVoteResult", true);
            message.setBoolean("voteResult", voteResult);
            sendMessage(message);
        }
    }

    /* (non-Javadoc)
     * @see protocols.VSAbstractProtocol#onServerRecv(core.VSMessage)
     */
    public void onServerRecv(VSMessage recvMessage) {
        if (votePids.size() != 0 && recvMessage.getBoolean("isVote")) {
            Integer pid = recvMessage.getIntegerObj("pid");
            if (votePids.contains(pid))
                votePids.remove(pid);
            else
                return;

            boolean vote = recvMessage.getBoolean("vote");
            log("Vote from process " + pid + " received! Result: " + vote);

            if (!vote)
                voteResult = false;

            if (votePids.size() == 0) {
                log("Votes from all involved processes received! Global result: " + voteResult);

                /* Remove the active schedule which has been created in the
                   onServerStart method */
                removeSchedules();
                /* Create a new schedule and send the vote result */
                onServerStart();
            }

        } else if (ackPids.size() != 0 && recvMessage.getBoolean("isAck")) {
            Integer pid = recvMessage.getIntegerObj("pid");
            if (ackPids.contains(pid))
                ackPids.remove(pid);
            else
                return;

            if (ackPids.size() == 0) {
                /* Remove the active schedule which has been created in the
                   onServerStart method */
                removeSchedules();
                log("All participants have received the vote");
            }
        }
    }

    /* (non-Javadoc)
     * @see protocols.VSAbstractProtocol#onServerSchedule()
     */
    public void onServerSchedule() {
        onServerStart();
    }

    /* Server variables */
    private boolean voteSent;
    private boolean myVote;

    /* (non-Javadoc)
     * @see events.VSAbstractProtocol#onClientInit()
     */
    public void onClientInit() {
        initInteger("ackProb", 50, "Festschreibw'keit", 0, 100, "%");
    }

    /* (non-Javadoc)
     * @see protocols.VSAbstractProtocol#onClientReset()
     */
    public void onClientReset() {
        voteSent = false;
    }

    /* (non-Javadoc)
     * @see protocols.VSAbstractProtocol#onClientRecv(core.VSMessage)
     */
    public void onClientRecv(VSMessage recvMessage) {
        if (recvMessage.getBoolean("wantVote")) {
            if (!voteSent) {
                voteSent = true;
                myVote = process.getRandomPercentage() <= getInteger("ackProb");
            }

            VSMessage message = new VSMessage();
            message.setBoolean("isVote", true);
            message.setBoolean("vote", myVote);
            message.setInteger("pid", process.getProcessID());
            sendMessage(message);

            log("Vote " + myVote + " sent");


        } else if (recvMessage.getBoolean("isVoteResult")) {
            boolean voteResult = recvMessage.getBoolean("voteResult");
            log("Global voting result received. Result: " + voteResult);

            VSMessage message = new VSMessage();
            message.setBoolean("isAck", true);
            message.setInteger("pid", process.getProcessID());
            sendMessage(message);
        }
    }

    /* (non-Javadoc)
     * @see protocols.VSAbstractProtocol#onClientSchedule()
     */
    public void onClientSchedule() {
    }
}
