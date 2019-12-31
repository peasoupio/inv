package io.peasoup.inv.web

import groovy.transform.CompileStatic
import io.peasoup.inv.Logger
import io.peasoup.inv.graph.GraphNavigator
import io.peasoup.inv.graph.RunGraph

@CompileStatic
class RunFile {

    final private File runFile
    final private RunGraph runGraph


    final private Map<String, List<String>> ownerOfScm = [:]
    final private Map<String, RunGraph.FileStatement> invOfScm = [:]

    final Map<String, Selected> selected = [:]
    final List<GraphNavigator.Linkable> nodes = []

    final Set<String> owners = new HashSet<>()
    final Set<String> names = new HashSet<>()

    RunFile(File runFile) {
        assert runFile
        assert runFile.exists()

        this.runFile = runFile
        runGraph = new RunGraph(runFile.newReader())

        ownerOfScm = runGraph.files.groupBy { it.scm }.collectEntries { [(it.key): it.value.collect { it.inv} ]} as Map<String, List<String>>
        invOfScm = runGraph.files.collectEntries { [(it.inv): it.scm] }

        nodes = (runGraph.g.vertexSet() as List<GraphNavigator.Linkable>).findAll { it.isId() }

        nodes.each {
            def node = runGraph.navigator.nodes[it.value]
            owners.add(node.owner)
            names.add(it.value.split(' ')[0].replace('[', '').replace(']', ''))
        }
    }

    synchronized void stage(String id) {
        selected.put(id, new Selected(
            selected: true,
            link: new GraphNavigator.Id(value: id)
        ))

        propagate()
    }

    synchronized void stageWithoutPropagate(String id) {
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
        selected.remove(id)

        propagate()
    }

    synchronized void unstageAll() {
        selected.clear()
    }

    synchronized void propagate() {

        def checkRequiresByAll = selected
                .findAll { it.value.selected }

        // (Re)init with selected only
        selected.clear()
        selected.putAll(checkRequiresByAll)

        // Are all nodes required ?
        if (selected.size() == nodes.size())
            return

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
                    checkRequiresByAllList.removeAll { it.key == link.value}
                    continue
                }

                selected.put(link.value, new Selected(
                        required: true,
                        link: link
                ))
            }
        }
    }

    boolean isSelected(String scm) {
        if (!ownerOfScm.containsKey(scm)) {
            Logger.warn("scm ${scm} does not exists")
            return false
        }

        return ownerOfScm[scm].any { String inv ->
            def node = runGraph.navigator.nodes[inv] as GraphNavigator.Node

            if (!node)
                return false

            return selected.containsKey(node.id) && selected[node.id].selected
        }
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

        Integer total = reduced.size()

        if (total > from) {
            def top = Math.min(total, from + step)
            reduced = reduced[from..top - 1]
        }

        return [
            count: total,
            total: nodes.size(),
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
                            viewScm: "/scms/view?name=${scm}",
                            requiredBy: "/run/requiredBy?id=${value}",
                            stage: "/run/stage?id=${value}",
                            unstage: "/run/unstage?id=${value}"
                        ]
                        //required: flattenedEdges[owner]
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
