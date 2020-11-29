package io.peasoup.inv.cli

import groovy.transform.CompileStatic
import io.peasoup.inv.Logger
import io.peasoup.inv.graph.RunGraph
import spark.utils.StringUtils

@CompileStatic
class GraphCommand implements CliCommand {

    @Override
    int call(Map args = [:]) {
        if (args == null)
            throw new IllegalArgumentException("args")

        String base = args["<base>"]
        if (StringUtils.isEmpty(base))
            return 1

        File baseFile = new File(base)
        if (!baseFile.exists())
            return 2

        def run = new RunGraph(baseFile.newReader())

        // Print "dot" graph format
        Logger.trace(run.toDotGraph())
        return 0
    }

    @Override
    boolean rolling() {
        return false
    }

    @Override
    String usage() {
        """
Generate delta between two run files.

Usage:
  inv [-dsx] graph <base>

Arguments:
  <base>       Base file location
"""
    }
}
