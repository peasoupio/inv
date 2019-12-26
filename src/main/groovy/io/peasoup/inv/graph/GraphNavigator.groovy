package io.peasoup.inv.graph

import org.jgrapht.Graph
import org.jgrapht.alg.util.NeighborCache
import org.jgrapht.graph.DefaultEdge

class GraphNavigator {

    private final Graph<Linkable, DefaultEdge> g
    private final NeighborCache nc

    final Map<String, Node> nodes = [:]

    GraphNavigator(Graph<Linkable, DefaultEdge> graph) {
        g = graph
        nc = new NeighborCache(g)
    }

    def addBroadcastNode(Node node) {

        assert node != null
        assert node.owner
        assert node.id

        def owner = new Owner(value: node.owner)
        def id = new Id(value: node.id)

        g.addVertex(owner)
        g.addVertex(id)
        g.addEdge(id, owner)

        nodes.put(node.owner, node)
        nodes.put(node.id, node)
    }

    def addRequireNode(Node node) {

        assert node != null
        assert node.owner
        assert node.id

        def owner = new Owner(value: node.owner)
        def id = new Id(value: node.id)

        g.addVertex(owner)
        g.addVertex(id)
        g.addEdge(owner, id)

        nodes.put(node.owner, node)
    }

    def requiredByAll(Linkable linkable) {

        Set<Linkable> total = []
        List<Linkable> predecessors = []

        if (!g.containsVertex(linkable))
            return

        predecessors.addAll(nc.predecessorsOf(linkable))

        if (predecessors.isEmpty())
            return []

        while(!predecessors.isEmpty()) {
            def predecessor = predecessors.pop()

            if (total.contains(predecessor))
                continue

            total << predecessor

            predecessors.addAll(nc.predecessorsOf(predecessor))
        }

        return total
    }

    def requiresAll(Linkable linkable) {

        Set<Linkable> total = []
        List<Linkable> successors = []

        if (!g.containsVertex(linkable))
            return

        successors.addAll(nc.successorsOf(linkable))

        if (successors.isEmpty())
            return []

        while(!successors.isEmpty()) {
            def successor = successors.pop()

            if (total.contains(successor))
                continue

            total << successor

            successors.addAll(nc.successorsOf(successor))
        }

        return total
    }

    interface Node {
        String getOwner()
        String getId()
    }

    interface Linkable {
        String getValue()
        boolean isId()
        boolean isOwner()
    }

    static class Owner implements Linkable {
        String value

        boolean isId() {
            return false
        }

        boolean isOwner() {
            return true
        }

        @Override
        boolean equals(Object obj) {
            if (!value)
                false

            if (obj instanceof Owner)
                return value == ((Owner) obj).value

            return false
        }

        @Override
        int hashCode() {
            return value.hashCode()
        }

        @Override
        String toString() {
            return "Owner=${value}"
        }
    }

    static class Id implements Linkable {
        String value

        boolean isId() {
            return true
        }

        boolean isOwner() {
            return false
        }

        @Override
        boolean equals(Object obj) {
            if (!value)
                false

            if (obj instanceof Id)
                return value == ((Id) obj).value

            return false
        }

        @Override
        int hashCode() {
            return value.hashCode()
        }

        @Override
        String toString() {
            return "ID=${value}"
        }
    }
}
