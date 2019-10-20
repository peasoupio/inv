package io.peasoup.inv.graph

class DeltaGraph {

    final private static lf = System.properties['line.separator']

    Map beforeNodes = [:]
    Map beforeEdges = [:]

    Map afterNodes = [:]
    Map afterEdges = [:]

    Map sharedNodes = [:]
    Map sharedEdges = [:]

    DeltaGraph(BufferedReader before, BufferedReader after) {

        def beforePlainGraph = new PlainGraph(before)
        def afterPlainGraph = new PlainGraph(after)

        beforeNodes += beforePlainGraph.nodes
        beforeEdges += beforePlainGraph.edges

        afterNodes += afterPlainGraph.nodes
        afterEdges += afterPlainGraph.edges


        // Calculate shared nodes
        beforeNodes.each { String name, HashSet<Map> edges ->
            if (!afterNodes[name])
                return

            def afterNode = afterNodes[name] as List<Map>

            if (edges.size() != afterNode.size())
                return

            for (def i = 0; i < edges.size(); i++) {
                Map beforeEdge = edges[i]
                Map afterEdge = afterNode[i]

                if (!beforeEdge.equals(afterEdge))
                    continue

                if (!sharedNodes.containsKey(name))
                    sharedNodes << [(name): new HashSet<>()]

                sharedNodes[name] << beforeEdge
            }
        }

        // Calculate shared edges
        beforeEdges.clone().each { String name, HashSet<Map> edges ->
            if (!afterEdges[name])
                return

            def afterNode = afterEdges[name] as List<Map>

            if (edges.size() != afterNode.size())
                return

            for (def i = 0; i < edges.size(); i++) {
                Map beforeEdge = edges[i]
                Map afterEdge = afterNode[i]

                if (!beforeEdge.equals(afterEdge))
                    continue

                if (!sharedEdges.containsKey(name))
                    sharedEdges << [(name): new HashSet<>()]

                sharedEdges[name] << beforeEdge
            }

            // Remove all matched
            edges.removeAll(sharedEdges[name])
            afterNode.removeAll(sharedEdges[name])

            // Delete if empty - no need
            if (edges.isEmpty())
                beforeEdges.remove(name)

            if (afterNode.isEmpty())
                afterEdges.remove(name)
        }
    }

    String print() {
        print """    
# Regex rule:^(?'state'\\W) (?!\\#.*\$)(?'require'.*) -> (?'broadcast'.*) \\((?'id'.*)\\)\$
${
    // Shared nodes and edges
    sharedEdges
        .collectMany { String owner, Set edges ->
            edges.collect { Map edge ->
                "= ${owner} -> ${edge.owner} (${edge.broadcast})"
            }
        }
        .join(lf)
}
${
    // Deleted nodes and edges (present before, but not present after)
    beforeEdges
        .collectMany { String owner, Set edges ->
            edges.collect { Map edge ->
                "- ${owner} -> ${edge.owner} (${edge.broadcast})"
            }
        }
        .join(lf)
}
${
    // Added nodes and edges (not present before, but present after)
    afterEdges
        .collectMany { String owner, Set edges ->
            edges.collect { Map edge ->
                "+ ${owner} -> ${edge.owner} (${edge.broadcast})"
            }
        }
        .join(lf)
}
"""

    }

}
