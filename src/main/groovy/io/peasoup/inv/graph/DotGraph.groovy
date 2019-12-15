package io.peasoup.inv.graph

class DotGraph {

    final private static lf = System.properties['line.separator']

    Map nodes = [:]
    Map edges = [:]

    DotGraph(BufferedReader logs) {

        def plainGraph = new PlainGraph(logs)

        nodes += plainGraph.baseGraph.nodes
        edges += plainGraph.baseGraph.edges


    }

    String echo() {
        return """
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
            edges.collect { BaseGraph.Node edge ->
                "\t\"${owner}\" -> \"${edge.owner}\" [ label = \"${edge.id.replace("undefined", "").trim()}\" ];"
            }
        }
        .join(lf)
}
}
"""
    }

}
