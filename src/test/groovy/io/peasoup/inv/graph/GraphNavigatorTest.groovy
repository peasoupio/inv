package io.peasoup.inv.graph


import org.jgrapht.graph.DefaultDirectedGraph
import org.jgrapht.graph.DefaultEdge
import org.junit.Before
import org.junit.Test

import static org.junit.jupiter.api.Assertions.assertThrows

class GraphNavigatorTest {

    GraphNavigator graph

    @Before
    void setup() {
        graph = new GraphNavigator(new DefaultDirectedGraph<>(DefaultEdge.class))
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

        graph.addRequireNode(new TestNode(owner: "A", id: "id1"))
        graph.addRequireNode(new TestNode(owner: "B", id: "id1"))
        graph.addBroadcastNode(new TestNode(owner: "A", id: "id1"))

        graph.addRequireNode(new TestNode(owner: "AA", id: "id2"))
        graph.addBroadcastNode(new TestNode(owner: "B", id: "id2"))

        graph.addRequireNode(new TestNode(owner: "A", id: "id3"))
        graph.addBroadcastNode(new TestNode(owner: "B", id: "id3"))

        graph.addRequireNode(new TestNode(owner: "BB", id: "id4"))
        graph.addBroadcastNode(new TestNode(owner: "AA", id: "id4"))

        def requiredBy = graph.requiredByAll(new GraphNavigator.Owner (value: "A"))
        assert requiredBy.containsKey(new GraphNavigator.Id(value: "id1"))
        assert requiredBy.containsKey(new GraphNavigator.Id(value: "id2"))
        assert requiredBy.containsKey(new GraphNavigator.Owner(value: "A"))
        assert requiredBy.containsKey(new GraphNavigator.Owner(value: "B"))

        def requires = graph.requiresAll(new GraphNavigator.Owner (value: "A"))

        assert requires.containsKey(new GraphNavigator.Id(value: "id1"))
        assert requires.containsKey(new GraphNavigator.Id(value: "id3"))
        assert requires.containsKey(new GraphNavigator.Owner(value: "A"))
        assert requires.containsKey(new GraphNavigator.Owner(value: "B"))

    }

    @Test
    void linkable_toString() {
        def value = "value1"

        assert new GraphNavigator.Id(value:value).toString() == "ID=${value}"
        assert new GraphNavigator.Owner(value:value).toString() == "Owner=${value}"
    }

    @Test
    void addBroadcast_not_ok() {

        assertThrows(AssertionError.class, {
            graph.addBroadcastNode(null)
        })

        assertThrows(AssertionError.class, {
            graph.addBroadcastNode(new TestNode(owner: "", id: "ok"))
        })

        assertThrows(AssertionError.class, {
            graph.addBroadcastNode(new TestNode(owner: "ok", id: ""))
        })
    }

    @Test
    void addRequireNode_not_ok() {

        assertThrows(AssertionError.class, {
            graph.addRequireNode(null)
        })

        assertThrows(AssertionError.class, {
            graph.addRequireNode(new TestNode(owner: "", id: "ok"))
        })

        assertThrows(AssertionError.class, {
            graph.addRequireNode(new TestNode(owner: "ok", id: ""))
        })
    }

    @Test
    void requiredByAll_not_existing() {

        def notExisting = graph.requiredByAll(new GraphNavigator.Id(value: "notexisting"))

        assert notExisting == null
    }

    @Test
    void requiredByAll_empty() {
        graph.addBroadcastNode(new TestNode(owner: "owner", id: "id"))

        def empty = graph.requiredByAll(new GraphNavigator.Id(value: "id"))

        assert empty.isEmpty()
    }

    @Test
    void requiresAll_not_existing() {

        def notExisting = graph.requiresAll(new GraphNavigator.Id(value: "notexisting"))

        assert notExisting == null
    }

    @Test
    void requiresAll_empty() {
        graph.addBroadcastNode(new TestNode(owner: "owner", id: "id"))

        def notEmpty = graph.requiresAll(new GraphNavigator.Id(value: "id"))
        assert !notEmpty.isEmpty()

        def empty = graph.requiresAll(new GraphNavigator.Owner(value: "owner"))
        assert empty.isEmpty()
    }


    @Test
    void id_notEqual() {

        assert !new GraphNavigator.Id().equals("something")

        def id = new GraphNavigator.Id(value: "1")

        assert !id.equals(null)
        assert !id.equals([:])
        assert !id.equals(new GraphNavigator.Id(value: "2"))
    }

    @Test
    void owner_notEqual() {

        assert !new GraphNavigator.Owner().equals("something")

        def owner = new GraphNavigator.Owner(value: "1")

        assert !owner.equals(null)
        assert !owner.equals([:])
        assert !owner.equals(new GraphNavigator.Owner(value: "2"))
    }

    class TestNode implements GraphNavigator.Node {
        String owner
        String id
        long index
    }
}
