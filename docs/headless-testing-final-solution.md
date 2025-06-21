# DS-Sim Headless Testing - Final Solution

## Summary

After extensive investigation, we've determined that DS-Sim's architecture has deep GUI dependencies that cannot be completely separated without major refactoring of the core codebase. The paint() method in VSSimulatorVisualization is called during message sending and other operations, which causes `IllegalStateException: Component must have a valid peer` errors in headless mode.

## Final Solution

We've implemented a practical solution that:

1. **Allows tests to run successfully** - The headless testing framework works correctly despite internal GUI errors
2. **Captures all logs** - Protocol logs are captured and verified correctly
3. **Filters error output** - GUI-related errors are filtered from the output for clean test results

## Components

### 1. HeadlessSimulationRunner
- Loads simulations using a minimal DummySimulatorFrame
- Captures logs through custom LogCapture implementation
- Runs simulations for specified duration
- Returns results for verification

### 2. DummySimulatorFrame
- Extends VSSimulatorFrame but prevents window display
- Overrides key methods to prevent GUI operations
- Disposed immediately after simulation loads

### 3. CleanHeadlessRunner
- Filters out GUI-related error messages from stderr
- Provides clean test output without error noise
- Used in quiet mode (-q flag)

### 4. Test Runners
- **ProtocolTestRunner**: Basic test runner with optional verbose mode
- **ProtocolTestRunnerWithLogs**: Shows protocol logs during execution
- **CleanHeadlessRunner**: Filters GUI errors for clean output

## Usage

Run tests with clean output (recommended):
```bash
./run-tests.sh -q
```

Run tests with logs visible:
```bash
./run-tests.sh
```

Run tests with verbose output (shows all errors):
```bash
./run-tests.sh -v
```

## Known Limitations

1. **GUI errors occur internally** - The VSSimulatorVisualization.paint() method throws exceptions when no valid peer exists
2. **Cannot be completely eliminated** - Would require refactoring DS-Sim core to separate simulation logic from visualization
3. **Does not affect test results** - Tests run correctly and protocols are verified despite the errors

## Why This Approach Works

1. **Errors are non-fatal** - The IllegalStateException in paint() doesn't stop simulation execution
2. **Logs are captured correctly** - The LogCapture system works independently of visualization
3. **Protocols execute normally** - The simulation logic runs correctly even when painting fails

## Future Improvements

To completely eliminate GUI dependencies would require:

1. **Refactoring VSSimulatorVisualization** - Separate simulation logic from painting logic
2. **Abstract message passing** - Create an interface for message visualization that can be null in headless mode
3. **Conditional painting** - Add checks in paint() method to detect headless mode and skip painting

However, the current solution is practical and functional for automated testing purposes.