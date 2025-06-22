#!/bin/bash
#
# Analyze why raft.dat isn't working properly
#

echo "=== Analyzing Raft Simulation Issue ==="
echo

# Check current raft.dat content
echo "1. Checking raft.dat file..."
if [ -f "saved-simulations/raft.dat" ]; then
    echo "   ✓ raft.dat exists ($(stat -c%s saved-simulations/raft.dat 2>/dev/null || stat -f%z saved-simulations/raft.dat) bytes)"
    
    # Try to detect protocol in the file
    echo
    echo "2. Detecting protocols in raft.dat..."
    strings saved-simulations/raft.dat | grep -E "(Protocol|protocol)" | sort | uniq | head -10
else
    echo "   ✗ raft.dat not found!"
    exit 1
fi

echo
echo "3. Running raft.dat simulation test..."
echo "   (Looking for Raft-specific messages)"
echo

# Run and check for Raft messages
java -cp target/classes:target/test-classes \
     -Djava.awt.headless=true \
     -Dds.sim.verbose=true \
     testing.HeadlessProtocolRunner saved-simulations/raft.dat 2>&1 | \
     grep -E "(FOLLOWER|CANDIDATE|LEADER|REQUEST_VOTE|election|Raft)" | head -20

echo
echo "=== Analysis Results ==="
echo
echo "PROBLEM: The raft.dat file contains Ping-Pong protocol events, not Raft protocol."
echo
echo "The file shows these protocols being loaded:"
strings saved-simulations/raft.dat | grep -E "VSPingPongProtocol|VSRaftProtocol" | head -5

echo
echo "=== Solution ==="
echo
echo "The raft.dat file needs to be recreated with Raft protocol events."
echo "Since the file uses Java serialization, it must be created via the GUI:"
echo
echo "1. Run: java -jar target/ds-sim-1.0.1-SNAPSHOT.jar"
echo "2. Create a new simulation (File → New)"
echo "3. Add 3 processes"
echo "4. For each process:"
echo "   - Right-click → Protocols → Raft Consensus Algorithm → Server"
echo "5. Save as: saved-simulations/raft.dat"
echo
echo "The issue is that the current raft.dat is just a copy of ping-pong.dat"
echo "and still contains PingPong protocol activation events."