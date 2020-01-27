package io.peasoup.inv.composer

import groovy.transform.CompileStatic
import io.peasoup.inv.graph.GraphNavigator
import io.peasoup.inv.graph.RunGraph

@CompileStatic
class RunFile {

    final private File runFile
    final private RunGraph runGraph


    final private Map<String, List<String>> ownerOfScm
    final private Map<String, RunGraph.FileStatement> invOfScm

    final Map<String, Selected> selected = [:]
    final List<GraphNavigator.Linkable> nodes

    final Map<String, List<GraphNavigator.Id>> owners
    final Map<String, List<GraphNavigator.Id>> names

    RunFile(File runFile) {
        assert runFile != null, 'Run file is required'
        assert runFile.exists(), 'Run file must exist on filesystem'

        this.runFile = runFile
        runGraph = new RunGraph(runFile.newReader())

        ownerOfScm = runGraph.files.groupBy { it.scm }.collectEntries { [(it.key): it.value.collect { it.inv} ]} as Map<String, List<String>>
        invOfScm = runGraph.files.collectEntries { [(it.inv): it.scm] }

        nodes = (runGraph.g.vertexSet() as List<GraphNavigator.Linkable>).findAll { it.isId() }

        owners = nodes.groupBy { runGraph.navigator.nodes[it.value].owner } as Map<String, List<GraphNavigator.Id>>
        names = nodes.groupBy { it.value.split(' ')[0].replace('[', '').replace(']', '') } as Map<String, List<GraphNavigator.Id>>
    }

    synchronized void stage(String id) {
        assert id, 'Id is required'

        if (selected.containsKey(id))
            return

        selected.put(id, new Selected(
                selected: true,
                link: new GraphNavigator.Id(value: id)
        ))

        propagate()
    }

    synchronized void stageWithoutPropagate(String id) {
        assert id, 'Id is required'

        if (selected.containsKey(id))
            return

        selected.put(id, new Selected(
                selected: true,
                link: new GraphNavigator.Id(value: id)
        ))
    }

    synchronized void stageAll() {
        nodes.each { GraphNavigator.Linkable link ->
            selected.put(link.value, new Selected(
                    selected: true,
                    link: link
            ))
        }
    }

    synchronized void unstage(String id) {
        assert id, 'Id is required'

        if (!selected.containsKey(id))
            return

        selected.remove(id)

        propagate()
    }

    synchronized void unstageAll() {
        selected.clear()
    }

    /**
     * Iterate through the nodes to get the required ones by association
     * @return Number of required added
     */
    synchronized Map propagate() {

        def checkRequiresByAll = selected
                .findAll { it.value.selected }

        Map<String, Integer> output = [
            all: selected.size() == nodes.size(),
            checked: checkRequiresByAll.size(),
            added: 0,
            skipped: 0
        ] as Map<String, Integer>

        // (Re)init with selected only
        selected.clear()
        selected.putAll(checkRequiresByAll)

        // Are all nodes required ?
        if (output.all)
            return output

        def checkRequiresByAllList = checkRequiresByAll.entrySet().toList()

        while(!checkRequiresByAllList.isEmpty()) {
            def chosen = checkRequiresByAllList.pop()

            def links = runGraph.navigator.requiresAll(chosen.value.link as GraphNavigator.Linkable)

            if (!links) {
                println "${chosen.value.link.value} is not a valid id"
                continue
            }

            for(GraphNavigator.Linkable link : links.keySet()) {

                if (selected.containsKey(link.value)) {

                    // If already selected, remove from queue
                    if (checkRequiresByAllList.removeAll { it.key == link.value})
                        output.skipped++

                    continue
                }

                selected.put(link.value, new Selected(
                        required: true,
                        link: link
                ))

                if (link.isId())
                    output.added++
            }
        }

        return output
    }

    boolean isSelected(String scm) {
        if (!ownerOfScm.containsKey(scm))
            return false

        return ownerOfScm[scm].any { String inv ->
            def node = runGraph.navigator.nodes[inv] as GraphNavigator.Node

            if (!node)
                return false

            return selected.containsKey(node.id) && selected[node.id].selected
        }
    }

