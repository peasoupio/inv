package io.peasoup.inv.composer

import io.peasoup.inv.Home
import io.peasoup.inv.TempHome
import io.peasoup.inv.graph.DeltaGraph
import io.peasoup.inv.graph.RunGraph
import io.peasoup.inv.run.RunsRoller
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TempHome.class)
class ReviewTest {

    @Test
    void ok() {
        def base =  new File(getClass().getResource('/baseRun.txt').toURI())
        def other =  new File(getClass().getResource('/subsetRun.txt').toURI())

        def review = new Review(base, other)

        assert review.compare().lines
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

        assert baseRun.text == testContent
    }

    @Test
    void merge() {
        RunsRoller.latest.roll() // make sure to roll once

        // Generate base
        def baseRun = new File(Home.getCurrent(), 'run.txt')
        baseRun.delete()
        baseRun << new File(getClass().getResource('/baseRun.txt').toURI()).text

        // Generate subset
        def subsetFile =  new File(RunsRoller.latest.folder(), 'run.txt')
        subsetFile.delete()
        subsetFile << new File(getClass().getResource('/subsetRun.txt').toURI()).text

        def deltaGraph = new DeltaGraph(baseRun.newReader(), subsetFile.newReader())
        deltaGraph.resolve()

        new Review(baseRun, subsetFile).merge()
        assert subsetFile.exists()

        def newBaseFileGraph = new RunGraph(subsetFile.newReader())
        def removed = deltaGraph.deltaLines.findAll { it.state == 'x' }

        assert removed.size() > 0
        removed.each {
            assert !newBaseFileGraph.navigator.contains(it.link)
        }
    }
}
