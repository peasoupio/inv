package io.peasoup.inv.web

import io.peasoup.inv.graph.DeltaGraph

class Review {

    private final DeltaGraph deltaGraph

    Review(File baseRun, File latestExecution) {
        assert baseRun
        assert baseRun.exists()

        assert latestExecution
        assert latestExecution.exists()


        deltaGraph = new DeltaGraph(baseRun.newReader(), latestExecution.newReader())
    }


    Map toMap() {
        return [
            lines: deltaGraph.deltaLines
        ]
    }

}
