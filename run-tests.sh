#!/bin/bash

# DS-Sim Protocol Test Runner Script
# This script runs all protocol tests using the headless testing framework

echo "=== DS-Sim Protocol Test Runner ==="
echo

# Check if target/classes exists
if [ ! -d "target/classes" ]; then
    echo "Building project..."
    mvn compile || { echo "Build failed!"; exit 1; }
fi

# Parse arguments
QUIET=false
VERBOSE=false

while [[ $# -gt 0 ]]; do
    case $1 in
        -q|--quiet)
            QUIET=true
            shift
            ;;
        -v|--verbose)
            VERBOSE=true
            shift
            ;;
        -h|--help)
            echo "Usage: $0 [options]"
            echo "Options:"
            echo "  -q, --quiet     Run with filtered output (hides GUI errors)"
            echo "  -v, --verbose   Show detailed logs during tests"
            echo "  -h, --help      Show this help"
            echo
            echo "Available test runners:"
            echo "  Default:        Shows test results and limited logs"
            echo "  Quiet mode:     Filters out GUI-related errors"
            echo "  Verbose mode:   Shows all logs during test execution"
            exit 0
            ;;
        *)
            echo "Unknown option: $1"
            echo "Run with -h for help"
            exit 1
            ;;
    esac
done

# Run the appropriate test runner
if [ "$QUIET" = true ]; then
    echo "Running tests in quiet mode (GUI errors filtered)..."
    echo
    java -cp target/classes testing.CleanHeadlessRunner
elif [ "$VERBOSE" = true ]; then
    echo "Running tests in verbose mode..."
    echo
    java -cp target/classes testing.ProtocolTestRunner -v
else
    echo "Running tests with log output..."
    echo
    java -cp target/classes testing.ProtocolTestRunnerWithLogs
fi

exit_code=$?
exit $exit_code