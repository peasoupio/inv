package io.peasoup.inv.graph

import io.peasoup.inv.run.InvInvoker
import org.junit.Test

class RunGraphTest {

    @Test
    void ok() {
        def logOutput1Txt =  new File(getClass().getResource('/baseRun.txt').toURI())

        def runGraph = new RunGraph(logOutput1Txt.newReader())

        // test nodes
        assert runGraph.navigator.contains(new GraphNavigator.Owner(value: "ServerA"))
        assert runGraph.navigator.contains(new GraphNavigator.Owner(value: "ServerB"))
        assert runGraph.navigator.contains(new GraphNavigator.Owner(value: "IIS"))
        assert runGraph.navigator.contains(new GraphNavigator.Owner(value: "Kubernetes"))
        assert runGraph.navigator.contains(new GraphNavigator.Owner(value: "files"))
        assert runGraph.navigator.contains(new GraphNavigator.Owner(value: "maven"))
        assert runGraph.navigator.contains(new GraphNavigator.Owner(value: "my-app-1"))
        assert runGraph.navigator.contains(new GraphNavigator.Owner(value: "my-app-2"))
        assert runGraph.navigator.contains(new GraphNavigator.Owner(value: "appA"))
        assert runGraph.navigator.contains(new GraphNavigator.Owner(value: "appB"))

        assert runGraph.files.size() == 7
        assert runGraph.files.any {it.scm == "scm1"}
        assert runGraph.files.any {it.scm == "scm2"}
        assert runGraph.files.any {it.scm == "scm3"}
        assert runGraph.files.any {it.scm == "scm4"}
        assert runGraph.files.any {it.scm == "scm5"}
        assert runGraph.files.any {it.scm == "scm6"}
        assert runGraph.files.any {it.scm == InvInvoker.UNDEFINED_SCM}

        assert runGraph.toPlainList()
        assert runGraph.toDotGraph()

    }
}