    List<String> selectedScms() {
        return selected.values()
                .collect { runGraph.navigator.nodes[it.link.value] }
                .findAll { it }
                .collect { invOfScm[it.owner] }
                .findAll { it }
                .unique() as List<String>
    }

    Map nodesToMap(Map filter = [:], Integer from = 0, Integer step = 20) {

        List<GraphNavigator.Linkable> links = nodes
        List<GraphNavigator.Linkable> selectedLinks = selected.values()
                .findAll { it.selected }
                .collect { it.link }
                .findAll { it.isId() }

        List<GraphNavigator.Linkable> requiredLinks = selected.values()
                .findAll { it.required }
                .collect { it.link }
                .findAll { it.isId() }

        if (filter.selected)
            links = selectedLinks

        if (filter.required)
            links = requiredLinks

        List<Reduced> reduced = links
                .findAll { GraphNavigator.Linkable link ->  !filter.id || link.value.contains(filter.id as CharSequence)}
                .collect{ GraphNavigator.Linkable link ->
                    new Reduced(
                        link: link,
                        node: runGraph.navigator.nodes[link.value],
                        name: link.value.split(' ')[0].replace('[', '').replace(']', ''),
                        subId: link.value.split(' ')[1].replace('[', '').replace(']', ''))
                }
                .findAll { it.node }

        if (filter.owner)
            reduced = reduced.findAll { it.node.owner.contains(filter.owner as CharSequence)}

        if (filter.name)
            reduced = reduced.findAll { it.name.contains(filter.name as CharSequence)}

        List<String> names = reduced.collect {it.name}.unique()
        List<String> owners = reduced.collect {it.node.owner}.unique()

        Integer requiredCount = reduced.sum { selected.containsKey(it.link.value) && selected[it.link.value].required? 1 : 0   } as Integer
        Integer selectedCount = reduced.sum { selected.containsKey(it.link.value) && selected[it.link.value].selected? 1 : 0    } as Integer

        Integer total = reduced.size()

        if (total > from) {
            def top = Math.min(total, from + step)
            reduced = reduced[from..top - 1]
        }

        return [
            count: total,
            total: nodes.size(),
            selected: requiredCount,
            requiredByAssociation: selectedCount,
            names: names,
            owners: owners,
            nodes: reduced.collect {

                    String value = it.link.value as String
                    String owner = it.node.owner as String

                    def scm = invOfScm[owner]

                    return [
                        required: selected[value] && selected[value].required,
                        selected: selected[value] && selected[value].selected,
                        owner: owner,
                        name: it.name,
                        id: it.subId,
                        scm: scm,
                        links: [
                            viewScm: "/scms/view?name=${scm}",
                            requiredBy: "/run/requiredBy?id=${value}",
                            stage: "/run/stage?id=${value}",
                            unstage: "/run/unstage?id=${value}"
                        ]
                    ]
                }
        ]
    }

    Map requireByToMap(String id) {

        def nodes = []
        def linkableToCheck = selected.find { it.value.link.value == id }

        if (linkableToCheck) {
            for (Map.Entry<GraphNavigator.Linkable, Integer> entry : runGraph.navigator.requiredByAll(linkableToCheck.value.link)) {

                GraphNavigator.Linkable link = entry.key
                Integer iteration = entry.value

                if (!link.isOwner())
                    continue

                String value = link.value

                def node = runGraph.navigator.nodes[value]

                String name = node.id.split(' ')[0].replace('[', '').replace(']', '')
                String subId = node.id.split(' ')[1].replace('[', '').replace(']', '')

                nodes.add([
                    name: name,
                    id: subId,
                    owner: node.owner,
                    iteration: iteration,
                    required: selected[node.id] && selected[node.id].required,
                    selected: selected[node.id] && selected[node.id].selected,
                ])
            }
        }

        return [
            nodes: nodes
        ]

    }

    private static class Selected {
        boolean required
        boolean selected
        GraphNavigator.Linkable link
    }

    private static class Reduced {
        GraphNavigator.Linkable link
        GraphNavigator.Node node
        String name
        String subId
    }

}
