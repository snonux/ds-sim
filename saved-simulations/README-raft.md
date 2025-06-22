# Raft Consensus Simulation

## Current Status

The `raft.dat` file exists but is currently a copy of `ping-pong.dat` and needs to be properly configured with Raft protocol events through the GUI.

## Why Manual Configuration is Required

The simulation files use Java's native object serialization format which includes:
- Complex object graphs with circular references
- Private field serialization requiring specific class versions
- GUI-dependent initialization sequences
- Protocol activation through VSProtocolEvent objects

Programmatic creation attempts failed because:
1. VSSerialize methods require GUI components
2. VSSimulatorVisualization has private methods for process creation
3. The serialization format includes UI state and preferences

## How to Create a Working Raft Simulation

### Step 1: Start DS-Sim
```bash
java -jar target/ds-sim-1.0.1-SNAPSHOT.jar
```

### Step 2: Create New Simulation
- File → New (or Ctrl+N)
- This creates a blank simulation

### Step 3: Add Processes
- Click "Add Process" button 3 times
- This creates a 3-node Raft cluster

### Step 4: Configure Each Process as Raft Server
For each process (Process 1, 2, and 3):
- Right-click on the process
- Select "Protocols" → "Raft Consensus Algorithm" → "Server"
- You'll see a protocol activation event added to the task list

### Step 5: Set Simulation Duration
- Edit → Preferences → Simulator
- Set "Simulation duration" to 15000 (15 seconds)
- Click OK

### Step 6: Save the Simulation
- File → Save As
- Navigate to `saved-simulations/`
- Save as `raft.dat`

### Step 7: Run the Simulation
- Click the Play button (▶)
- Watch the leader election process

## Expected Behavior

### Time 0-300ms: Initial State
- All nodes start as FOLLOWERS
- Each sets a random election timeout (150-300ms)
- Status: "FOLLOWER" shown in logs

### Time 150-500ms: Election Phase
- First node to timeout transitions to CANDIDATE
- Increments term to 1
- Sends REQUEST_VOTE messages to all other nodes
- Other nodes respond with VOTE_RESPONSE messages

### Time 300-600ms: Leader Establishment
- Candidate receiving majority votes becomes LEADER
- Leader node is highlighted in the visualization
- Begins sending APPEND_ENTRIES (heartbeat) messages

### Time 600ms+: Steady State
- Leader sends heartbeats every 50ms
- Followers acknowledge with APPEND_RESPONSE
- If leader fails, new election begins after timeout

## Verification

### GUI Verification
1. Run the simulation and observe:
   - REQUEST_VOTE messages during election
   - One node becoming highlighted (leader)
   - Regular APPEND_ENTRIES messages from leader

### Headless Verification
```bash
java -cp target/classes:target/test-classes \
     -Djava.awt.headless=true \
     -Dds.sim.verbose=true \
     testing.HeadlessProtocolRunner saved-simulations/raft.dat
```

Look for these log messages:
- `[FOLLOWER T:0 N:X] Raft node initialized as FOLLOWER`
- `[CANDIDATE T:1 N:X] Starting election for term 1`
- `[LEADER T:1 N:X] Elected as leader with Y votes`

## Implementation Details

The Raft protocol implementation (`VSRaftProtocol.java`) includes:

- **State Machine**: FOLLOWER → CANDIDATE → LEADER transitions
- **Election Timeout**: Random 150-300ms to prevent split votes
- **Heartbeat Interval**: 50ms from leader to maintain authority
- **Term Management**: Monotonically increasing terms for safety
- **Vote Tracking**: Majority (n/2 + 1) required for leadership
- **Message Types**:
  - REQUEST_VOTE: Candidate requests votes
  - VOTE_RESPONSE: Follower grants/denies vote
  - APPEND_ENTRIES: Leader heartbeat/log replication
  - APPEND_RESPONSE: Follower acknowledgment

## Troubleshooting

If the simulation shows no Raft activity:
1. Verify all processes have Raft protocol events in task list
2. Check that events are scheduled at time 0
3. Ensure simulation duration is > 5 seconds
4. Confirm VSRaftProtocol has `setClassname()` in constructor

If you see PingPong messages instead of Raft:
- The file wasn't properly recreated
- Delete raft.dat and create from scratch via GUI

## Scripts

- `scripts/create-raft-simulation.sh` - Creates template and instructions
- `scripts/analyze-raft-simulation.sh` - Diagnoses simulation issues