package io.peasoup.inv.graph

class BaseGraph {

    List<Node> requires = []
    List<Node> broadcasts = []

    Map<String, Set<Node>> nodes = [:]
    Map<String, Set<Node>> edges = [:]

    BaseGraph() {

    }

    def addBroadcastNode(Node node) {

        assert node != null
        assert node.owner
        assert node.id

        // Add or override existing node
        if (!nodes.containsKey(node.owner))
            nodes << [(node.owner): new HashSet<>()]

        nodes[node.owner] << node

        broadcasts << node
    }

    def addRequireNode(Node node) {

        assert node != null
        assert node.owner
        assert node.id

        // Add or override existing node
        if (!nodes.containsKey(node.owner))
            nodes << [(node.owner): new HashSet<>()]

        nodes[node.owner] << node

        requires << node
    }

    def linkEdges() {

        // Merge broadcasts them into an index
        Map<String, List<Node>> broadcasts_index = broadcasts.collectEntries { [(it.id): it]}

        // Create edges
        (broadcasts + requires).each {
            if (edges.containsKey(it.owner))
                return

            edges << [(it.owner): new HashSet<>()]
        }

        // Look requires to get matches with broadcasts
        requires.each {
            def match = broadcasts_index[it.id]

            if (!match)
                return

            edges[it.owner] << match
        }
    }

    Map flattenedEdges() {
        return edges.collectEntries { String owner, Set<Node> _nodes ->
            Closure<Set<Node>> recursive
            recursive = { Set<Node> nodes ->

                if (nodes.isEmpty())
                    return []

                return nodes.collectMany { Node node ->
                    def myNodes = edges[node.owner]

                    if (myNodes.isEmpty())
                        return [node]

                    return [node] + recursive.call(myNodes)
                }

            }

            return [(owner): recursive(_nodes)]
        }
    }

    interface Node {
        String owner
        String id
    }
}
