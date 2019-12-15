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

    interface Node {
        String owner
        String id
    }
}
