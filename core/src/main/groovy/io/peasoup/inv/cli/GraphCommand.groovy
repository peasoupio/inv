package io.peasoup.inv.cli

import groovy.transform.CompileStatic
import io.peasoup.inv.graph.RunGraph
import io.peasoup.inv.run.Logger

@CompileStatic
class GraphCommand implements CliCommand {

    Map arguments

    int call() {
        assert arguments != null, 'A valid value is required for args'

        String base = arguments["<base>"] as String
        def run = new RunGraph(new File(base).newReader())

        if (arguments["dot"])
            Logger.trace(run.toDotGraph())

        return 0
    }

    boolean rolling() {
        return false
    }
}
