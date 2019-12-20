package io.peasoup.inv.web

import io.peasoup.inv.graph.BaseGraph
import io.peasoup.inv.graph.PlainGraph

class Run {

    final private File run
    final private PlainGraph plainGraph
    final private Map<String, PlainGraph.FileStatement> ownerOfScm = [:]
    final private Map<String, PlainGraph.FileStatement> invOfScm = [:]

    final Map<String, Map> selected = [:]

    Run(File run) {
        assert run
        assert run.exists()

        this.run = run
        plainGraph = new PlainGraph(run.newReader())

        ownerOfScm = plainGraph.files.collectEntries { [(it.scm): it.inv]}
        invOfScm = plainGraph.files.collectEntries { [(it.inv): it.scm]}
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

    boolean isSelected(String scm) {
        def node = plainGraph.graph.nodes[ownerOfScm[scm]]

        if (!node)
            return false

        return selected.containsKey(node.id) && selected[node.id].selected
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

}
