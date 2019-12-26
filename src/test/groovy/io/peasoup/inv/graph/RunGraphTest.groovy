package io.peasoup.inv.graph

import org.junit.Test

class RunGraphTest {

    @Test
    void ctor() {
        def logOutput1Txt =  new File(getClass().getResource('/logOutput1.txt').toURI())

        def plainGraph = new RunGraph(logOutput1Txt.newReader())

        // test nodes
        assert plainGraph.baseGraph.nodes["ServerA"]
        assert plainGraph.baseGraph.nodes["ServerB"]
        assert plainGraph.baseGraph.nodes["iis"]
        assert plainGraph.baseGraph.nodes["Kubernetes"]
        assert plainGraph.baseGraph.nodes["files"]
        assert plainGraph.baseGraph.nodes["maven"]
        assert plainGraph.baseGraph.nodes["my-app-1"]
        assert plainGraph.baseGraph.nodes["my-app-2"]
        assert plainGraph.baseGraph.nodes["appA"]
        assert plainGraph.baseGraph.nodes["appB"]

        // test edges
        assert plainGraph.baseGraph.edges["iis"].find { it.owner == "ServerA" }
        assert plainGraph.baseGraph.edges["Kubernetes"].find { it.owner == "ServerA" }
        assert plainGraph.baseGraph.edges["maven"].find { it.owner == "files" }
        assert plainGraph.baseGraph.edges["my-app-1"].find { it.owner == "maven" }
        assert plainGraph.baseGraph.edges["my-app-2"].find { it.owner == "maven" }
        assert plainGraph.baseGraph.edges["my-app-2"].find { it.owner == "my-app-1" }
        assert plainGraph.baseGraph.edges["appA"].find { it.owner == "Kubernetes" }
        assert plainGraph.baseGraph.edges["appB"].find { it.owner == "iis" }

        assert plainGraph.baseGraph.edges["ServerA"].isEmpty()
        assert plainGraph.baseGraph.edges["ServerB"].isEmpty()

        plainGraph.echo()
    }
}