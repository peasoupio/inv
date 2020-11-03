package io.peasoup.inv.cli

import groovy.transform.CompileStatic
import io.peasoup.inv.graph.DeltaGraph
import io.peasoup.inv.run.Logger

@CompileStatic
class DeltaCommand implements CliCommand {

    String base
    String other

    int call() {
        assert base, 'Base is required'
        assert other, 'Other is required'

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
