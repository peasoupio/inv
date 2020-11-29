package io.peasoup.inv.cli

import groovy.transform.CompileStatic
import io.peasoup.inv.Home
import io.peasoup.inv.Logger
import io.peasoup.inv.run.RunsRoller

import java.nio.file.Files
import java.nio.file.StandardCopyOption

@CompileStatic
class PromoteCommand implements CliCommand {

    private static final File DEFAULT = RunsRoller.latest.successFolder()

    @Override
    int call(Map args = [:]) {
        if (args == null)
            throw new IllegalArgumentException("args")

        String runIndex = args["<runIndex>"]

        File toPromote = DEFAULT

        if (runIndex && runIndex.isInteger())
            toPromote = new File(RunsRoller.runsFolder(), runIndex.toString())

        if (!toPromote.exists()) {
            Logger.warn "You attempt to promote a non-existing run"
            return 1
        }

        def runFile = new File(toPromote, "run.txt")

        if (!runFile.exists()) {
            Logger.warn "run.txt does not exist in the run folder to promote"
            return 2
        }

        Files.copy(runFile.toPath(), new File(Home.getCurrent(), "run.txt").toPath(), StandardCopyOption.REPLACE_EXISTING)

        Logger.system "[PROMOTE] file: ${runFile.canonicalPath}, target:${Home.getCurrent().canonicalPath}"

        return 0
    }

    @Override
    boolean rolling() {
        return false
    }

    @Override
    String usage() {
        """
Promote a run.txt as the new base.

Usage:
  inv [-dsx] promote [<runIndex>]

Arguments:
  <runIndex>   The run index whose promotion will be granted.
               Runs are located inside INV_HOME/.runs/ 
               By default, it uses the latest successful run
"""
    }
}
