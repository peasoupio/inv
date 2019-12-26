package io.peasoup.inv.graph

import groovy.text.SimpleTemplateEngine

class DeltaGraph {

    final private static lf = System.properties['line.separator']

    Map previousNodes = [:]
    Map previousEdges = [:]

    Map currentNodes = [:]
    Map currentEdges = [:]

    Map sharedNodes = [:]
    Map sharedEdges = [:]

    DeltaGraph(BufferedReader previous, BufferedReader current) {

        def beforePlainGraph = new RunGraph(previous)
        def afterPlainGraph = new RunGraph(current)

        previousNodes += beforePlainGraph.baseGraph.nodes
        previousEdges += beforePlainGraph.baseGraph.edges

        currentNodes += afterPlainGraph.baseGraph.nodes
        currentEdges += afterPlainGraph.baseGraph.edges


        // Calculate shared nodes
        previousNodes.each { String name, HashSet<Map> edges ->
            if (!currentNodes[name])
                return

            def currentNode = currentNodes[name] as List<Map>

            if (edges.size() != currentNode.size())
                return

            for (def i = 0; i < edges.size(); i++) {
                GraphNavigator.Node previousEdge = edges[i]
                GraphNavigator.Node afterEdge = currentNode[i]

                if (!previousEdge.equals(afterEdge))
                    continue

                if (!sharedNodes.containsKey(name))
                    sharedNodes << [(name): new HashSet<>()]

                sharedNodes[name] << previousEdge
            }
        }

        // Calculate shared edges
        previousEdges.clone().each { String name, HashSet<Map> edges ->
            if (!currentEdges[name])
                return

            def previousNode = currentEdges[name] as List<Map>

            if (edges.size() != previousNode.size())
                return

            for (def i = 0; i < edges.size(); i++) {
                GraphNavigator.Node currentEdge = edges[i]
                GraphNavigator.Node previousEdge = previousNode[i]

                if (currentEdge.owner != previousEdge.owner && currentEdge.id != previousEdge.id)
                    continue

                if (!sharedEdges.containsKey(name))
                    sharedEdges << [(name): new HashSet<>()]

                sharedEdges[name] << currentEdge
            }

            // Remove all matched
            edges.removeAll(sharedEdges[name])
            previousNode.removeAll(sharedEdges[name])

            // Delete if empty - no need
            if (edges.isEmpty())
                previousEdges.remove(name)

            if (previousNode.isEmpty())
                currentEdges.remove(name)
        }
    }

    String echo() {
        // Regex rule:^(?'state'\\W) (?!\\#.*\$)(?'require'.*) -> (?'broadcast'.*) \\((?'id'.*)\\)\$
        return """    
${
    // Shared nodes and edges
    sharedEdges
        .collectMany { String owner, Set edges ->
            edges.collect { GraphNavigator.Node edge ->
                "= ${owner} -> ${edge.owner} (${edge.id})"
            }
        }
        .join(lf)
}
${
    // Deleted nodes and edges (in previous, not in current)
    previousEdges
        .collectMany { String owner, Set edges ->
            edges.collect { GraphNavigator.Node edge ->
                "- ${owner} -> ${edge.owner} (${edge.id})"
            }
        }
        .join(lf)
}
${
    // Added nodes and edges (not in previous, but in current)
    currentEdges
        .collectMany { String owner, Set edges ->
            edges.collect { GraphNavigator.Node edge ->
                "+ ${owner} -> ${edge.owner} (${edge.id})"
            }
        }
        .join(lf)
}
"""

    }

    String html(String previousFilename) {


        def templateEngine = new SimpleTemplateEngine()
        def htmlReport = this.class.getResource("/delta-report.template.html")

        def htmlOutput = new File("./reports/${previousFilename}.html")

        htmlOutput.mkdirs()

        if (htmlOutput.exists())
            htmlOutput.delete()

        htmlOutput << templateEngine.createTemplate(htmlReport.text).make([
                now: new Date().toString(),
                previousFile: previousFilename,
                lines: echo().split(System.lineSeparator())
        ])

        return "Report generated at: ${htmlOutput.canonicalPath}"
    }

}
