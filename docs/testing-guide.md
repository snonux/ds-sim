# DS-Sim Testing Guide

## Overview

DS-Sim uses a comprehensive testing framework that includes:
- Unit tests for core components using JUnit 5
- Integration tests for protocol implementations
- Headless simulation testing framework with full GUI decoupling

## Running Tests

### Unit Tests

Run all unit tests:
```bash
mvn test
```

This runs tests for:
- Core components (`core/*Test.java`)
- Event system (`events/**/*Test.java`)
- Protocol abstractions (`protocols/VSAbstractProtocolTest.java`)
- Specific protocols (PingPong, Raft)

### Protocol Simulation Tests

With the GUI decoupling implementation complete, protocol simulations now run cleanly in headless mode:

```bash
# Use the interactive test script
./test-protocols.sh

# Run all protocol tests directly
java -cp target/classes:target/test-classes -Djava.awt.headless=true \
    testing.HeadlessProtocolRunner

# Run a specific protocol test
java -cp target/classes:target/test-classes -Djava.awt.headless=true \
    testing.HeadlessProtocolRunner saved-simulations/ping-pong.dat

# Test GUI decoupling (verify no errors)
java -cp target/classes:target/test-classes testing.TestNoGuiErrors
```

### Running Specific Tests

```bash
# Run a single test class
mvn test -Dtest=VSMessageTest

# Run tests matching a pattern
mvn test -Dtest="*Protocol*"

# Run with specific profile
mvn test -Punit-tests-only
```

### Building Without Tests

```bash
mvn clean package -DskipTests
```

## Test Structure

### Unit Tests (src/test/java/)
- `core/` - Tests for core classes (VSMessage, VSTask)
- `events/` - Event system tests
- `protocols/` - Protocol implementation tests

### Integration Tests
These can now run in headless mode:
- `testing/protocols/*Test.java` - Protocol simulation tests
- `HeadlessEngineTest.java` - Engine decoupling tests

## Headless Testing Framework

### Overview
DS-Sim includes a complete headless testing framework for running protocol simulations without GUI. The GUI decoupling has been fully implemented, allowing all tests to run cleanly in CI/CD environments.

### Key Components

1. **MessageHandler Interface** - Decouples message sending from visualization
2. **HeadlessLoader** - Loads simulations without creating GUI components
3. **HeadlessSimulationRunner** - Core headless test runner

### Available Test Runners

Located in `src/main/java/testing/`:

1. **HeadlessSimulationRunner** - Core headless test runner
   ```java
   HeadlessSimulationRunner runner = new HeadlessSimulationRunner();
   runner.setPrintLogs(true);
   SimulationResult result = runner.runSimulation("saved-simulations/ping-pong.dat", 2000);
   ```

2. **HeadlessProtocolRunner** - Runs all protocol tests with summary
3. **TestNoGuiErrors** - Verifies GUI decoupling is working correctly

### Running Headless Tests

```bash
# Run all protocol tests
java -cp target/classes:target/test-classes -Djava.awt.headless=true \
    testing.HeadlessProtocolRunner

# Run with verbose output
java -cp target/classes:target/test-classes -Djava.awt.headless=true \
    -Dds.sim.verbose=true testing.HeadlessProtocolRunner

# Verify no GUI errors
java -cp target/classes:target/test-classes testing.TestNoGuiErrors
```

## Test Configuration

### Maven Surefire Configuration (pom.xml)

The project is configured to run only unit tests by default:

```xml
<plugin>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
        <includes>
            <include>**/core/*Test.java</include>
            <include>**/events/**/*Test.java</include>
            <include>**/protocols/VSAbstractProtocolTest.java</include>
            <include>**/protocols/implementations/VSPingPongProtocolTest.java</include>
            <include>**/protocols/implementations/VSRaftProtocolTest.java</include>
        </includes>
        <excludes>
            <exclude>**/SimpleRaftGUITest.java</exclude>
            <exclude>**/testing/**/*Test.java</exclude>
        </excludes>
    </configuration>
</plugin>
```

## Writing Tests

### Unit Tests
Standard JUnit 5 tests:
```java
@Test
void testMessageCreation() {
    VSMessage message = new VSMessage();
    message.setString("type", "REQUEST");
    assertEquals("REQUEST", message.getString("type"));
}
```

### Protocol Tests
For testing protocol behavior with mocks:
```java
@BeforeEach
void setUp() {
    mockProcess = mock(VSInternalProcess.class);
    protocol = new VSPingPongProtocol();
    protocol.init(mockProcess);
}
```

### Headless Simulation Tests
```java
HeadlessSimulationRunner runner = new HeadlessSimulationRunner();
SimulationResult result = runner.runSimulation("saved-simulations/test.dat", 5000);

// Verify results
assertTrue(result.getAllLogs().size() > 0);
assertEquals(expectedProcessCount, result.getMetrics().getNumProcesses());
```

## CI/CD Integration

All tests now work in CI/CD environments:

```yaml
# Example GitHub Actions workflow
- name: Run all tests
  run: |
    mvn test
    ./test-protocols.sh
```

## Troubleshooting

### JUnit version conflicts
Ensure JUnit version matches across dependencies (currently 5.10.0).

### Test failures after code changes
Always run `mvn clean` before testing to ensure fresh compilation.

### Slow test execution
Protocol tests may take a few seconds to run. Use shorter timeouts for faster CI builds.