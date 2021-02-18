package io.peasoup.inv.run

import io.peasoup.inv.Home
import io.peasoup.inv.TempHome
import org.junit.Test
import org.junit.runner.RunWith

import static org.junit.Assert.*

@RunWith(TempHome.class)
class RunsRollerTest {

    @Test
    void ok() {
        assertNotNull Home.getRunsFolder()
        assertTrue Home.getRunsFolder().exists()
    }

    @Test
    void roll() {
        RunsRoller.forceDelete();
        assertNull Home.getRunsFolder().listFiles()

        RunsRoller.getLatest().roll()

        assertNotNull Home.getRunsFolder().listFiles()
        assertEquals 2, Home.getRunsFolder().listFiles().length
        assertTrue Home.getRunsFolder().listFiles().any { it.name == "latest" }
        assertTrue Home.getRunsFolder().listFiles().any { it.name == "1" }
    }

    @Test
    void roll_multiple_times() {
        Home.getRunsFolder().deleteDir()
        assertNull Home.getRunsFolder().listFiles()

        int amount = 12

        1.upto(amount, {
            RunsRoller.getLatest().roll()
        })

        assertNotNull Home.getRunsFolder().listFiles() != null
        assertEquals  amount + 1, Home.getRunsFolder().listFiles().length
        assertTrue Home.getRunsFolder().listFiles().any { it.name == "latest" }

        1.upto(amount, { index ->
            assertTrue Home.getRunsFolder().listFiles().any { it.name == index.toString() }
        })
    }

}
