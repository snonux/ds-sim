# Creating a Raft Consensus Simulation

This guide explains how to create a working Raft consensus simulation in DS-Sim.

## Overview

The Raft protocol implementation in DS-Sim demonstrates:
- Leader election with randomized timeouts
- Heartbeat messages from leader to followers
- Vote requests and responses
- Term management
- Log replication (basic implementation)

## Creating the Simulation via GUI

1. **Start DS-Sim**:
   ```bash
   java -jar target/ds-sim-1.0.1-SNAPSHOT.jar
   ```

2. **Add Processes**:
   - Click "Add Process" button 3 times to create 3 nodes
   - This creates the minimum cluster size for consensus

3. **Configure Each Process as Raft Server**:
   - Right-click on Process 1
   - Select "Protocols" → "Raft Consensus Algorithm" → "Server"
   - Repeat for Process 2 and Process 3

4. **Set Simulation Duration**:
   - Go to Edit → Preferences → Simulator
   - Set "Simulation duration" to 15 seconds
   - This gives enough time to see leader election

5. **Save the Simulation**:
   - File → Save As
   - Save as `saved-simulations/raft.dat`

6. **Run the Simulation**:
   - Click the "Play" button
   - Watch the message exchanges and leader election

## Expected Behavior

When you run the simulation:

1. **Initial State** (0-300ms):
   - All nodes start as FOLLOWERS
   - Each sets a random election timeout (150-300ms)

2. **Election Phase** (150-500ms):
   - First node to timeout becomes CANDIDATE
   - Sends REQUEST_VOTE messages to all nodes
   - Other nodes respond with VOTE_RESPONSE

3. **Leader Establishment** (300-600ms):
   - Candidate with majority votes becomes LEADER
   - Leader is highlighted in the visualization
   - Starts sending APPEND_ENTRIES (heartbeats)

4. **Steady State** (600ms+):
   - Leader sends periodic heartbeats (every 50ms)
   - Followers reset election timeout on heartbeat
   - System remains stable with one leader

## Testing the Simulation

Run the simulation in headless mode:
```bash
java -cp target/classes:target/test-classes \
  -Djava.awt.headless=true \
  -Dds.sim.verbose=true \
  testing.HeadlessProtocolRunner saved-simulations/raft.dat
```

Expected output includes:
- "[FOLLOWER T:0 N:X] Raft node initialized as FOLLOWER"
- "[CANDIDATE T:1 N:X] Starting election for term 1"
- "Sending vote request to all nodes"
- "[FOLLOWER T:1 N:Y] Granted vote to node X for term 1"
- "[LEADER T:1 N:X] Elected as leader with Y votes"
- "Sending heartbeats to all followers"

## Troubleshooting

If leader election doesn't occur:
- Ensure all processes are configured as "Server" not "Client"
- Check that simulation duration is long enough (>5 seconds)
- Verify VSRaftProtocol has `setClassname()` in constructor

## Implementation Notes

The Raft protocol uses:
- `onServerStart()`: Initializes election timeout
- `onServerSchedule()`: Handles timeouts and periodic tasks
- `scheduleAt()`: Schedules future events
- `sendMessage()`: Broadcasts to all other nodes

See `VSRaftProtocol.java` for full implementation details.