package io.peasoup.inv.run

import io.peasoup.inv.TempHome
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TempHome.class)
class RunsRollerTest {

    @Test
    void ok() {
        assert RunsRoller.runsFolder() != null
        assert RunsRoller.runsFolder().exists()
    }

    @Test
    void roll() {
        RunsRoller.forceDelete();
        assert RunsRoller.runsFolder().listFiles() == null

        RunsRoller.getLatest().roll()

        assert RunsRoller.runsFolder().listFiles() != null
        assert RunsRoller.runsFolder().listFiles().length == 2
        assert RunsRoller.runsFolder().listFiles().any { it.name == "latest" }
        assert RunsRoller.runsFolder().listFiles().any { it.name == "1" }
    }

    @Test
    void roll_multiple_times() {
        RunsRoller.runsFolder().deleteDir()
        assert RunsRoller.runsFolder().listFiles() == null

        int amount = 12

        1.upto(amount, {
            RunsRoller.getLatest().roll()
        })

        assert RunsRoller.runsFolder().listFiles() != null
        assert RunsRoller.runsFolder().listFiles().length == amount + 1
        assert RunsRoller.runsFolder().listFiles().any { it.name == "latest" }

        1.upto(amount, { index ->
            assert RunsRoller.runsFolder().listFiles().any { it.name == index.toString() }
        })
    }

}
