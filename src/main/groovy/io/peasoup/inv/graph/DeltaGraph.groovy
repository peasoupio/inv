package io.peasoup.inv.graph

import groovy.text.SimpleTemplateEngine

class DeltaGraph {

    final private static lf = System.properties['line.separator']

    Map<GraphNavigator.Linkable, String> deltaStates = [:]


    DeltaGraph(BufferedReader base, BufferedReader other) {

        def baseGraph = new RunGraph(base)
        def otherGraph = new RunGraph(other)

        for(GraphNavigator.Linkable link : baseGraph.g.vertexSet()) {
            if (otherGraph.g.containsVertex(link)) {
                deltaStates.put(link, '=')
            } else {
                deltaStates.put(link, '-')
            }
        }

        for(GraphNavigator.Linkable link : baseGraph.g.vertexSet()) {
            if (!baseGraph.g.containsVertex(link)) {
                deltaStates.put(link, '+')
            }
        }
    }

    String echo() {
        // Regex rule:^(?'state'\\W) (?!\\#.*\$)(?'require'.*) -> (?'broadcast'.*) \\((?'id'.*)\\)\$
        return """    
${
    // Shared nodes and edges
    deltaStates
        .collect { GraphNavigator.Linkable link, String state ->
            "${state} ${link.value}"
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
