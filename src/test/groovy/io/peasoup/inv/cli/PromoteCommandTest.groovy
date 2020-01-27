package io.peasoup.inv.cli

import io.peasoup.inv.Main
import io.peasoup.inv.run.RunsRoller
import org.junit.Test

class PromoteCommandTest {

    @Test
    void ok() {
        assert PromoteCommand.DEFAULT == RunsRoller.latest.successFolder()
        RunsRoller.latest.successFolder().mkdirs()
        def run = new File(RunsRoller.latest.successFolder(), "run.txt")
        run.delete()
        run << "something"

        def dest = new File(Main.currentHome, "run.txt")

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

        new File(RunsRoller.runsFolder(), "123").mkdirs()

        assert new PromoteCommand(runIndex: "123").call() == -2
    }

    @Test
    void rolling() {
        assert !new PromoteCommand().rolling()
    }
}
