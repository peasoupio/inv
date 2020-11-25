package io.peasoup.inv.cli

import groovy.transform.CompileStatic
import io.peasoup.inv.Logger
import io.peasoup.inv.graph.RunGraph

@CompileStatic
class GraphCommand implements CliCommand {

    @Override
    int call(Map args = [:]) {
        if (args == null)
            throw new IllegalArgumentException("args")

        String base = args["<base>"]
        if (!base)
            return 1

        def run = new RunGraph(new File(base).newReader())

        String graphType = null

        // For future uses
        if (args["dot"])
            graphType = "dot"

        switch (graphType) {
            case "dot":
                Logger.trace(run.toDotGraph())
                break
            default:
                Logger.trace(run.toDotGraph())
        }

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

    @Override
    boolean requireSafeExecutionLibraries() {
        return false
    }
}
