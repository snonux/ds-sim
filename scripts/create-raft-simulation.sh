#!/bin/bash
#
# Create and verify a Raft consensus simulation for DS-Sim
#

echo "=== DS-Sim Raft Simulation Creator ==="
echo

# Check if we're in the right directory
if [ ! -f "pom.xml" ]; then
    echo "Error: Must be run from the DS-Sim root directory"
    exit 1
fi

# Check if the application is built
if [ ! -f "target/ds-sim-1.0.1-SNAPSHOT.jar" ]; then
    echo "Error: Application not built. Run 'mvn clean package' first"
    exit 1
fi

# Create the raft.dat file from template
if [ -f "saved-simulations/ping-pong.dat" ]; then
    cp "saved-simulations/ping-pong.dat" "saved-simulations/raft.dat"
    echo "✓ Created saved-simulations/raft.dat from template"
else
    echo "✗ Error: Could not find ping-pong.dat template"
    exit 1
fi

# Create a verification script
cat > verify-raft.sh << 'EOF'
#!/bin/bash
echo "Verifying Raft simulation..."
if [ -f "saved-simulations/raft.dat" ]; then
    echo "✓ raft.dat exists"
    ls -lh saved-simulations/raft.dat
else
    echo "✗ raft.dat not found"
fi
EOF
chmod +x verify-raft.sh

echo
echo "=== MANUAL STEPS REQUIRED ==="
echo
echo "The raft.dat file has been created but needs manual configuration."
echo "Please follow these steps:"
echo
echo "1. Start DS-Sim:"
echo "   java -jar target/ds-sim-1.0.1-SNAPSHOT.jar"
echo
echo "2. Open the template:"
echo "   File → Open → saved-simulations/raft.dat"
echo
echo "3. Configure for Raft (IMPORTANT - do all steps):"
echo "   a) Delete existing protocol events in the task list"
echo "   b) Right-click Process 1 → Protocols → Raft Consensus Algorithm → Server"
echo "   c) Right-click Process 2 → Protocols → Raft Consensus Algorithm → Server"
echo "   d) Right-click Process 3 → Protocols → Raft Consensus Algorithm → Server"
echo
echo "4. Save the file:"
echo "   File → Save"
echo
echo "5. Test the simulation:"
echo "   Click the Play button to see leader election"
echo
echo "=== EXPECTED BEHAVIOR ==="
echo "- All nodes start as FOLLOWERS"
echo "- First node to timeout (150-300ms) becomes CANDIDATE"
echo "- CANDIDATE requests votes from other nodes"
echo "- Node with majority votes becomes LEADER (highlighted)"
echo "- LEADER sends heartbeats every 50ms"
echo
echo "To verify after configuration: ./verify-raft.sh"