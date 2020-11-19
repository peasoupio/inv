package io.peasoup.inv.cli

import groovy.transform.CompileStatic
import io.peasoup.inv.Logger
import io.peasoup.inv.graph.DeltaGraph
import spark.utils.StringUtils

@CompileStatic
class DeltaCommand implements CliCommand {

    String base
    String other

    int call() {
        if (StringUtils.isEmpty(base))
            return 1

        if (StringUtils.isEmpty(other))
            return 2

        def delta = new DeltaGraph(
                new File(base).newReader(),
                new File(other).newReader())
        delta.resolve()

        Logger.trace(delta.echo())

        return 0
    }

    boolean rolling() {
        return false
    }
}
