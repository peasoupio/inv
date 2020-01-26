package io.peasoup.inv.cli

import io.peasoup.inv.Main
import io.peasoup.inv.run.Logger
import io.peasoup.inv.run.RunsRoller

import java.nio.file.Files
import java.nio.file.StandardCopyOption

class PromoteCommand implements CliCommand {

    private static final DEFAULT = RunsRoller.latest.successFolder()

    String runIndex // optional

    int call() {
        File toPromote = DEFAULT

        if (runIndex && runIndex.isInteger())
            toPromote = new File(RunsRoller.runsFolder(), runIndex.toString())

        if (!toPromote.exists()) {
            Logger.warn "You attempt to promote a non-existing run"
            return -1
        }

        def runFile = new File(toPromote, "run.txt")

        if (!runFile.exists()) {
            Logger.warn "run.txt does not exist in the run folder to promote"
            return -2
        }

        Files.copy(runFile.toPath(), new File(Main.currentHome, "run.txt").toPath(), StandardCopyOption.REPLACE_EXISTING)

        Logger.debug "${runFile.canonicalPath} has been promoted to ${Main.currentHome.canonicalPath}"

        return 0
    }

    boolean rolling() {
        return false
    }
}
