package io.peasoup.inv.graph

import org.junit.Ignore
import org.junit.Test

import static org.junit.jupiter.api.Assertions.assertThrows

class DeltaGraphTest {

    @Test
    void ok() {
        def logOutput1Txt =  new File(getClass().getResource('/logOutput1.txt').toURI())
        def logAfterOutput1Txt =  new File(getClass().getResource('/logAfterOutput1.txt').toURI())

        def deltaGraph = new DeltaGraph(logOutput1Txt.newReader(), logAfterOutput1Txt.newReader())

        println deltaGraph.echo()
    }

    @Test
    @Ignore
    void html() {
        def logOutput1Txt =  new File(getClass().getResource('/logOutput1.txt').toURI())
        def logAfterOutput1Txt =  new File(getClass().getResource('/logAfterOutput1.txt').toURI())

        def deltaGraph = new DeltaGraph(logOutput1Txt.newReader(), logAfterOutput1Txt.newReader())

        deltaGraph.html("my_previous_filename")

        assert new File("./reports/my_previous_filename.html").exists()
    }

    @Test
    void not_ok() {

        def existing = new File(getClass().getResource('/logOutput1.txt').toURI())

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