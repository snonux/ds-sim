package testing;

import java.io.PrintStream;
import java.io.OutputStream;

/**
 * A test runner that suppresses GUI-related error messages while still showing test results.
 * This provides a cleaner output when running headless tests.
 */
public class QuietProtocolTestRunner {
    
    public static void main(String[] args) {
        // Create a custom PrintStream that filters out specific error messages
        PrintStream originalErr = System.err;
        PrintStream filteredErr = new PrintStream(new FilteredOutputStream(originalErr));
        
        try {
            // Redirect System.err to our filtered stream
            System.setErr(filteredErr);
            
            // Run the actual test runner
            System.out.println("=== DS-Sim Protocol Test Runner (Quiet Mode) ===\n");
            System.out.println("Note: GUI errors are suppressed for cleaner output.\n");
            
            // Pass through any arguments (like -v for verbose)
            ProtocolTestRunnerWithLogs.main(args);
            
        } finally {
            // Restore original error stream
            System.setErr(originalErr);
        }
    }
    
    /**
     * An OutputStream that filters out specific error messages.
     */
    private static class FilteredOutputStream extends OutputStream {
        private final PrintStream target;
        private final StringBuilder buffer = new StringBuilder();
        
        public FilteredOutputStream(PrintStream target) {
            this.target = target;
        }
        
        @Override
        public void write(int b) {
            buffer.append((char) b);
            
            // Check if we have a complete line
            if (b == '\n') {
                String line = buffer.toString();
                
                // Filter out specific GUI-related errors
                if (!line.contains("Component must have a valid peer") &&
                    !line.contains("java.lang.IllegalStateException") &&
                    !line.contains("at java.desktop/") &&
                    !line.contains("at simulator.VSSimulatorVisualization.paint") &&
                    !line.contains("createBufferStrategy") &&
                    !line.contains("FlipBufferStrategy")) {
                    
                    // Pass through other messages
                    target.print(line);
                }
                
                buffer.setLength(0);
            }
        }
        
        @Override
        public void flush() {
            target.flush();
        }
        
        @Override
        public void close() {
            target.close();
        }
    }
}