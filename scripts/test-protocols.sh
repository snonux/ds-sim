#!/bin/bash
#
# DS-Sim Protocol Test Runner
# 
# This script runs protocol simulation tests in headless mode.
# GUI decoupling has been implemented, so tests run cleanly without GUI errors.
#

echo "DS-Sim Protocol Test Runner"
echo "=========================="
echo

# Check if we're in the right directory
if [ ! -f "pom.xml" ]; then
    echo "ERROR: Please run this script from the project root directory"
    exit 1
fi

# Build if needed
if [ ! -d "target/classes" ]; then
    echo "Building project..."
    mvn compile -q || { echo "Build failed!"; exit 1; }
fi

# Menu
echo "Choose an option:"
echo "1) Run all protocol tests"
echo "2) Run specific protocol test"
echo "3) Run tests with detailed logs"
echo "4) Test GUI decoupling (verify no GUI errors)"
echo "5) Exit"
echo

read -p "Enter choice [1-5]: " choice

case $choice in
    1)
        echo "Running all protocol tests..."
        java -cp target/classes:target/test-classes -Djava.awt.headless=true testing.HeadlessProtocolRunner
        ;;
    2)
        echo "Available simulations:"
        ls saved-simulations/*.dat | sed 's/saved-simulations\//  - /g'
        echo
        read -p "Enter simulation name (without .dat): " sim
        if [ -f "saved-simulations/${sim}.dat" ]; then
            java -cp target/classes:target/test-classes -Djava.awt.headless=true \
                testing.HeadlessProtocolRunner "saved-simulations/${sim}.dat"
        else
            echo "Simulation not found!"
        fi
        ;;
    3)
        echo "Running tests with detailed logs..."
        java -cp target/classes:target/test-classes -Djava.awt.headless=true \
            -Dds.sim.verbose=true testing.HeadlessProtocolRunner
        ;;
    4)
        echo "Testing GUI decoupling..."
        java -cp target/classes:target/test-classes testing.TestNoGuiErrors
        ;;
    5)
        echo "Exiting..."
        exit 0
        ;;
    *)
        echo "Invalid choice!"
        exit 1
        ;;
esac