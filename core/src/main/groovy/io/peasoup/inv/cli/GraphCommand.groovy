package io.peasoup.inv.cli

import groovy.transform.CompileStatic
import io.peasoup.inv.graph.RunGraph

@CompileStatic
class GraphCommand implements CliCommand {

    Map arguments

    int call() {
        assert arguments != null, 'A valid value is required for args'

        String base = arguments["<base>"] as String
        def run = new RunGraph(new File(base).newReader())

        if (arguments["plain"])
            println run.toPlainList()

        if (arguments["dot"])
            println run.toDotGraph()

        return 0
    }

    boolean rolling() {
        return false
    }
}