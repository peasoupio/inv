package io.peasoup.inv.graph

import io.peasoup.inv.run.InvInvoker
import org.junit.Test

import static org.junit.Assert.*

class RunGraphTest {

    @Test
    void ok() {
        def logOutput1Txt =  new File(getClass().getResource('/baseRun.txt').toURI())
        def runGraph = new RunGraph(logOutput1Txt.newReader())

        // test nodes
        assertTrue runGraph.navigator.contains(new GraphNavigator.Owner(value: "ServerA"))
        assertTrue runGraph.navigator.contains(new GraphNavigator.Owner(value: "ServerB"))
        assertTrue runGraph.navigator.contains(new GraphNavigator.Owner(value: "IIS"))
        assertTrue runGraph.navigator.contains(new GraphNavigator.Owner(value: "Kubernetes"))
        assertTrue runGraph.navigator.contains(new GraphNavigator.Owner(value: "files"))
        assertTrue runGraph.navigator.contains(new GraphNavigator.Owner(value: "maven"))
        assertTrue runGraph.navigator.contains(new GraphNavigator.Owner(value: "my-app-1"))
        assertTrue runGraph.navigator.contains(new GraphNavigator.Owner(value: "my-app-2"))
        assertTrue runGraph.navigator.contains(new GraphNavigator.Owner(value: "appA"))
        assertTrue runGraph.navigator.contains(new GraphNavigator.Owner(value: "appB"))

        assertTrue runGraph.files.size() == 7
        assertTrue runGraph.files.any {it.repo == "repo1"}
        assertTrue runGraph.files.any {it.repo == "repo2"}
        assertTrue runGraph.files.any {it.repo == "repo3"}
        assertTrue runGraph.files.any {it.repo == "repo4"}
        assertTrue runGraph.files.any {it.repo == "repo5"}
        assertTrue runGraph.files.any {it.repo == "repo6"}
        assertTrue runGraph.files.any {it.repo == InvInvoker.UNDEFINED_REPO}

        assertNotNull runGraph.toDotGraph()
    }

    @Test
    void getPaths() {
        def logOutput1Txt =  new File(getClass().getResource('/baseRun.txt').toURI())
        def runGraph = new RunGraph(logOutput1Txt.newReader())

        def path = runGraph.navigator.getPaths(
                new GraphNavigator.Owner(value: "appA"),
                new GraphNavigator.Owner(value: "ServerA"))

        assertNotNull path
        assertEquals 5, path.size()
        assertEquals new GraphNavigator.Owner(value: "appA"), path[0]
        assertEquals new GraphNavigator.Owner(value: "ServerA"), path[4]
    }
}