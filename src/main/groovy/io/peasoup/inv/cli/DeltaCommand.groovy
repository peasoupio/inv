package io.peasoup.inv.cli

import io.peasoup.inv.graph.DeltaGraph

class DeltaCommand {

    static int call(String base, String other) {
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

}
