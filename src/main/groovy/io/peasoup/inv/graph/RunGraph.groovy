package io.peasoup.inv.graph

import org.jgrapht.Graph
import org.jgrapht.graph.DefaultDirectedGraph
import org.jgrapht.graph.DefaultEdge
import org.jgrapht.io.ComponentNameProvider
import org.jgrapht.io.DOTExporter
import org.jgrapht.io.IntegerComponentNameProvider

class RunGraph {

    final private static lf = System.properties['line.separator']

    final Graph<GraphNavigator.Linkable, DefaultEdge> g
    final GraphNavigator navigator

    final List<FileStatement> files = []

    RunGraph(BufferedReader logs) {

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
                return
            }
        }
    }

    String toPlainList() {
        return g.vertexSet().collect {
            it.value
        }.join(lf)
    }

    String toDotGraph() {
        StringWriter writer = new StringWriter()

        DOTExporter<GraphNavigator.Linkable, DefaultEdge> dotExporter = new DOTExporter<>(
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

    static class RequireStatement implements GraphNavigator.Node {

        private static def RE = /^\[INV\] \[(\S*)\] => \[REQUIRE\] (.*)\u0024/

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

    static class BroadcastStatement implements GraphNavigator.Node {

        private static def RE = /^\[INV\] \[(\S*)\] => \[BROADCAST\] (.*)\u0024/

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

    static class FileStatement {

        private static def RE = /^\[INV\] \[(\S*)\] \[(\S*)\] \[(\S*)\]\u0024/

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
