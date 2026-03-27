package simulator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import java.awt.Component;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import prefs.VSDefaultPrefs;
import prefs.VSPrefs;

public class VSMainTest {
    @AfterEach
    void resetHooks() {
        VSMain.resetTestHooks();
    }

    @Test
    void resolveStartupSimulationFileReturnsNullForMissingArgs() {
        assertNull(VSMain.resolveStartupSimulationFile(null));
        assertNull(VSMain.resolveStartupSimulationFile(new String[0]));
        assertNull(VSMain.resolveStartupSimulationFile(new String[] {""}));
        assertNull(VSMain.resolveStartupSimulationFile(new String[] {"   "}));
    }

    @Test
    void resolveStartupSimulationFileUsesFirstArgument() {
        assertEquals("saved-simulations/raft.dat",
                     VSMain.resolveStartupSimulationFile(
                         new String[] {"saved-simulations/raft.dat"}));
        assertEquals("saved-simulations/raft.dat",
                     VSMain.resolveStartupSimulationFile(
                         new String[] {"  saved-simulations/raft.dat  ",
                                       "ignored"}));
    }

    @Test
    void runOnEventDispatchThreadExecutesOnSwingEdt() {
        AtomicBoolean ranOnEdt = new AtomicBoolean(false);

        VSMain.runOnEventDispatchThread(new Runnable() {
            public void run() {
                ranOnEdt.set(javax.swing.SwingUtilities
                             .isEventDispatchThread());
            }
        });

        assertTrue(ranOnEdt.get());
    }

    @Test
    void launchSimulatorFrameCreatesAndStartsOnSwingEdt() {
        VSPrefs prefs = VSDefaultPrefs.init();
        AtomicBoolean createdOnEdt = new AtomicBoolean(false);
        AtomicBoolean openedOnEdt = new AtomicBoolean(false);
        AtomicReference<String> openedFilename = new AtomicReference<String>();
        VSSimulatorFrame frame = mock(VSSimulatorFrame.class);
        doAnswer(invocation -> {
            openedOnEdt.set(javax.swing.SwingUtilities
                            .isEventDispatchThread());
            openedFilename.set(invocation.getArgument(0, String.class));
            return null;
        }).when(frame).openAndStartSimulator("saved-simulations/raft.dat");

        VSSimulatorFrame launchedFrame = VSMain.launchSimulatorFrame(
            prefs, null, "saved-simulations/raft.dat",
            new VSMain.SimulatorFrameFactory() {
                public VSSimulatorFrame create(VSPrefs framePrefs,
                                               Component relativeTo) {
                    createdOnEdt.set(javax.swing.SwingUtilities
                                     .isEventDispatchThread());
                    return frame;
                }
            });

        assertTrue(createdOnEdt.get());
        assertTrue(openedOnEdt.get());
        assertNotNull(launchedFrame);
        assertSame(frame, launchedFrame);
        assertEquals("saved-simulations/raft.dat", openedFilename.get());
    }

    @Test
    void mainRoutesStartupReplayThroughEdt() {
        AtomicInteger splashCalls = new AtomicInteger(0);
        AtomicInteger delayCalls = new AtomicInteger(0);
        AtomicBoolean createdOnEdt = new AtomicBoolean(false);
        AtomicBoolean openedOnEdt = new AtomicBoolean(false);
        AtomicReference<String> openedFilename = new AtomicReference<String>();
        VSSimulatorFrame frame = mock(VSSimulatorFrame.class);

        VSMain.splashScreenLauncher = new VSMain.SplashScreenLauncher() {
            public void show() {
                splashCalls.incrementAndGet();
            }
        };
        VSMain.startupDelay = new VSMain.StartupDelay() {
            public void pause() {
                delayCalls.incrementAndGet();
            }
        };
        VSMain.simulatorFrameFactory = new VSMain.SimulatorFrameFactory() {
            public VSSimulatorFrame create(VSPrefs framePrefs,
                                           Component relativeTo) {
                createdOnEdt.set(javax.swing.SwingUtilities
                                 .isEventDispatchThread());
                return frame;
            }
        };

        doAnswer(invocation -> {
            openedOnEdt.set(javax.swing.SwingUtilities
                            .isEventDispatchThread());
            openedFilename.set(invocation.getArgument(0, String.class));
            return null;
        }).when(frame).openAndStartSimulator("saved-simulations/raft.dat");

        VSMain.main(new String[] {"saved-simulations/raft.dat"});

        assertEquals(1, splashCalls.get());
        assertEquals(1, delayCalls.get());
        assertTrue(createdOnEdt.get());
        assertTrue(openedOnEdt.get());
        assertEquals("saved-simulations/raft.dat", openedFilename.get());
    }
}
