package io.peasoup.inv.cli

import io.peasoup.inv.Home
import io.peasoup.inv.TempHome
import io.peasoup.inv.run.RunsRoller
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TempHome.class)
class PromoteCommandTest {

    @Test
    void ok() {
        assert PromoteCommand.DEFAULT == RunsRoller.latest.successFolder()
        RunsRoller.latest.successFolder().mkdirs()
        def run = new File(RunsRoller.latest.successFolder(), "run.txt")
        run.delete()
        run << "something"

        def dest = new File(Home.getCurrent(), "run.txt")

        assert new PromoteCommand().call() == 0
        assert dest.exists()

        dest.delete()
    }

    @Test
    void runfolder_not_existing() {
        assert new PromoteCommand(runIndex: "999").call() == -1
    }

    @Test
    void runfile_not_existing() {
        def run = new File(RunsRoller.runsFolder(), "10")
        run.mkdirs()

        assert new PromoteCommand(runIndex: "10").call() == -2
    }

    @Test
    void rolling() {
        assert !new PromoteCommand().rolling()
    }
}
