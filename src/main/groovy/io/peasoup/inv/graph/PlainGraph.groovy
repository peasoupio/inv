package io.peasoup.inv.graph

import java.util.regex.Matcher

class PlainGraph {

    final private static lf = System.properties['line.separator']

    Map nodes = [:]
    Map edges = [:]

    PlainGraph(BufferedReader logs) {

        logs.eachLine { String line ->

            if (!line.startsWith("[INV]"))
                return

            Matcher broadcast = line =~ /\[INV\] \[(.*)\] => \[BROADCAST\] (.*)/
            Matcher require = line =~ /\[INV\] \[(.*)\] => \[REQUIRE\] (.*)/

            if (broadcast.matches()) {
                String name = broadcast.group(1)

                def node = [
                    owner: name,
                    broadcast: broadcast.group(2)
                ]

                // Add or override existing node
                if (!nodes.containsKey(name))
                    nodes << [(name): new HashSet<>()]

                nodes[name] << node
            }

            if (require.matches()) {
                String name = require.group(1)

                def node = [
                    owner: name,
                    require: require.group(2)
                ]

                // Add or override existing node
                if (!nodes.containsKey(name))
                    nodes << [(name): new HashSet<>()]

                nodes[name] << node

            }
        }

        // Get all broadcasts
        List broadcasts = nodes.values().collectMany { it.findAll { it.broadcast } }
        // Get all requires
        List requires = nodes.values().collectMany { it.findAll { it.require } }

        // Merge broadcasts them into an index
        Map broadcasts_index = broadcasts.collectEntries { [(it.broadcast): it]}

        // Create edges
        (broadcasts + requires).each {
            if (edges.containsKey(it.owner))
                return

            edges << [(it.owner): new HashSet<>()]
        }

        // Look requires to get matches with broadcasts
        requires.each {
            def match = broadcasts_index[it.require]

            if (!match)
                return

            edges[match.owner] << it
        }

    }

    String print() {
        print """    
# Regex rule:^(?!\\#.*\$)(?'giver'.*) -> (?'receiver'.*) \\((?'edge'.*)\\)\$
${
    edges
        .collectMany { String owner, Set edges ->
            edges.collect { Map edge ->
                "${owner} -> ${edge.owner} (${edge.require})"
            }
        }
        .join(lf)
}"""
    }

}
