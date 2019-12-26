package io.peasoup.inv.web

import io.peasoup.inv.graph.GraphNavigator
import io.peasoup.inv.graph.RunGraph

class Run {

    final private File runFile
    final private RunGraph runGraph
    final private List<GraphNavigator.Linkable> totalLinks = []

    final private Map<String, RunGraph.FileStatement> ownerOfScm = [:]
    final private Map<String, RunGraph.FileStatement> invOfScm = [:]

    final Map<String, Map> selected = [:]

    Run(File runFile) {
        assert runFile
        assert runFile.exists()

        this.runFile = runFile
        runGraph = new RunGraph(runFile.newReader())

        ownerOfScm = runGraph.files.groupBy { it.scm }.collectEntries { [(it.key): it.value.collect { it.inv} ]}
        invOfScm = runGraph.files.collectEntries { [(it.inv): it.scm]}

        totalLinks = (runGraph.g.vertexSet() as List).findAll { it.isId() }
    }

    synchronized void stage(String id) {
        selected.put(id, [
            selected: true,
            link: new GraphNavigator.Id(value: id)
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

            for(GraphNavigator.Linkable link : runGraph.navigator.requiresAll(chosen.value.link)) {
                if (selected.containsKey(link.value))
                    continue

                selected.put(link.value, [
                    required: true,
                    link: link
                ])
            }
        }
    } // traneiakits/pise.git

    boolean isSelected(String scm) {
        return ownerOfScm[scm].any {
            def node = runGraph.navigator.nodes[it]

            if (!node)
                return false

            return selected.containsKey(node.id) && selected[node.id].selected
        }
    }

    Map getNodes(Map filter = [:], Integer from = 0, Integer step = 20) {

        List<GraphNavigator.Linkable> links = totalLinks
        List<GraphNavigator.Linkable> selectedLinks = selected.values().findAll { it.selected }.collect { it.link }
        List<GraphNavigator.Linkable> requiredLinks = selected.values().collect { it.link }.findAll { it.isId() }

        if (filter.selected)
            links = selectedLinks

        if (filter.required)
            links = requiredLinks

        List<Map> reduced = links
                .findAll { GraphNavigator.Linkable link ->  !filter.id || link.value.contains(filter.id)}
                .collect{ GraphNavigator.Linkable link -> [
                    link: link,
                    node: runGraph.navigator.nodes[link.value],
                    name: link.value.split(' ')[0].replace('[', '').replace(']', ''),
                    subId: link.value.split(' ')[1].replace('[', '').replace(']', '')
                ]}
                .findAll { it.node }

        if (filter.owner)
            reduced = reduced.findAll { it.node.owner.contains(filter.owner)}

        if (filter.name)
            reduced = reduced.findAll { it.name.contains(filter.name)}

        Integer total = reduced.size()

        if (total > from) {
            def top = Math.min(total, from + step)
            reduced = reduced[from..top - 1]
        }

        return [
            count: total,
            total: totalLinks.size(),
            selected: selectedLinks.size(),
            requiredByAssociation: requiredLinks.size(),
            nodes: reduced.collect {

                    String value = it.link.value as String
                    String owner = it.node.owner as String

                    def scm = invOfScm[owner]

                    return [
                        required: selected[value] && selected[value].required,
                        selected: selected[value] && selected[value].selected,
                        //broughtBySomeone: new HashSet<String>(),
                        owner: owner,
                        name: it.name,
                        id: it.subId,
                        scm: scm,
                        links: [
                            scm: "/scm/view?name=${scm}",
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
            for (GraphNavigator.Linkable link : runGraph.navigator.requiredByAll(linkableToCheck.value.link)) {
                if (!link.isOwner())
                    continue

                String value = link.value

                if (!selected.containsKey(value))
                    continue

                def node = runGraph.navigator.nodes[value]

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

}
