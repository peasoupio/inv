package io.peasoup.inv.graph

import io.peasoup.inv.run.RunsRoller
import org.junit.Test

import static org.junit.jupiter.api.Assertions.assertThrows

class DeltaGraphTest {

    @Test
    void ok() {
        def logOutput1Txt =  new File(getClass().getResource('/baseRun.txt').toURI())
        def logAfterOutput1Txt =  new File(getClass().getResource('/subsetRun.txt').toURI())

        def deltaGraph = new DeltaGraph(logOutput1Txt.newReader(), logAfterOutput1Txt.newReader())

        println deltaGraph.echo()
    }

    @Test
    void html() {
        def logOutput1Txt =  new File(getClass().getResource('/baseRun.txt').toURI())
        def logAfterOutput1Txt =  new File(getClass().getResource('/subsetRun.txt').toURI())

        def deltaGraph = new DeltaGraph(logOutput1Txt.newReader(), logAfterOutput1Txt.newReader())

        deltaGraph.html("my_previous_filename")

        assert new File(RunsRoller.latest.folder(), "./reports/my_previous_filename.html").exists()
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