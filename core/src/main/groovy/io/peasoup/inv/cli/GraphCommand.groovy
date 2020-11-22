package io.peasoup.inv.cli

import groovy.transform.CompileStatic
import io.peasoup.inv.Logger
import io.peasoup.inv.graph.RunGraph

@CompileStatic
class GraphCommand implements CliCommand {

    Map arguments

    int call() {
        if (arguments == null)
            return 1

        String base = arguments["<base>"] as String
        def run = new RunGraph(new File(base).newReader())

        String graphType = null

        // For future uses
        if (arguments["dot"])
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

    boolean rolling() {
        return false
    }
}
