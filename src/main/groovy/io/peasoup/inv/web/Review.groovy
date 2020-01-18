package io.peasoup.inv.web

import io.peasoup.inv.graph.DeltaGraph

class Review {

    private final File baseRun
    private final File latestExecution
    private final DeltaGraph deltaGraph

    Review(File baseRun, File latestExecution) {
        assert baseRun, 'Base run file is required'
        assert baseRun.exists(), 'Base run file must exist on filesystem'

        assert latestExecution, 'Latest execution file is required'
        assert latestExecution.exists(), 'Latest execution file must exist on filesystem'

        this.baseRun = baseRun
        this.latestExecution = latestExecution

        deltaGraph = new DeltaGraph(baseRun.newReader(), latestExecution.newReader())
    }


    Map toMap() {
        return [
            baseExecution: baseRun.lastModified(),
            lastExecution: latestExecution.lastModified(),
            lines: deltaGraph.deltaLines
        ]
    }

}
