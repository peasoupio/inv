package io.peasoup.inv.graph

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.transform.ToString
import org.jgrapht.Graph
import org.jgrapht.graph.DefaultDirectedGraph
import org.jgrapht.graph.DefaultEdge
import org.jgrapht.io.ComponentNameProvider
import org.jgrapht.io.DOTExporter
import org.jgrapht.io.IntegerComponentNameProvider

@CompileStatic
class RunGraph {

    final private static String lf = System.properties['line.separator']

    final GraphNavigator navigator
    final List<FileStatement> files = []

    private final Graph<GraphNavigator.Linkable, DefaultEdge> g

    RunGraph(BufferedReader logs) {
        assert logs, 'Logs are required'

        g = new DefaultDirectedGraph<> (DefaultEdge.class)
        navigator = new GraphNavigator(g)

        logs.eachLine { String line ->

            // Don't bother process useless lines
            if (!line.startsWith("[INV]"))
                return

            def broadcast = BroadcastStatement.matches(line)
            if (broadcast) {
                navigator.addBroadcastNode(broadcast)
                return
            }

            def require = RequireStatement.matches(line)
            if (require) {
                navigator.addRequireNode(require)
                return
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

    @CompileDynamic
    @ToString
    static class RequireStatement implements GraphNavigator.Node {

        private static def RE = /^\[INV\] \[(\S*)\] => \[REQUIRE\] (.*)\u0024/

        @SuppressWarnings("GroovyAssignabilityCheck")
        static RequireStatement matches(String line) {
            def require = line =~ RE

            if (!require.matches())
                return null

            return new RequireStatement(
                owner: require[0][1],
                id: require[0][2],
            )
        }

        // Ctor
        String owner
        String id

        private RequireStatement() { }
    }

    @CompileDynamic
    @ToString
    static class BroadcastStatement implements GraphNavigator.Node {

        private static def RE = /^\[INV\] \[(\S*)\] => \[BROADCAST\] (.*)\u0024/

        @SuppressWarnings("GroovyAssignabilityCheck")
        static BroadcastStatement matches(String line) {
            def broadcast = line =~ RE

            if (!broadcast.matches())
                return null

            return new BroadcastStatement(
                owner: broadcast[0][1],
                id: broadcast[0][2],
            )
        }

        // Ctor
        String owner
        String id

        private BroadcastStatement() {}
    }

    @CompileDynamic
    @ToString
    static class FileStatement {

        private static def RE = /^\[INV\] \[(\S*)\] \[(\S*)\] \[(\S*)\]\u0024/

        @SuppressWarnings("GroovyAssignabilityCheck")
        static FileStatement matches(String line) {
            def file = line =~ RE

            if (!file.matches())
                return null

            return new FileStatement(
                scm: file[0][1],
                file: file[0][2],
                inv: file[0][3]
            )
        }

        // Ctor
        String scm
        String file
        String inv

        private FileStatement() {}
    }

}
