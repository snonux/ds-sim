# SimulationBuilder Framework

## Overview

The SimulationBuilder framework provides a programmatic way to create DS-Sim simulation files without using the GUI. This solves the problem of needing to manually create simulations through the graphical interface.

## Framework Components

### 1. SimulationBuilder (`simulator.builder.SimulationBuilder`)

The core builder class that provides a fluent API for creating simulations:

```java
new SimulationBuilder()
    .withProcesses(5)
    .withProtocol("protocols.implementations.VSRaftProtocol")
    .withDuration(20000)
    .activateServers(0, 1, 2)
    .activateClients(1000, 3, 4)
    .addCrashEvent(0, 5000)
    .addRecoveryEvent(0, 10000)
    .save("saved-simulations/my-simulation.dat");
```

### 2. SimulationFactory (`simulator.builder.SimulationFactory`)

Factory methods for common simulation patterns:

```java
// Create a standard 3-server Raft cluster
SimulationFactory.createRaftSimulation(3, 0)
    .save("saved-simulations/raft.dat");

// Create a Raft cluster with fault tolerance testing
SimulationFactory.createRaftFaultToleranceSimulation(5)
    .save("saved-simulations/raft-fault-tolerant.dat");

// Other protocols
SimulationFactory.createPingPongSimulation(2);
SimulationFactory.createBerkeleyTimeSimulation(4);
SimulationFactory.createTwoPhaseCommitSimulation(3);
SimulationFactory.createReliableMulticastSimulation(5);
SimulationFactory.createBroadcastSimulation(4);
```

## Key Features

### Process Management
- Specify number of processes
- Default is 3 processes (DS-Sim standard)
- Can add or remove processes as needed

### Protocol Configuration
- Set protocol by full classname
- Convenience constants for common protocols
- Activate as server or client on specific processes

### Event Scheduling
- Add protocol activation events at specific times
- Support for crash and recovery events
- Custom event scheduling

### Simulation Parameters
- Set simulation duration
- Configure timing parameters
- All preferences preserved

## Implementation Details

### Challenges Solved

1. **Private Methods**: Used reflection to access private `addProcess()` method
2. **Serialization Format**: Properly serializes using VSSerialize format
3. **Object Initialization**: Correct order for creating simulator components
4. **Protocol Events**: Proper configuration of VSProtocolEvent fields

### Technical Approach

The framework:
1. Creates preferences and initializes event registry
2. Builds simulator and visualization objects
3. Manages processes through reflection
4. Schedules protocol activation events
5. Serializes using DS-Sim's custom format

## Usage Examples

### Basic Raft Simulation
```java
new SimulationBuilder()
    .withProcesses(3)
    .withProtocol(SimulationBuilder.Protocols.RAFT)
    .activateServers(0, 1, 2)
    .save("saved-simulations/raft.dat");
```

### Complex Scenario
```java
new SimulationBuilder()
    .withProcesses(7)
    .withProtocol(SimulationBuilder.Protocols.RAFT)
    .withDuration(30000)
    .activateServers(0, 1, 2, 3, 4)  // 5 servers
    .activateClients(1000, 5, 6)     // 2 clients
    .addCrashEvent(2, 5000)          // Server 2 crashes
    .addRecoveryEvent(2, 8000)       // Server 2 recovers
    .addCrashEvent(0, 10000)         // Leader crashes
    .addRecoveryEvent(0, 12000)      // Leader recovers
    .save("saved-simulations/raft-complex.dat");
```

## Created Simulations

Using this framework, we successfully created:

1. **raft.dat** - Basic 3-node Raft cluster
2. **raft-with-clients.dat** - Raft with 3 servers and 2 clients  
3. **raft-fault-tolerant.dat** - 5 servers with crash/recovery events
4. **raft-complex.dat** - 7 processes with complex fault scenarios

## Verification

The created files:
- Are valid DS-Sim .dat files
- Can be loaded in the DS-Sim GUI
- Contain the correct protocol classnames
- Include all specified events and configurations

## Limitations

1. **Protocol Activation**: While the framework creates valid simulation files with protocol activation events, the actual protocol behavior depends on proper implementation of the protocol's `onServerStart()` method.

2. **GUI Dependencies**: Some aspects of the simulation still have GUI dependencies, though the framework works around these using reflection.

3. **Serialization Complexity**: The Java serialization format requires exact object graph structure, making some configurations challenging.

## Conclusion

The SimulationBuilder framework successfully eliminates the need for GUI-based simulation creation. It provides a clean, programmatic API that can be used in:
- Automated testing
- Batch simulation generation
- CI/CD pipelines
- Research experiments

This framework demonstrates that with careful analysis and proper use of reflection, even tightly GUI-coupled applications can be automated for headless operation.