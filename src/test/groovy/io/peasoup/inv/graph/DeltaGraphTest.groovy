package io.peasoup.inv.graph

import org.junit.Test

class DeltaGraphTest {

    @Test
    void ctor() {
        def logOutput1Txt =  new File(getClass().getResource('/logOutput1.txt').toURI())
        def logAfterOutput1Txt =  new File(getClass().getResource('/logAfterOutput1.txt').toURI())

        def deltaGraph = new DeltaGraph(logOutput1Txt.newReader(), logAfterOutput1Txt.newReader())

        deltaGraph.print()
    }
}