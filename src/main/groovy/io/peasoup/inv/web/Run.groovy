package io.peasoup.inv.web

import io.peasoup.inv.graph.BaseGraph
import io.peasoup.inv.graph.PlainGraph

class Run {

    final private File run
    final private PlainGraph plainGraph

    final Map<String, Map> selected = [:]

    Run(File run) {
        assert run
        assert run.exists()

        this.run = run
        plainGraph = new PlainGraph(run.newReader())
    }

    synchronized void stage(String id) {
        selected.put(id, [
            selected: true,
            link: new BaseGraph.Id(value: id)
        ])

        propagate()
    }

    synchronized void unstage(String id) {
        selected.remove(id)

        propagate()
    }
    private  void propagate() {

        def checkRequiresByAll = selected.findAll { it.value.selected }

        selected.clear()

        for(Map.Entry<String, Map> chosen : checkRequiresByAll) {
            selected.put(chosen.key, chosen.value)

            for(BaseGraph.Linkable link : plainGraph.graph.requiresAll(chosen.value.link)) {
                if (selected.containsKey(link.value))
                    continue

                selected.put(link.value, [
                    required: true,
                    link: link
                ])
            }
        }
    }

    Map getNodes(String source = "all", Map filter = [:], Integer from = 0, Integer to = 20) {

        List<BaseGraph.Linkable> links

        switch (source) {
            case "all":
                links = (plainGraph.graph.g.vertexSet() as List).findAll { it.isId() }
                break

            case "selected":
                links = selected.values().collect { it.link }.findAll { it.isId() }
                break
        }

        List<Map> reduced = links
                .findAll { !filter.id || it.value.contains(filter.id)}
                .collect{[
                    link: it,
                    node: plainGraph.graph.nodes[it.value],
                    name: it.value.split(' ')[0].replace('[', '').replace(']', ''),
                    subId: it.value.split(' ')[1].replace('[', '').replace(']', '')
                ]}
                .findAll { it.node }

        if (filter.owner)
            reduced = reduced.findAll { it.node.owner.contains(filter.owner)}

        if (filter.name)
            reduced = reduced.findAll { it.name.contains(filter.name)}


        if (reduced.size() > to)
            reduced = reduced[from..to - 1]

        return [
            count: reduced.size(),
            total: links.size(),
            selected: selected.values().findAll { it.link.isId() }.size(),
            requiredByAssociation: selected.values().findAll { !it }.size(),
            nodes: reduced.collect {

                    String value = it.link.value as String
                    String owner = it.node.owner as String

                    def scm = ''

                    //TODO NOT PERFORMANT!!
                    plainGraph.files.each { PlainGraph.FileStatement file ->
                        if (file.inv != owner)
                            return

                        scm = file.scm
                    }

                    return [
                        required: selected[value] && selected[value].required,
                        selected: selected[value] && selected[value].selected,
                        //broughtBySomeone: new HashSet<String>(),
                        owner: owner,
                        name: it.name,
                        id: it.subId,
                        scm: scm,
                        links: [
                            requiredBy: "/run/requiredBy?id=${value}",
                            stage: "/run/stage?id=${value}",
                            unstage: "/run/unstage?id=${value}"
                        ]
                        //required: flattenedEdges[owner]
                    ]
                },
            links: [
                search: "/run",
                selected: "/run/selected"
            ]
        ]
    }

    Map getRequiredBy(String id) {

        def nodes = []
        def linkableToCheck = selected.find { it.value.link.value == id }

        if (linkableToCheck) {
            for (BaseGraph.Linkable link : plainGraph.graph.requiredByAll(linkableToCheck.value.link)) {
                if (!link.isOwner())
                    continue

                String value = link.value

                if (!selected.containsKey(value))
                    continue

                def node = plainGraph.graph.nodes[value]

                String name = node.id.split(' ')[0].replace('[', '').replace(']', '')
                String subId = node.id.split(' ')[1].replace('[', '').replace(']', '')

                nodes.add([
                    name: name,
                    id: subId,
                    owner: node.owner,
                    required: selected[node.id] && selected[node.id].required,
                    selected: selected[node.id] && selected[node.id].selected,
                ])
            }
        }

        return [
            nodes: nodes
        ]

    }

    /*
    return [(owner): [
                chosen: false,
                broughtBySomeone: new HashSet<String>(),
                owner: owner,
                name: name,
                id: id,
                scm: scm,
                required: flattenedEdges[owner]
            ]]
     */

    /*
    var matchedBroadcast = ''

        data.graph.broadcasts.forEach(function(broadcast) {
            if (broadcast.owner != owner)
                return

            matchedBroadcast = broadcast.id
        });

        var matchedSCM = ''




        var name = ''
        var id = ''

        if (matchedBroadcast != '') {
           name = matchedBroadcast.split(' ')[0].replace('[', '').replace(']', '')
           id = matchedBroadcast.split(' ')[1].replace('[', '').replace(']', '')
        }


        vm.value.availables.push({
            chosen: false,
            broughtBySomeone: false,
            owner: owner,
            name: name,
            id: id,
            scm: matchedSCM,
            required: data.flattenedEdges[owner]
        })
     */

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
