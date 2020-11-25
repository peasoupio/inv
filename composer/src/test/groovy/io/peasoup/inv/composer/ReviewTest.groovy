package io.peasoup.inv.composer

import io.peasoup.inv.Home
import io.peasoup.inv.TempHome
import io.peasoup.inv.graph.DeltaGraph
import io.peasoup.inv.graph.RunGraph
import io.peasoup.inv.run.RunsRoller
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith

import static org.junit.Assert.*

@RunWith(TempHome.class)
@Ignore
class ReviewTest {

    @Test
    void ok() {
        def base =  new File(getClass().getResource('/baseRun.txt').toURI())
        def other =  new File(getClass().getResource('/subsetRun.txt').toURI())

        def review = new Review(base, other)

        assertNotNull review.compare().lines
    }

    @Test
    void promote() {
        RunsRoller.latest.roll() // make sure to roll once

        def testContent = "My run file"
        def runFile =  new File(RunsRoller.latest.folder(), 'run.txt')
        runFile.delete()
        runFile << testContent

        def baseRun = new File(Home.getCurrent(), 'run.txt')
        baseRun.delete()
        baseRun << "something"

        new Review(baseRun, runFile).promote()

        assertEquals testContent, baseRun.text
    }

    @Test
    void merge() {
        RunsRoller.latest.roll() // make sure to roll once

        // Generate base
        def baseRun = new File(Home.getCurrent(), 'base-merge.txt')
        baseRun.delete()
        baseRun << new File(getClass().getResource('/baseRun.txt').toURI()).text

        // Generate subset
        def subsetFile =  new File(RunsRoller.latest.folder(), 'run-merge.txt')
        subsetFile.delete()
        subsetFile << new File(getClass().getResource('/subsetRun.txt').toURI()).text

        def deltaGraph = new DeltaGraph(baseRun.newReader(), subsetFile.newReader())
        deltaGraph.resolve()
        def removed = deltaGraph.deltaLines.findAll { it.state == 'x' }

        new Review(baseRun, subsetFile).merge()
        assertTrue subsetFile.exists()
        def newBaseFileGraph = new RunGraph(subsetFile.newReader())

        assertFalse removed.isEmpty()
        removed.each {
            assertFalse newBaseFileGraph.navigator.contains(it.link)
        }
    }
}
