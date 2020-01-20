package io.peasoup.inv.graph

import org.junit.Test

class RunGraphTest {

    @Test
    void ok() {
        def logOutput1Txt =  new File(getClass().getResource('/logOutput1.txt').toURI())

        def runGraph = new RunGraph(logOutput1Txt.newReader())

        // test nodes
        assert runGraph.g.containsVertex(new GraphNavigator.Owner(value: "ServerA"))
        assert runGraph.g.containsVertex(new GraphNavigator.Owner(value: "ServerB"))
        assert runGraph.g.containsVertex(new GraphNavigator.Owner(value: "iis"))
        assert runGraph.g.containsVertex(new GraphNavigator.Owner(value: "Kubernetes"))
        assert runGraph.g.containsVertex(new GraphNavigator.Owner(value: "files"))
        assert runGraph.g.containsVertex(new GraphNavigator.Owner(value: "maven"))
        assert runGraph.g.containsVertex(new GraphNavigator.Owner(value: "my-app-1"))
        assert runGraph.g.containsVertex(new GraphNavigator.Owner(value: "my-app-2"))
        assert runGraph.g.containsVertex(new GraphNavigator.Owner(value: "appA"))
        assert runGraph.g.containsVertex(new GraphNavigator.Owner(value: "appB"))

        assert runGraph.files.size() == 6
        assert runGraph.files.any {it.scm == "scm1"}
        assert runGraph.files.any {it.scm == "scm2"}
        assert runGraph.files.any {it.scm == "scm3"}
        assert runGraph.files.any {it.scm == "scm4"}
        assert runGraph.files.any {it.scm == "scm5"}
        assert runGraph.files.any {it.scm == "scm6"}

        assert runGraph.toPlainList()
        assert runGraph.toDotGraph()

    }
}