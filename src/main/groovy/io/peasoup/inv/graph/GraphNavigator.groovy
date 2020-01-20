package io.peasoup.inv.graph

import groovy.transform.CompileStatic
import org.jgrapht.Graph
import org.jgrapht.alg.util.NeighborCache
import org.jgrapht.graph.DefaultEdge

@CompileStatic
class GraphNavigator {

    private final Graph<Linkable, DefaultEdge> g
    private final NeighborCache nc

    final Map<String, Node> nodes = [:]

    GraphNavigator(Graph<Linkable, DefaultEdge> graph) {
        g = graph
        nc = new NeighborCache(g)
    }

    void addBroadcastNode(Node node) {
        assert node != null, 'Node is required'
        assert node.owner, "Node's owner is required"
        assert node.id, "Node's id is required"

        def owner = new Owner(value: node.owner)
        def id = new Id(value: node.id)

        g.addVertex(owner)
        g.addVertex(id)
        g.addEdge(id, owner)

        nodes.put(node.owner, node)
        nodes.put(node.id, node)
    }

    void addRequireNode(Node node) {
        assert node != null, 'Node is required'
        assert node.owner, "Node's owner is required"
        assert node.id, "Node's id is required"

        def owner = new Owner(value: node.owner)
        def id = new Id(value: node.id)

        g.addVertex(owner)
        g.addVertex(id)
        g.addEdge(owner, id)

        nodes.put(node.owner, node)
    }

    Map<Linkable, Integer> requiredByAll(Linkable linkable) {

        Map<Linkable, Integer> total = [:]
        List<Linkable> predecessors = []
        List<Linkable> nextPredecessors = []

        if (!g.containsVertex(linkable))
            return null

        predecessors.addAll(nc.predecessorsOf(linkable))

        if (predecessors.isEmpty())
            return [:] as Map<Linkable, Integer>

        Integer iteration = 0
        while(!predecessors.isEmpty() || !nextPredecessors.isEmpty()) {

            if (predecessors.isEmpty()) {
                predecessors = nextPredecessors
                nextPredecessors = []

                iteration++
            }

            def predecessor = predecessors.pop()

            if (total.containsKey(predecessor))
                continue

            total.put(predecessor, iteration)

            nextPredecessors.addAll(nc.predecessorsOf(predecessor))
        }

        return total
    }

    Map<Linkable, Integer> requiresAll(Linkable linkable) {

        Map<Linkable, Integer> total = [:]
        List<Linkable> successors = []
        List<Linkable> nextSuccessors = []

        if (!g.containsVertex(linkable))
            return null

        successors.addAll(nc.successorsOf(linkable))

        if (successors.isEmpty())
            return [:] as Map<Linkable, Integer>

        Integer iteration = 0
        while(!successors.isEmpty() || !nextSuccessors.isEmpty()) {

            if (successors.isEmpty()) {
                successors = nextSuccessors
                nextSuccessors = []

                iteration++
            }

            def successor = successors.pop()

            if (total.containsKey(successor))
                continue

            total.put(successor, iteration)

            nextSuccessors.addAll(nc.successorsOf(successor))
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
