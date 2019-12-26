package io.peasoup.inv.graph

import org.junit.Before
import org.junit.Test

class GraphNavigatorTest {

    GraphNavigator baseGraph

    @Before
    void setup() {
        baseGraph = new GraphNavigator()
    }

    @Test
    void ok() {

        /*
            A -> A (id1)
            B -> A (id1)

            AA -> B (id2)

            A -> B (id3)

            BB -> AA (id4)
         */

        baseGraph.addRequireNode(new TestNode(owner: "A", id: "id1"))
        baseGraph.addRequireNode(new TestNode(owner: "B", id: "id1"))
        baseGraph.addBroadcastNode(new TestNode(owner: "A", id: "id1"))

        baseGraph.addRequireNode(new TestNode(owner: "AA", id: "id2"))
        baseGraph.addBroadcastNode(new TestNode(owner: "B", id: "id2"))

        baseGraph.addRequireNode(new TestNode(owner: "A", id: "id3"))
        baseGraph.addBroadcastNode(new TestNode(owner: "B", id: "id3"))

        baseGraph.addRequireNode(new TestNode(owner: "BB", id: "id4"))
        baseGraph.addBroadcastNode(new TestNode(owner: "AA", id: "id4"))


        println baseGraph.requiredByAll(new GraphNavigator.Owner (value: "A"))
        println baseGraph.requiresAll(new GraphNavigator.Owner (value: "A"))

    }

    class TestNode implements GraphNavigator.Node {
        String owner
        String id
    }
}
