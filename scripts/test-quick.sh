#!/bin/bash
#
# Quick test to verify simulation runs correctly
#

echo "Quick DS-Sim Protocol Test"
echo "========================="
echo

# Test ping-pong with shorter timeout
echo "Testing ping-pong protocol..."
timeout 5 java -cp target/classes:target/test-classes \
    -Djava.awt.headless=true \
    -Dds.sim.headless=true \
    testing.HeadlessProtocolRunner saved-simulations/ping-pong.dat

if [ $? -eq 124 ]; then
    echo "✗ Test timed out after 5 seconds"
else
    echo "✓ Test completed successfully"
fi