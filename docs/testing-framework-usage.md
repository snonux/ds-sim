# DS-Sim Testing Framework Usage

## Overview

The DS-Sim testing framework provides headless testing capabilities for protocol simulations without requiring GUI components.

## Quick Start

Run all protocol tests:
```bash
./run-tests.sh
```

## Test Runners

### 1. Standard Test Runner (with logs)
Shows test results with protocol logs:
```bash
java -cp target/classes testing.ProtocolTestRunnerWithLogs
```

### 2. Quiet Test Runner
Filters out GUI-related errors for cleaner output:
```bash
java -cp target/classes testing.QuietProtocolTestRunner
# or
./run-tests.sh -q
```

### 3. Verbose Test Runner
Shows detailed logs for debugging:
```bash
java -cp target/classes testing.ProtocolTestRunner -v
# or
./run-tests.sh -v
```

## Running Specific Tests

To run tests programmatically:
```java
HeadlessSimulationRunner runner = new HeadlessSimulationRunner();
runner.setPrintLogs(true); // Enable log output

SimulationResult result = runner.runSimulation(
    "saved-simulations/ping-pong.dat", 
    2000  // Duration in ms
);

// Verify results
ProtocolVerifier verifier = new ProtocolVerifier()
    .expectLog("Ping-Pong.*activated")
    .expectLog("Message sent")
    .expectNoLog("ERROR");

VerificationResult verification = verifier.verify(result.getAllLogs());
System.out.println("Test passed: " + verification.passed());
```

## Test Coverage

The framework tests all non-Raft protocols:
- Ping-Pong
- Ping-Pong Sturm
- Broadcast
- Basic Multicast
- Reliable Multicast
- Berkeley Time Sync
- Internal Time Sync
- External vs Internal Sync
- One-Phase Commit
- Two-Phase Commit
- Slow Connection

## Known Limitations

- Some GUI-related errors may appear due to DS-Sim's tight coupling with visual components
- These errors don't affect test functionality
- Use quiet mode (`-q`) to filter them out

## Maven Integration

Run tests as part of the build:
```bash
mvn test
```

Note: Ensure Maven Surefire plugin is properly configured to discover test classes.