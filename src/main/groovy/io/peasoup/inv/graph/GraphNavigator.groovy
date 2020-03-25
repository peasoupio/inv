package io.peasoup.inv.graph

import groovy.transform.CompileStatic
import org.jgrapht.Graph
import org.jgrapht.alg.shortestpath.DijkstraShortestPath
import org.jgrapht.alg.util.NeighborCache
import org.jgrapht.graph.DefaultEdge

@CompileStatic
class GraphNavigator {

    private final Graph<Linkable, DefaultEdge> g
    private final NeighborCache nc

    final Map<String, Node> nodes = [:]

    GraphNavigator(Graph<Linkable, DefaultEdge> graph) {
        assert graph, 'Graph is required'

        g = graph
        nc = new NeighborCache(g)
    }

    /**
     * Determines whether or not a linkable object is present in the graph
     * @param linkable the linkable object (can be null)
     * @return Returns true is present, otherwise false
     */
    boolean contains(Linkable linkable) {
        if (!linkable)
            return false

        return g.containsVertex(linkable)
    }

    /**
     * Returns all the available linkable in the graph
     * @return A Set representation of the linkable objects
     */
    Set<Linkable> links() {
        return g.vertexSet()
    }

    /**
     * Adds a broadcast type node into the graph.
     * If either node's name or id is already present, it will be replaced
     * @param node the node object
     */
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

    /**
     * Adds a require type node into the graph.
     * If either node's name or id is already present, it will be replaced
     * @param node the node object
     */
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

    /**
     * List the immediate predecessors of a linkable (one layer)
     * @param linkable the linkable object
     * @return A list of linkable objects which requires the one specified
     */
    List<Linkable> requiredBy(Linkable linkable) {
        assert linkable, 'Linkable is required'

        if (!g.containsVertex(linkable))
            return null

        return nc.predecessorsOf(linkable).toList()
    }

    /**
     * List all the predecessors of a linkable (all layers)
     * @param linkable the linkable object
     * @return A list of linkable objects which requires the one specified
     */
    Map<Linkable, Integer> requiredByAll(Linkable linkable) {
        assert linkable, 'Linkable is required'

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

    /**
     * List all the successors of a linkable (all layers)
     * @param linkable the linkable object
     * @return A list of linkable objects which are required by the one specified
     */
    Map<Linkable, Integer> requiresAll(Linkable linkable, List<Linkable> excludes = []) {
        assert linkable, 'Linkable is required'
        assert excludes != null, 'Excludes is required (but can be empty)'

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

            if (excludes.contains(successor))
                continue

            if (total.containsKey(successor))
                continue

            total.put(successor, iteration)

            nextSuccessors.addAll(nc.successorsOf(successor))
        }

        return total
    }

    List<Linkable> getPaths(Linkable leaf, Linkable root) {
        assert leaf
        assert root

        assert g.containsVertex(leaf)
        assert g.containsVertex(root)

        def directedPaths = new DijkstraShortestPath<Linkable, DefaultEdge>(g)

        def sourcePath = directedPaths.getPaths(leaf)
        def destPath = sourcePath.getPath(root)

        if (!destPath)
            return []

        def output = destPath.vertexList
        //output.removeAll { it.isId() }

        return output
    }

    /**
     * A generic neutral node type
     */
    interface Node {
        String getOwner()
        String getId()
    }

    /**
     * A generic neutral linkable type.
     * Nodes are managed in sets of owners and ids.
     * Per example this graph:
     *     Owner1 broadcasts Id1
     *     Owner2 broadcasts Id2
     *     Id1 requires Id2
     * would resolve into those linkable:
     *     Owner1 [Owner]
     *     Owner2 [Owner]
     *     Id1 [Id]
     *     Id2 [Id]
     * and then resolve into this sequence:
     *     Owner1 -> Id1 -> Id2 -> Owner2
     */
    interface Linkable {
        String getValue()
        boolean isId()
        boolean isOwner()
    }

    /**
     * A default "Owner" implementation of a linkable
     */
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
            if (obj == null)
                return false

            if (value == null)
                return false

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

    /**
     * A default "Id" implementation of a linkable
     */
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
            if (obj == null)
                return false

            if (value == null)
                return false

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
