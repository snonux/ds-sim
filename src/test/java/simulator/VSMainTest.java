package simulator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

public class VSMainTest {
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
}
