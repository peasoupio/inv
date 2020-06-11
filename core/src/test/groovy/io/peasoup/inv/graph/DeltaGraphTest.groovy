package io.peasoup.inv.graph


import org.junit.Test

import static org.junit.jupiter.api.Assertions.assertThrows

class DeltaGraphTest {

    @Test
    void ok() {
        def logOutput1Txt =  new File(getClass().getResource('/baseRun.txt').toURI())
        def logAfterOutput1Txt =  new File(getClass().getResource('/subsetRun.txt').toURI())

        def deltaGraph = new DeltaGraph(logOutput1Txt.newReader(), logAfterOutput1Txt.newReader())
        deltaGraph.resolve()

        assert deltaGraph.deltaLines
        assert deltaGraph.echo()
    }

    @Test
    void html() {
        def logOutput1Txt =  new File(getClass().getResource('/baseRun.txt').toURI())
        def logAfterOutput1Txt =  new File(getClass().getResource('/subsetRun.txt').toURI())

        def deltaGraph = new DeltaGraph(logOutput1Txt.newReader(), logAfterOutput1Txt.newReader())
        deltaGraph.resolve()

        assert deltaGraph.deltaLines
        assert deltaGraph.html("my_previous_filename")

        //TODO Randomly crash on Travis
        //assert new File(RunsRoller.latest.folder(), "./reports/my_previous_filename.html").exists()
    }

    @Test
    void not_ok() {

        def existing = new File(getClass().getResource('/baseRun.txt').toURI())

        assertThrows(AssertionError.class, {
            new DeltaGraph(null, null)
        })

        assertThrows(AssertionError.class, {
            new DeltaGraph(existing.newReader(), null)
        })

        assertThrows(AssertionError.class, {
            new DeltaGraph(null, existing.newReader())
        })
    }
}