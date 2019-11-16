package io.peasoup.inv.graph

import org.junit.Test

class PlainGraphTest {

    @Test
    void ctor() {
        def logOutput1Txt =  new File(getClass().getResource('/logOutput1.txt').toURI())

        def plainGraph = new PlainGraph(logOutput1Txt.newReader())

        // test nodes
        assert plainGraph.nodes["ServerA"]
        assert plainGraph.nodes["ServerB"]
        assert plainGraph.nodes["iis"]
        assert plainGraph.nodes["Kubernetes"]
        assert plainGraph.nodes["files"]
        assert plainGraph.nodes["maven"]
        assert plainGraph.nodes["my-app-1"]
        assert plainGraph.nodes["my-app-2"]
        assert plainGraph.nodes["appA"]
        assert plainGraph.nodes["appB"]

        // test edges
        assert plainGraph.edges["iis"].find { it.owner == "ServerA" }
        assert plainGraph.edges["Kubernetes"].find { it.owner == "ServerA" }
        assert plainGraph.edges["maven"].find { it.owner == "files" }
        assert plainGraph.edges["my-app-1"].find { it.owner == "maven" }
        assert plainGraph.edges["my-app-2"].find { it.owner == "maven" }
        assert plainGraph.edges["my-app-2"].find { it.owner == "my-app-1" }
        assert plainGraph.edges["appA"].find { it.owner == "Kubernetes" }
        assert plainGraph.edges["appB"].find { it.owner == "iis" }

        assert plainGraph.edges["ServerA"].isEmpty()
        assert plainGraph.edges["ServerB"].isEmpty()

        plainGraph.echo()
    }
}