package simulator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.GraphicsEnvironment;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import prefs.VSDefaultPrefs;
import serialize.VSSerialize;

public class VSSimulatorFrameStartupTest {
    private static final String RAFT_FILE = "saved-simulations/raft.dat";

    private static final class TrackingSimulatorFrame extends VSSimulatorFrame {
        private static final long serialVersionUID = 1L;

        volatile boolean revalidated;
        volatile boolean repainted;
        volatile int startCalls;

        TrackingSimulatorFrame() {
            super(VSDefaultPrefs.init(), null);
            setVisible(false);
        }

        @Override
        public void revalidate() {
            super.revalidate();
            revalidated = true;
        }

        @Override
        public void repaint() {
            super.repaint();
            repainted = true;
        }

        @Override
        public void startCurrentSimulator() {
            ++startCalls;
            super.startCurrentSimulator();
        }

        void resetTracking() {
            revalidated = false;
            repainted = false;
            startCalls = 0;
        }
    }

    @Test
    void openAndStartSimulatorRefreshesAndStartsLoadedSimulation() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                                "requires a display");

        TrackingSimulatorFrame frame = new TrackingSimulatorFrame();
        try {
            frame.resetTracking();

            VSSimulator simulator = frame.openAndStartSimulator(RAFT_FILE);
            assertNotNull(simulator);

            SwingUtilities.invokeAndWait(() -> { });

            assertEquals(simulator, frame.getCurrentSimulator());
            assertTrue(frame.revalidated);
            assertTrue(frame.repainted);
            assertEquals(1, frame.startCalls);
            assertFalse(simulator.getSimulatorCanvas().isPaused());
            assertTrue(simulator.getSimulatorCanvas().getNumProcesses() > 0);
        } finally {
            SwingUtilities.invokeAndWait(frame::dispose);
        }
    }

    @Test
    void openSimulatorPreservesSavedReplayDuration() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                                "requires a display");

        TrackingSimulatorFrame frame = new TrackingSimulatorFrame();
        try {
            frame.resetTracking();

            VSSerialize serialize = new VSSerialize();
            VSSimulator simulator = serialize.openSimulator(RAFT_FILE, frame);
            assertNotNull(simulator);

            SwingUtilities.invokeAndWait(() -> { });

            assertEquals(simulator, frame.getCurrentSimulator());
            assertEquals(60, simulator.getPrefs().getInteger("sim.seconds"));
            assertEquals(60000L, simulator.getSimulatorCanvas().getUntilTime());
        } finally {
            SwingUtilities.invokeAndWait(frame::dispose);
        }
    }
}
