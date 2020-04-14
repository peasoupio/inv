package io.peasoup.inv.graph


import groovy.transform.CompileStatic
import groovy.transform.ToString
import org.jgrapht.Graph
import org.jgrapht.graph.DefaultDirectedGraph
import org.jgrapht.graph.DefaultEdge
import org.jgrapht.io.ComponentNameProvider
import org.jgrapht.io.DOTExporter
import org.jgrapht.io.IntegerComponentNameProvider

import java.util.regex.Pattern

@CompileStatic
class RunGraph {
    final private static String lf = System.properties['line.separator']

    final GraphNavigator navigator

    final Map<String, VirtualInv> virtualInvs = [:]
    final List<FileStatement> files = []

    private final Graph<GraphNavigator.Linkable, DefaultEdge> g

    RunGraph(BufferedReader logs) {
        assert logs, 'Logs are required'

        g = new DefaultDirectedGraph<> (DefaultEdge.class)
        navigator = new GraphNavigator(g)

        long nodeCount = 0

        String line
        while((line = logs.readLine()) != null) {

            // Don't bother process useless lines
            if (!line.startsWith("[INV]"))
                continue

            def broadcast = BroadcastStatement.matches(line)
            if (broadcast) {
                navigator.addBroadcastNode(broadcast)

                broadcast.index = nodeCount++

                String virtualInvName = broadcast.owner
                VirtualInv virtualInv = virtualInvs[virtualInvName]
                if (!virtualInv) {
                    virtualInv = new VirtualInv(virtualInvName)
                    virtualInvs.put(virtualInvName, virtualInv)
                }

                virtualInv.nodes << broadcast

                continue
            }

            def require = RequireStatement.matches(line)
            if (require) {
                navigator.addRequireNode(require)

                require.index = nodeCount++

                String virtualInvName = require.owner
                VirtualInv virtualInv = virtualInvs[virtualInvName]
                if (!virtualInv) {
                    virtualInv = new VirtualInv(virtualInvName)
                    virtualInvs.put(virtualInvName, virtualInv)
                }

                virtualInv.nodes << require

                continue
            }

            def file = FileStatement.matches(line)
            if (file) {
                files << file
            }
        }
    }

    String toPlainList() {
        return navigator.links().collect {
            it.value
        }.join(lf)
    }

    String toDotGraph() {
        StringWriter writer = new StringWriter()

        DOTExporter<GraphNavigator.Linkable, DefaultEdge> dotExporter = new DOTExporter<GraphNavigator.Linkable, DefaultEdge>(
                new IntegerComponentNameProvider<GraphNavigator.Linkable>(),
                new ComponentNameProvider<GraphNavigator.Linkable>() {
                    String getName(GraphNavigator.Linkable linkable) {
                        return linkable.value
                    }
                },
                null
        )

        dotExporter.exportGraph(g, writer)

        return writer.toString()
    }

    static class VirtualInv {

        final String name
        final List<GraphNavigator.Node> nodes = []

        VirtualInv(String name) {
            this.name = name
        }
    }

    @ToString
    static class RequireStatement implements GraphNavigator.Node {

        private static Pattern RE = Pattern.compile(/^\[INV\] \[(\S*)\] => \[REQUIRE\] (.*)\u0024/)

        @SuppressWarnings("GroovyAssignabilityCheck")
        static RequireStatement matches(String line) {
            def require = RE.matcher(line)
            if (!require.matches())
                return null

            return new RequireStatement(
                owner: require.group(1),
                id: require.group(2),
            )
        }

        // Ctor
        String owner
        String id
        long index

        private RequireStatement() { }
    }

    @ToString
    static class BroadcastStatement implements GraphNavigator.Node {

        private static Pattern RE = Pattern.compile(/^\[INV\] \[(\S*)\] => \[BROADCAST\] (.*)\u0024/)

        @SuppressWarnings("GroovyAssignabilityCheck")
        static BroadcastStatement matches(String line) {
            def broadcast = RE.matcher(line)
            if (!broadcast.matches())
                return null

            return new BroadcastStatement(
                owner: broadcast.group(1),
                id: broadcast.group(2)
            )
        }

        // Ctor
        String owner
        String id
        long index

        private BroadcastStatement() {}
    }

    @ToString
    static class FileStatement {

        private static Pattern RE = Pattern.compile(/^\[INV\] \[(\S*)\] \[(\S*)\] \[(\S*)\]\u0024/)

        @SuppressWarnings("GroovyAssignabilityCheck")
        static FileStatement matches(String line) {
            def file = RE.matcher(line)
            if (!file.matches())
                return null

            return new FileStatement(
                scm: file.group(1),
                file: file.group(2),
                inv: file.group(3)
            )
        }

        // Ctor
        String scm
        String file
        String inv

        private FileStatement() {}
    }

}
