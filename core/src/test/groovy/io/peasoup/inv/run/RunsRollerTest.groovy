package io.peasoup.inv.run

import io.peasoup.inv.TempHome
import org.junit.Test
import org.junit.runner.RunWith

import static org.junit.Assert.*

@RunWith(TempHome.class)
class RunsRollerTest {

    @Test
    void ok() {
        assertNotNull RunsRoller.runsFolder()
        assertTrue RunsRoller.runsFolder().exists()
    }

    @Test
    void roll() {
        RunsRoller.forceDelete();
        assertNull RunsRoller.runsFolder().listFiles()

        RunsRoller.getLatest().roll()

        assertNotNull RunsRoller.runsFolder().listFiles()
        assertEquals 2, RunsRoller.runsFolder().listFiles().length
        assertTrue RunsRoller.runsFolder().listFiles().any { it.name == "latest" }
        assertTrue RunsRoller.runsFolder().listFiles().any { it.name == "1" }
    }

    @Test
    void roll_multiple_times() {
        RunsRoller.runsFolder().deleteDir()
        assertNull RunsRoller.runsFolder().listFiles()

        int amount = 12

        1.upto(amount, {
            RunsRoller.getLatest().roll()
        })

        assertNotNull RunsRoller.runsFolder().listFiles() != null
        assertEquals  amount + 1, RunsRoller.runsFolder().listFiles().length
        assertTrue RunsRoller.runsFolder().listFiles().any { it.name == "latest" }

        1.upto(amount, { index ->
            assertTrue RunsRoller.runsFolder().listFiles().any { it.name == index.toString() }
        })
    }

}
