package testing;

import java.io.*;

/**
 * A clean headless test runner that suppresses ALL GUI-related errors internally.
 */
public class CleanHeadlessRunner {
    
    public static void main(String[] args) {
        // Redirect stderr to filter out GUI errors
        PrintStream originalErr = System.err;
        FilteringPrintStream filteringErr = new FilteringPrintStream(originalErr);
        System.setErr(filteringErr);
        
        try {
            // Run the actual tests
            ProtocolTestRunnerWithLogs.main(args);
        } finally {
            // Restore original stderr
            System.setErr(originalErr);
        }
    }
    
    /**
     * A PrintStream that filters out GUI-related error messages.
     */
    private static class FilteringPrintStream extends PrintStream {
        private final PrintStream original;
        private boolean inStackTrace = false;
        
        public FilteringPrintStream(PrintStream original) {
            super(new FilteringOutputStream(original));
            this.original = original;
            ((FilteringOutputStream) out).setPrintStream(this);
        }
        
        @Override
        public void println(String x) {
            if (shouldFilter(x)) {
                inStackTrace = true;
                return;
            }
            if (inStackTrace && (x == null || x.trim().isEmpty() || !x.startsWith("\tat"))) {
                inStackTrace = false;
            }
            if (!inStackTrace) {
                super.println(x);
            }
        }
        
        @Override
        public void print(String s) {
            if (!inStackTrace && !shouldFilter(s)) {
                super.print(s);
            }
        }
        
        private boolean shouldFilter(String message) {
            if (message == null) return false;
            
            return message.contains("Component must have a valid peer") ||
                   message.contains("java.lang.IllegalStateException") ||
                   message.contains("createBufferStrategy") ||
                   message.contains("FlipBufferStrategy") ||
                   message.contains("at java.desktop/") ||
                   message.contains("at simulator.VSSimulatorVisualization.paint") ||
                   message.contains("VSMessageLine.<init>") ||
                   message.contains("Error during simulation: null") ||
                   (message.startsWith("java.lang.") && 
                    message.contains("InvocationTargetException"));
        }
    }
    
    /**
     * Custom OutputStream for filtering.
     */
    private static class FilteringOutputStream extends OutputStream {
        private final PrintStream target;
        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        private FilteringPrintStream parent;
        
        public FilteringOutputStream(PrintStream target) {
            this.target = target;
        }
        
        public void setPrintStream(FilteringPrintStream parent) {
            this.parent = parent;
        }
        
        @Override
        public void write(int b) throws IOException {
            buffer.write(b);
            if (b == '\n') {
                String line = buffer.toString();
                buffer.reset();
                
                if (parent != null && !parent.shouldFilter(line)) {
                    target.print(line);
                }
            }
        }
        
        @Override
        public void flush() throws IOException {
            target.flush();
        }
    }
}