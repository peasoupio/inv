package io.peasoup.inv.cli

import io.peasoup.inv.Home
import io.peasoup.inv.TempHome
import io.peasoup.inv.run.RunsRoller
import org.junit.Test
import org.junit.runner.RunWith

import static org.junit.Assert.*

@RunWith(TempHome.class)
class PromoteCommandTest {

    @Test
    void ok() {
        assertEquals(PromoteCommand.DEFAULT, RunsRoller.latest.successFolder())
        RunsRoller.latest.successFolder().mkdirs()
        def run = new File(RunsRoller.latest.successFolder(), "run.txt")
        run.delete()
        run << "something"

        def dest = new File(Home.getCurrent(), "run.txt")

        assertEquals(0, new PromoteCommand().call())
        assertTrue(dest.exists())

        dest.delete()
    }

    @Test
    void runfolder_not_existing() {
        assertEquals(1, new PromoteCommand().call("<runIndex>": "999"))
    }

    @Test
    void runfile_not_existing() {
        def run = new File(Home.getRunsFolder(), "10")
        run.mkdirs()

        assertEquals(2, new PromoteCommand().call("<runIndex>": "10"))
    }

    @Test
    void rolling() {
        assertFalse(new PromoteCommand().rolling())
    }
}
