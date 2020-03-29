package io.peasoup.inv.graph

import groovy.text.SimpleTemplateEngine
import groovy.transform.CompileStatic
import groovy.transform.ToString
import io.peasoup.inv.run.InvInvoker
import io.peasoup.inv.run.RunsRoller

@CompileStatic
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

        for(GraphNavigator.Linkable link : baseGraph.navigator.links()) {

            def linksNode = baseGraph.navigator.nodes[link.value]
            assert linksNode, "Link's node cannot be null"

            // Process state
            if (otherGraph.navigator.contains(link)) {
                deltaLines << new DeltaLine(state: '=', link: link, owner: linksNode)
            } else {

                def fileStatement = baseGraph.files.find { it.inv == linksNode }

                if (fileStatement && fileStatement.scm != InvInvoker.UNDEFINED_SCM)
                    deltaLines << new DeltaLine(state: '-', link: link, owner: linksNode)
                else
                    deltaLines << new DeltaLine(state: 'x', link: link, owner: linksNode)
            }
        }

        for(GraphNavigator.Linkable link : otherGraph.navigator.links()) {

            def linksNode = otherGraph.navigator.nodes[link.value]
            assert linksNode, "Link's node cannot be null"


            // Process state
            def fileStatement = otherGraph.files.find { it.inv == linksNode.owner }
            if (!fileStatement || fileStatement.scm == InvInvoker.UNDEFINED_SCM) {

                // Check if a deltaLine was added from base when a correspondence was found in other.
                // In that case, if other as not defined an SCM, it must be removed.
                def alreadyProcessed = deltaLines.find { it.owner == linksNode && it.state == '=' }
                if (alreadyProcessed)
                    alreadyProcessed.state = 'x'
                else
                    deltaLines << new DeltaLine(state: 'x', link: link, owner: linksNode)

                return
            }

            if (!baseGraph.navigator.contains(link)) {
                deltaLines << new DeltaLine(state: '+', link: link, owner: linksNode)
            }
        }
    }

    String echo() {
        // Regex rule:^(?'state'\\W) (?!\\#.*\$)(?'require'.*) -> (?'broadcast'.*) \\((?'id'.*)\\)\$
        return """${
    // Shared nodes and edges
    deltaLines
        .sort { it.owner.index }
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


    StringBuilder merge() {
        StringBuilder builder = new StringBuilder()

        Map<String, RunGraph.FileStatement> files = baseGraph.files.collectEntries {[(it.inv): it]}
        otherGraph.files.each {
            files.put(it.inv, it)
        }

        // Process lines
        List<DeltaGraph.DeltaLine> approuvedLines =  deltaLines.findAll { DeltaGraph.DeltaLine line -> line.state != 'x' } // get non removed lines

        // Get scm for lines
        List<RunGraph.FileStatement> approuvedFiles = approuvedLines
                .findAll { it.link.isOwner() }
                .collect { files[it.owner.owner] }

        // Write files
        approuvedFiles.each { RunGraph.FileStatement fileStatement ->
            builder.append "[INV] [${fileStatement.scm}] [${fileStatement.file}] [${fileStatement.inv}]${System.lineSeparator()}"
        }

        // Write broadcast lines
        def nodes = approuvedLines
                .findAll { it.link.isOwner() }
                .collect { otherGraph.virtualInvs[it.owner.owner] ?: baseGraph.virtualInvs[it.owner.owner]}
                .collectMany { it.nodes } as List<GraphNavigator.Node>

        nodes
                .sort { GraphNavigator.Node node -> node.index }
                .each { GraphNavigator.Node node ->

                    if (node instanceof RunGraph.BroadcastStatement)
                        builder.append "[INV] [${node.owner}] => [BROADCAST] ${node.id}${System.lineSeparator()}"
                    else
                        builder.append "[INV] [${node.owner}] => [REQUIRE] ${node.id}${System.lineSeparator()}"
                }

        builder.append "# file(s): ${approuvedFiles.size()}, broadcast(s): ${approuvedLines.size()}"

        return builder
    }

    @ToString
    static private class DeltaLine {

        String state
        GraphNavigator.Linkable link
        GraphNavigator.Node owner

    }
}
