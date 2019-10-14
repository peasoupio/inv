package io.peasoup.inv.graph

class DotGraph {

    final private static lf = System.properties['line.separator']

    Map nodes = [:]
    Map edges = [:]

    DotGraph(BufferedReader logs) {

        def plainGraph = new PlainGraph(logs)

        nodes += plainGraph.nodes
        edges += plainGraph.edges
    }

    String print() {
        print """
digraph inv {
${
    edges.keySet()
        .collect { String owner ->
            "\t\"${owner}\";"
        }
        .join(lf)
}
    
${
    edges
        .collectMany { String owner, Set edges ->
            edges.collect { Map edge ->
                "\t\"${owner}\" -> \"${edge.owner}\" [ label = \"${edge.require}\" ];"
            }
        }
        .join(lf)
}
}

"""
    }

}
