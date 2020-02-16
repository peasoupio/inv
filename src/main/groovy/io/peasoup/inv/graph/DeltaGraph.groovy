package io.peasoup.inv.graph

import groovy.text.SimpleTemplateEngine
import io.peasoup.inv.run.InvInvoker
import io.peasoup.inv.run.RunsRoller

class DeltaGraph {
    final private static String lf = System.properties['line.separator']

    final RunGraph baseGraph
    final RunGraph otherGraph
    final List<DeltaLine> deltaLines = []

    DeltaGraph(BufferedReader base, BufferedReader other) {
        assert base != null, 'Base (reader) is required'
        assert other != null, 'Other (reader) is required'

        baseGraph = new RunGraph(base)
        otherGraph = new RunGraph(other)

        Map<String, Integer> baseIndexes = baseGraph.navigator.nodes.keySet().withIndex().collectEntries()
        Map<String, Integer> otherIndexes = otherGraph.navigator.nodes.keySet().withIndex().collectEntries()

        for(GraphNavigator.Linkable link : baseGraph.navigator.links()) {

            if (link.isOwner())
                continue

            // Get properties
            Integer index = baseIndexes[link.value]
            assert index, 'Index is required for link'

            def linksNode = baseGraph.navigator.nodes[link.value]
            assert linksNode, "Link's node cannot be null"

            def linksOwner = linksNode.owner
            assert linksOwner, "Link's node owner cannot be null or empty"

            // Process state
            if (otherGraph.navigator.contains(link)) {
                deltaLines << new DeltaLine(index: index, state: '=', link: link, owner: linksOwner)
            } else {

                def fileStatement = baseGraph.files.find { it.inv == linksOwner }

                if (fileStatement && fileStatement.scm != InvInvoker.UNDEFINED_SCM)
                    deltaLines << new DeltaLine(index: index, state: '-', link: link, owner: linksOwner)
                else
                    deltaLines << new DeltaLine(index: index, state: 'x', link: link, owner: linksOwner)
            }
        }

        for(GraphNavigator.Linkable link : otherGraph.navigator.links()) {

            if (link.isOwner())
                continue

            // Get properties
            Integer index = otherIndexes[link.value]
            assert index, 'Index is required for link'

            def linksNode = otherGraph.navigator.nodes[link.value]
            assert linksNode, "Link's node cannot be null"

            def linksOwner = linksNode.owner
            assert linksOwner, "Link's node owner cannot be null or empty"

            // Process state
            def fileStatement = otherGraph.files.find { it.inv == linksOwner }
            if (!fileStatement || fileStatement.scm == InvInvoker.UNDEFINED_SCM) {

                // Check if a deltaLine was added from base when a correspondence was found in other.
                // In that case, if other as not defined an SCM, it must be removed.
                def alreadyProcessed = deltaLines.find { it.owner == linksOwner && it.state == '=' }
                if (alreadyProcessed)
                    alreadyProcessed.state = 'x'
                else
                    deltaLines << new DeltaLine(index: index, state: 'x', link: link, owner: linksOwner)

                return
            }

            if (!baseGraph.navigator.contains(link)) {
                deltaLines << new DeltaLine(index: index, state: '+', link: link, owner: linksOwner)
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
