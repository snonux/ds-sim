# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build and Development Commands

**Prerequisites:**
```bash
# Set JAVA_HOME if not already configured
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk
# Or on macOS with Homebrew: export JAVA_HOME=$(/usr/libexec/java_home -v 21)
# Or on Windows: set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.x-hotspot
```

**Essential Commands:**
```bash
# Full build (recommended)
mvn clean package

# Run the application
java -jar target/ds-sim-1.0.1-SNAPSHOT.jar

# Quick build and run
mvn clean package && java -jar target/ds-sim-1.0.1-SNAPSHOT.jar

# Clean build artifacts
mvn clean
```

**Development Commands:**
```bash
# Fast compilation for development
mvn compile

# Run directly with Maven
mvn exec:java

# Build without tests (faster development)
mvn package -DskipTests

# Run tests
mvn test

# Generate Javadoc
mvn javadoc:javadoc
```

**Code Quality Scripts:**
```bash
# Format code before commit
./scripts/formatthecode.sh

# Run pre-commit checks
./scripts/beforecommit.sh
```

## Architecture Overview

This is a distributed systems simulator built on an **event-driven architecture** with clear layered separation:

### Core Components

- **Simulator Engine**: `VSSimulator` drives the main simulation loop with `VSTaskManager` executing time-ordered tasks
- **Process Framework**: `VSAbstractProcess` provides base functionality; `VSInternalProcess` extends for simulation features
- **Event System**: All actions are `VSTask` objects containing events, executed in temporal order via priority queues
- **Protocol Framework**: `VSAbstractProtocol` base class for implementing distributed algorithms with server/client modes
- **Time Management**: Supports global time, local logical clocks, Lamport timestamps, and vector clocks
- **Message System**: `VSMessage` objects with automatic timestamp updates and configurable network simulation

### Key Patterns

- **Maven Standard Layout**: Source code in `src/main/java/` with standard Maven directory structure
- **Template Method**: Abstract protocol classes define structure, concrete implementations provide specifics  
- **Event-Driven**: Tasks trigger events which generate new tasks/messages in the simulation loop
- **Pluggable Protocols**: Register new protocols in `VSRegisteredEvents.init()`

### Implementation Guidelines

**Adding New Protocols:**
1. Extend `VSAbstractProtocol` 
2. Implement `onServerInit()`, `onClientInit()`, `onServerRecv()`, `onClientRecv()` methods
3. Register protocol class name in `VSRegisteredEvents.init()`

**Adding New Events:**
1. Extend `VSAbstractEvent`
2. Implement `onInit()` and `onStart()` methods  
3. Register event class name in `VSRegisteredEvents`

**Understanding Simulation Flow:**
- `VSSimulatorVisualization.run()` drives main loop
- `VSTaskManager.runTasks()` executes pending tasks each time step
- Tasks are either local-timed (process clock) or global-timed (simulation clock)
- Messages include network delay simulation and visual representation

## Entry Points

- **Main Class**: `simulator.VSMain` 
- **Simulator Core**: `simulator.VSSimulator`
- **Protocol Implementations**: `protocols.implementations.*`
- **Event Registration**: `events.VSRegisteredEvents.init()`