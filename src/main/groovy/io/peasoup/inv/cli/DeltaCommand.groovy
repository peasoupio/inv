package io.peasoup.inv.cli

import groovy.transform.CompileStatic
import io.peasoup.inv.graph.DeltaGraph

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

        /*
        if (args.hasHtml)
            print(delta.html(arg1.name))
        else
        */

        print(delta.echo())

        return 0
    }

    boolean rolling() {
        return false
    }
}
