package io.peasoup.inv.web

import groovy.json.JsonOutput
import io.peasoup.inv.graph.BaseGraph
import io.peasoup.inv.graph.PlainGraph

class Run {

    final File run
    final PlainGraph plainGraph

    String cache

    Run(File run) {
        assert run
        assert run.exists()

        this.run = run

        plainGraph = new PlainGraph(run.newReader())
    }

    String toJson() {
        if (cache)
            return cache

        def flattenedEdges = plainGraph.baseGraph.edges.collectEntries { String owner, Set<BaseGraph.Node> _nodes ->
            Closure<Set<BaseGraph.Node>> recursive
            recursive = { Set<BaseGraph.Node> nodes ->

                if (nodes.isEmpty())
                    return []

                return nodes.collectMany { BaseGraph.Node node ->
                    def myNodes = plainGraph.baseGraph.edges[node.owner]

                    if (myNodes.isEmpty())
                        return [node]

                    return [node] + recursive.call(myNodes)
                }

            }

            return [(owner): recursive(_nodes)]
        }


        def output = [
            graph: plainGraph.baseGraph,
            files: plainGraph.files,
            flattenedEdges: flattenedEdges
        ]

        cache = JsonOutput.toJson(output)

        return cache
    }




    /*
    private void process() {
        def reader = run.newReader()
        String line

        Map<String, List<InvStatement>> tmp = [:]

        while((line = reader.readLine()) != null) {
            if (!line)
                continue

            def matches = line =~ re

            if (!matches.matches())
                continue

            String id = matches[0][3]

            if (!id || id.contains("undefined"))
                id = "undefined"

            // From require perspective
            def inv1 = new InvStatement(
                state: "",//matches[0][1],
                beneficiary: matches[0][1],
                giver: matches[0][2],
                id: id,
                children: [],
                parent: null,
                sort: ""
            )

            // From broadcast perspective
            def inv2 = new InvStatement(
                    state: "",//matches[0][1],
                    beneficiary: matches[0][2],
                    giver: "",
                    id: id,
                    children: [],
                    parent: null,
                    sort: ""
            )

            if (!tmp[inv1.beneficiary])
                tmp[inv1.beneficiary] = []

            tmp[inv1.beneficiary] << inv1

            if (!tmp[inv2.beneficiary])
                tmp[inv2.beneficiary] = []

            tmp[inv2.beneficiary] << inv2
        }

        tmp.each { String beneficiary, List<InvStatement> statements ->

            if (!invs.containsKey(beneficiary))
                invs[beneficiary] = [:]

            invs[beneficiary] = statements.groupBy { it.id }
        }

        def flattened = invs.values().collectMany { it.collectMany { it.value } }
        def results =  flattened.each { a ->
            flattened.each { b ->
                greatestAncestor(b, a)
            }
        }

        println results.collect { "${it.beneficiary}: ${it.index()}"}
    }


    private Integer greatestAncestor (InvStatement parent, InvStatement child) {

        // Same breed
        if (child.beneficiary == parent.beneficiary)
            return 0

        if (child.giver != parent.beneficiary)
            return 0

        // Already has a greatest ancestor than parent
        if (child.index() -1 == parent.index())
            return 0

        // Has same ancestry "level"
        if (child.index() -1 > parent.index())
            return -1

        // Detach from previous parent
        if (child.parent)
            child.parent.children.remove(child)

        parent.children << child
        child.parent = parent

        return 1
    }

    static class InvStatement {
        String state
        String beneficiary
        String giver
        String id
        List children
        InvStatement parent
        String sort

        Integer index() {
            if (!parent)
                return 0

            return parent.index() + 1
        }

    }

     */

    /*
    var ;

            lines.forEach(function callback(it) {
                var matches = re.exec(it);

                if (matches === null) {
                    return;
                }

                var inv = {
                    state: matches[1],
                    require: matches[2],
                    broadcast: matches[3],
                    id: matches[4],
                    index: -1,
                    children: [],
                    parent: null,
                    sort: ""
                }

                if (vm.all[inv.require] === undefined) {
                    vm.all[inv.require] = []
                }
                vm.all[inv.require].push(inv)
            })

            var resolved = function(require, broadcast) {
                if (require.parent !== null) {

                    var index = require.parent.children.indexOf(require.require);
                    if (index > -1) {
                        require.parent.children.splice(index, 1)
                    }
                }

                require.index = broadcast.index + 1
                require.parent = broadcast
                broadcast.children.push(require.require)
            }

            // Resolve level of invs
            for (var key in vm.all) {
                var value = vm.all[key];

                value.forEach(function callback(it) {
                    (function resolver (require, data) {
                        if (data.index > -1) {
                            // Already resolved
                            return;
                        }

                        if (data.broadcast === "not-existing") {
                            // Not existing
                            return;
                        }

                        if (vm.all[data.broadcast] === undefined) {
                            var notExisting = {
                                state: data.state,
                                require: data.broadcast,
                                broadcast: "not-existing",
                                id: data.id,
                                index: -1,
                                children: [],
                                parent: null,
                                sort: ""
                            }

                            if (vm.all[data.broadcast] === undefined) {
                                vm.all[data.broadcast] = []
                            }

                            vm.all[data.broadcast].push(notExisting)

                            resolved(data, notExisting)

                            return
                        }

                        var broadcasts = vm.all[data.broadcast]

                        broadcasts.forEach(function callback(broadcast) {
                            if (broadcast.index < 0) {
                                resolver(data.broadcast, broadcast)
                            }

                            if (broadcast.index + 1 > data.index) {
                                resolved(data, broadcast)
                            }
                        })


                    })(key, it)
                })
            }

            var index = {}
            for (var key in vm.all) {
                var value = vm.all[key];

                value.forEach(function callback(inv) {

                    // get sorting data
                    var parent = inv.parent
                    while(parent !== null) {
                        inv.sort = parent.require + ">" + inv.sort
                        parent = parent.parent
                    }

                    inv.sort += inv.require

                    if (index[inv.require] === undefined) {
                        index[inv.require] = inv
                        return
                    }

                    if (index[inv.require].index >= inv.index) {
                        return
                    }

                    index[inv.require] = inv
                })
            }

            vm.preSorted = Object
                .values(index)
                .sort(function (a, b) {
                    return ('' + a.sort).localeCompare(b.sort);
                })

            vm.total = vm.preSorted.length
     */
}
