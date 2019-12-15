package io.peasoup.inv.graph

class PlainGraph {

    final private static lf = System.properties['line.separator']

    def baseGraph = new BaseGraph()

    final List<FileStatement> files = []

    PlainGraph(BufferedReader logs) {

        logs.eachLine { String line ->

            // Don't bother process useless lines
            if (!line.startsWith("[INV]"))
                return

            def broadcast = BroadcastStatement.matches(line)
            if (broadcast) {
                baseGraph.addBroadcastNode(broadcast)
                return
            }

            def require = RequireStatement.matches(line)
            if (require) {
                baseGraph.addRequireNode(require)
                return
            }

            def file = FileStatement.matches(line)
            if (file) {
                files << file
                return
            }
        }

        baseGraph.linkEdges()
    }

    String echo() {
        return """    
# Regex rule:^(?!\\#.*\$)(?'require'.*) -> (?'broadcast'.*) \\((?'id'.*)\\)\$
${
    baseGraph.edges
        .collectMany { String owner, Set edges ->
            edges.collect { BaseGraph.Node edge ->
                "${owner} -> ${edge.owner} (${edge.id})"
            }
        }
        .join(lf)
}
"""
    }

    static class RequireStatement implements BaseGraph.Node {

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

    static class BroadcastStatement implements BaseGraph.Node {

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
