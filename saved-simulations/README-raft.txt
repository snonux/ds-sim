RAFT CONSENSUS SIMULATION
========================

This directory contains Raft consensus protocol simulations:

1. raft-working.dat - Full working simulation with:
   - 3 Raft servers (processes 0-2)
   - 2 Raft clients (processes 3-4)
   - Server crash/recovery events

To run the simulation:
1. java -jar target/ds-sim-1.0.1-SNAPSHOT.jar
2. File → Open → saved-simulations/raft-working.dat
3. Click Run (▶) button

What to look for:
- Leader election (REQUEST_VOTE messages)
- Heartbeats from leader (APPEND_ENTRIES)
- Client requests and responses
- Re-election when servers crash

Timeline:
- Time 0: Servers start, begin leader election
- Time 500-700: Clients start
- Time 2000: Server 0 crashes
- Time 3000: Server 0 recovers
- Time 4000: Server 1 crashes
- Time 5000: Server 1 recovers
