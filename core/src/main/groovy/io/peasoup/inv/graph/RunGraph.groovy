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
    final Map<String, Map<String, List<VirtualInv>>> tags = [:]

    private final Graph<GraphNavigator.Linkable, DefaultEdge> g

    RunGraph(BufferedReader logs) {
        assert logs, 'Logs are required'

        g = new DefaultDirectedGraph<GraphNavigator.Linkable, DefaultEdge>(DefaultEdge.class)
        navigator = new GraphNavigator(g)

        long nodeCount = 0

        String line
        while((line = logs.readLine()) != null) {

            // Don't bother process useless lines
            if (!line.startsWith("[INV]"))
                continue

            def broadcastStatement = BroadcastStatement.matches(line)
            if (broadcastStatement) {
                broadcastStatement.index = nodeCount++

                VirtualInv virtualInv = resolveVirtualInv(broadcastStatement.owner)
                virtualInv.nodes.add(broadcastStatement)

                navigator.addBroadcastNode(broadcastStatement)

                continue
            }

            def requireStatement = RequireStatement.matches(line)
            if (requireStatement) {
                requireStatement.index = nodeCount++

                VirtualInv virtualInv = resolveVirtualInv(requireStatement.owner)
                virtualInv.nodes.add(requireStatement)

                navigator.addRequireNode(requireStatement)

                continue
            }

            def fileStatement = FileStatement.matches(line)
            if (fileStatement) {
                files << fileStatement
                continue
            }

            def tagsStatement = TagsStatement.matches(line)
            if (tagsStatement) {
                VirtualInv virtualInv = resolveVirtualInv(tagsStatement.owner)
                virtualInv.tags = tagsStatement.tags

                for(Map.Entry<String, String> tag : tagsStatement.tags) {
                    tags.putIfAbsent(tag.key, [:])
                    tags.get(tag.key).putIfAbsent(tag.value, [])
                    tags.get(tag.key).get(tag.value).add(virtualInv)
                }
            }
        }

        logs.close()
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

    private VirtualInv resolveVirtualInv(String virtualInvName) {
        VirtualInv virtualInv = virtualInvs[virtualInvName]
        if (!virtualInv) {
            virtualInv = new VirtualInv(virtualInvName)
            virtualInvs.put(virtualInvName, virtualInv)
        }

        return virtualInv
    }

    static class VirtualInv {

        final String name
        final List<GraphNavigator.Node> nodes = []
        Map<String, String> tags = [:]

        VirtualInv(String name) {
            this.name = name
        }
    }

    @ToString
    static class RequireStatement implements GraphNavigator.Node {

        private static Pattern RE = Pattern.compile(/^\[INV\] \[(\S*)\] => \[REQUIRE\] (.*)\u0024/)

        @SuppressWarnings("GroovyAssignabilityCheck")
        static RequireStatement matches(String line) {
            def requireMatcher = RE.matcher(line)
            if (!requireMatcher.matches())
                return null

            return new RequireStatement(
                owner: requireMatcher.group(1),
                id: requireMatcher.group(2),
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
            def broadcastMatcher = RE.matcher(line)
            if (!broadcastMatcher.matches())
                return null

            return new BroadcastStatement(
                owner: broadcastMatcher.group(1),
                id: broadcastMatcher.group(2)
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
            def fileMatcher = RE.matcher(line)
            if (!fileMatcher.matches())
                return null

            return new FileStatement(
                repo: fileMatcher.group(1),
                file: fileMatcher.group(2),
                inv: fileMatcher.group(3)
            )
        }

        // Ctor
        String repo
        String file
        String inv

        private FileStatement() {}
    }

    @ToString
    static class TagsStatement {

        private static Pattern RE = Pattern.compile(/^\[INV\] \[(\S*)\] => \[TAGS\] (.*)\u0024/)

        @SuppressWarnings("GroovyAssignabilityCheck")
        static TagsStatement matches(String line) {
            def tagsMatcher = RE.matcher(line)
            if (!tagsMatcher.matches())
                return null

            String[] rawTags = tagsMatcher.group(2)
                    .replaceAll('[\\[\\]]', '')
                    .split(',')

            Map<String, String> tags = [:]
            for(String rawTag : rawTags) {
                // Do not process ags without : character
                if (rawTag.lastIndexOf(':') < 0)
                    continue

                String[] keypair = rawTag.split(':')
                tags.put(keypair[0], keypair[1])
            }

            return new TagsStatement(
                    owner: tagsMatcher.group(1),
                    tags: tags
            )
        }

        // Ctor
        String owner
        Map<String, String> tags

        private TagsStatement() {}
    }

}
