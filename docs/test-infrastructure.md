# DS-Sim Test Infrastructure

## Overview

This document describes the current test infrastructure and available test utilities.

## Test Classes Location

### Unit Tests (`src/test/java/`)
- `core/` - Core component tests
  - `VSMessageTest.java` - Message handling tests
  - `VSTaskTest.java` - Task scheduling tests
- `events/` - Event system tests
  - `VSAbstractEventTest.java` - Event framework tests
  - `VSRegisteredEventsTest.java` - Event registration tests
  - `implementations/` - Specific event tests
- `protocols/` - Protocol tests
  - `VSAbstractProtocolTest.java` - Protocol framework tests
  - `implementations/` - Specific protocol tests

### Test Utilities (`src/main/java/testing/`)

#### Core Classes
- `HeadlessSimulationRunner.java` - Main headless test runner
- `LogCapture.java` - Captures simulation logs for verification
- `SimulationResult.java` - Contains test execution results
- `ProtocolVerifier.java` - Verifies protocol behavior via logs

#### Test Runners
- `CleanHeadlessRunner.java` - Filters GUI errors from output
- `ProtocolTestRunnerWithLogs.java` - Shows detailed logs
- `SimpleProtocolTestRunner.java` - Basic test execution
- `QuietProtocolTestRunner.java` - Minimal output runner

#### Support Classes
- `DummySimulatorFrame.java` - Mock frame for headless mode
- `LogEntry.java`, `LogType.java` - Log data structures
- `VerificationRule.java`, `RuleResult.java` - Verification framework

## Maven Configuration

### Test Execution
```xml
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
```

### Dependencies
- JUnit Jupiter 5.10.0
- Mockito 5.3.1
- SLF4J + Logback for logging

## Running Tests

### Command Line
```bash
# All tests
mvn test

# Specific test
mvn test -Dtest=VSMessageTest

# Pattern
mvn test -Dtest="*Event*"
```

### IDE
- Import as Maven project
- Run test classes/packages directly
- Use IDE test runners for debugging

## Test Categories

### 1. Fast Unit Tests (Default)
- No external dependencies
- No GUI components
- Millisecond execution time
- Run in CI/CD

### 2. Integration Tests (Excluded)
- Require saved simulations
- May create GUI components
- Longer execution time
- Manual execution only

### 3. GUI Tests (Excluded)
- Require display
- Test UI components
- Manual execution only

## Writing New Tests

### Unit Test Template
```java
package core;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class MyComponentTest {
    private MyComponent component;
    
    @BeforeEach
    void setUp() {
        component = new MyComponent();
    }
    
    @Test
    void testBasicFunctionality() {
        // Given
        String input = "test";
        
        // When
        String result = component.process(input);
        
        // Then
        assertEquals("expected", result);
    }
}
```

### Protocol Test Template
```java
@BeforeEach
void setUp() {
    mockProcess = mock(VSInternalProcess.class);
    mockTaskManager = mock(VSTaskManager.class);
    when(mockProcess.getTaskManager()).thenReturn(mockTaskManager);
    
    protocol = new MyProtocol();
    protocol.init(mockProcess);
}
```

## Limitations

1. **GUI Coupling**: Many components require GUI initialization
2. **Headless Mode**: Protocol simulations produce GUI errors
3. **Serialization**: Saved simulations may have version issues

## Future Improvements

See `docs/gui-decoupling-plan.md` for proposed architecture changes that would enable comprehensive headless testing.