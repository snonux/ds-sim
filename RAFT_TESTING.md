# Raft Simulation Testing Guide

## What We Fixed

The Raft simulation wasn't working because of a fundamental design issue with how protocols are activated:

1. **Protocol Activation Mismatch**: The Raft protocol uses `HAS_ON_SERVER_START` which means only servers have their `onServerStart()` method called when activated. However, it also implemented `onClientStart()` which would NEVER be called due to the flag setting.

2. **Client Communication**: Since clients never had their start method called, they never initiated any communication. We fixed this by having clients react to server heartbeats instead.

## Changes Made

1. Modified `VSRaftProtocol.java`:
   - Clients now react to `APPEND_ENTRIES` (heartbeat) messages from servers
   - When a client receives its first heartbeat, it schedules its first request
   - Client state is now tracked with simple instance variables instead of trying to use VSPrefs methods with default values

## How to Test the Raft Simulation

1. **Build the project**:
   ```bash
   mvn clean package
   ```

2. **Create/Update the simulation**:
   ```bash
   java -cp target/ds-sim-1.0.1-SNAPSHOT.jar examples.RaftSimulationBuilder
   ```

3. **Run the simulator GUI**:
   ```bash
   java -jar target/ds-sim-1.0.1-SNAPSHOT.jar
   ```

4. **Load and run the simulation**:
   - Click File → Open
   - Navigate to `saved-simulations/raft-consensus.dat`
   - Click the Play button to start the simulation
   - You should see:
     - Servers (processes 0-1) starting elections
     - One server becoming the leader (highlighted)
     - The leader sending heartbeats to all processes
     - The client (process 2) receiving heartbeats and sending requests
     - Log entries being replicated across servers

## Expected Behavior

1. **Initial State**: All servers start as FOLLOWERS
2. **Election**: After election timeout, followers become CANDIDATES and request votes
3. **Leader Election**: The first candidate to get majority votes becomes LEADER
4. **Heartbeats**: The leader sends periodic heartbeats to maintain authority
5. **Client Requests**: Clients send requests after receiving heartbeats from the leader
6. **Log Replication**: The leader replicates client commands to all followers

## Key Insights

- The simulator uses an event-driven architecture where protocols must be explicitly activated
- Protocols with `HAS_ON_SERVER_START` only trigger `onServerStart()` for servers
- Protocols with `HAS_ON_CLIENT_START` only trigger `onClientStart()` for clients
- Client-server communication often needs to be initiated by one side (usually the side that has the start method called)

## Debugging Tips

If the simulation doesn't work as expected:
1. Check the console output for any error messages
2. Verify that all 3 processes are created (0-1 as servers, 2 as client)
3. Ensure the protocol activations are scheduled at the right times
4. Look for the election timeout messages and leader elections in the logs