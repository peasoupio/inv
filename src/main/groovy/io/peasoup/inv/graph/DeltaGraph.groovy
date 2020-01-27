package io.peasoup.inv.graph

import groovy.text.SimpleTemplateEngine
import io.peasoup.inv.run.RunsRoller

class DeltaGraph {

    final private static String lf = System.properties['line.separator']

    final List<DeltaLine> deltaLines = []

    DeltaGraph(BufferedReader base, BufferedReader other) {
        assert base != null, 'Base (reader) is required'
        assert other != null, 'Other (reader) is required'

        def baseGraph = new RunGraph(base)
        def otherGraph = new RunGraph(other)

        Map<String, Integer> baseIndexes = baseGraph.navigator.nodes.keySet().withIndex().collectEntries()
        Map<String, Integer> otherIndexes = otherGraph.navigator.nodes.keySet().withIndex().collectEntries()

        for(GraphNavigator.Linkable link : baseGraph.g.vertexSet()) {

            if (link.isOwner())
                continue

            Integer index = baseIndexes[link.value]
            String owner = baseGraph.navigator.nodes[link.value].owner

            if (otherGraph.g.containsVertex(link)) {
                deltaLines << new DeltaLine(index: index, state: '=', link: link, owner: owner)
            } else {
                deltaLines << new DeltaLine(index: index, state: '-', link: link, owner: owner)
            }
        }

        for(GraphNavigator.Linkable link : otherGraph.g.vertexSet()) {

            if (link.isOwner())
                continue

            if (!baseGraph.g.containsVertex(link)) {
                Integer index = otherIndexes[link.value]
                String owner = otherGraph.navigator.nodes[link.value].owner

                deltaLines << new DeltaLine(index: index, state: '+', link: link, owner: owner)
            }
        }
    }

    String echo() {
        // Regex rule:^(?'state'\\W) (?!\\#.*\$)(?'require'.*) -> (?'broadcast'.*) \\((?'id'.*)\\)\$
        return """${
    // Shared nodes and edges
    deltaLines
        .sort { it.index }
        .collect { DeltaLine line ->
            "${line.state} ${line.link.value}"
        }
        .join(lf)
}"""

    }

    String html(String previousFilename) {

        def templateEngine = new SimpleTemplateEngine()
        def htmlReport = this.class.getResource("/delta-report.template.html")

        def reportFolder = new File(RunsRoller.latest.folder(), "reports/")
        reportFolder.mkdirs()

        def htmlOutput = new File(reportFolder, "${previousFilename}.html")

        if (htmlOutput.exists())
            htmlOutput.delete()

        htmlOutput << templateEngine.createTemplate(htmlReport.text).make([
                now: new Date().toString(),
                previousFile: previousFilename,
                lines: echo().split(System.lineSeparator())
        ])

        return "Report generated at: ${htmlOutput.canonicalPath}"
    }

    static private class DeltaLine {

        Integer index
        String state
        GraphNavigator.Linkable link
        String owner

    }
}
