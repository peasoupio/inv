package io.peasoup.inv.composer

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import io.peasoup.inv.graph.GraphNavigator
import io.peasoup.inv.graph.RunGraph

@CompileStatic
class RunFile {

    final private File runFile
    final private RunGraph runGraph


    final private Map<String, List<String>> ownerOfScm
    final private Map<String, RunGraph.FileStatement> invOfScm

    final Map<String, StagedId> staged = [:]
    final Set<GraphNavigator.Linkable> nodes

    final Map<String, List<GraphNavigator.Linkable>> owners
    final Map<String, List<GraphNavigator.Linkable>> names

    RunFile(File runFile) {
        assert runFile != null, 'Run file is required'
        assert runFile.exists(), 'Run file must exist on filesystem'

        this.runFile = runFile
        runGraph = new RunGraph(runFile.newReader())

        ownerOfScm = (Map<String, List<String>>)runGraph.files.groupBy { it.scm }.collectEntries { [(it.key): it.value.collect { it.inv} ]}
        invOfScm = runGraph.files.collectEntries { [(it.inv): it.scm] }

        nodes = runGraph.navigator.links().findAll { it.isId() }

        owners = (Map<String, List<GraphNavigator.Linkable>>) nodes.groupBy { runGraph.navigator.nodes[it.value].owner }
        names = (Map<String, List<GraphNavigator.Linkable>>) nodes.groupBy { it.value.split(' ')[0].replace('[', '').replace(']', '') }
    }

    synchronized void stage(String id) {
        assert id, 'Id is required'

        if (staged.containsKey(id))
            return

        def stagedId = new StagedId(new GraphNavigator.Id(value: id))
        stagedId.selected = true

        staged.put(id, stagedId)

        propagate()
    }

    synchronized void stageWithoutPropagate(String id) {
        assert id, 'Id is required'

        if (staged.containsKey(id))
            return

        def stagedId = new StagedId(new GraphNavigator.Id(value: id))
        stagedId.selected = true

        staged.put(id, stagedId)
    }

    synchronized void stageAll() {
        nodes.each { GraphNavigator.Linkable link ->
            def stagedId = new StagedId(link)
            stagedId.selected = true

            staged.put(link.value, stagedId)
        }
    }

    synchronized void unstage(String id) {
        assert id, 'Id is required'

        if (!staged.containsKey(id))
            return

        staged.remove(id)

        propagate()
    }

    synchronized void unstageAll() {
        staged.clear()
    }

    /**
     * Iterate through the nodes to get the required ones by association
     * @return Number of required added
     */
    @CompileDynamic
    synchronized Map propagate() {

        Map<String, StagedId> checkRequiresByAll = staged
                .findAll { it.value.selected }

        Map<String, Integer> output = [
            all: staged.size() == nodes.size(),
            checked: checkRequiresByAll.size(),
            added: 0
        ] as Map<String, Integer>

        // (Re)init with selected only
        staged.clear()
        staged.putAll(checkRequiresByAll)

        // Are all nodes required ?
        if (output.all)
            return output

        List<GraphNavigator.Linkable> alreadyStaged = checkRequiresByAll.values().collect { it.link }

        // If there's more selected than remaining, we'll seek from non-selected
        boolean reverseMode = staged.size() > (nodes.size() / 2)
        List<StagedId> toCheck

        if (!reverseMode)
            toCheck = checkRequiresByAll.values().toList() as List<StagedId>
        else
            toCheck = nodes.findAll { !checkRequiresByAll.containsKey(it.value) }.collect {
                new StagedId(new GraphNavigator.Id(value: it.value))
            }

        while(!toCheck.isEmpty()) {
            def chosen = toCheck.pop()
            GraphNavigator.Linkable chosenLink = chosen.link

            if (reverseMode) {
                def links = runGraph.navigator.requiredBy(chosenLink)

                boolean matched = false

                for(GraphNavigator.Linkable owner : links) {
                    def node = runGraph.navigator.nodes[owner.value]
                    if (!staged.containsKey(node.id))
                        continue

                    matched = true
                    break
                }

                if (!matched)
                    continue

                chosen.required = true
                staged.put(chosenLink.value, chosen)

                alreadyStaged.add(chosenLink)
                output.added++

                continue
            }

            Map<GraphNavigator.Linkable, Integer> links = runGraph.navigator.requiresAll(
                    chosenLink,
                    alreadyStaged)

            if (links == null) {
                println "${chosen.link.value} is not a valid id"
                continue
            }

            for(GraphNavigator.Linkable link : links.keySet()) {

                if (!link.isId())
                    continue

                StagedId selectedId = new StagedId(link)
                selectedId.required = true

                staged.put(link.value, selectedId)

                alreadyStaged.add(link)
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

            return staged.containsKey(node.id) && staged[node.id].selected
        }
    }

    List<String> selectedScms() {
        return staged.values()
                .collect { runGraph.navigator.nodes[it.link.value] }
                .findAll { it }
                .collect { invOfScm[it.owner] }
                .findAll { it }
                .unique() as List<String>
    }

    Map nodesToMap(Map filter = [:]) {

        Collection<GraphNavigator.Linkable> links = nodes
        Collection<GraphNavigator.Linkable> selectedLinks = staged.values()
                .findAll { it.selected }
                .collect { it.link }
                .findAll { it.isId() }

        Collection<GraphNavigator.Linkable> requiredLinks = staged.values()
                .findAll { it.required }
                .collect { it.link }
                .findAll { it.isId() }

        if (filter.selected)
            links = selectedLinks

        if (filter.required)
            links = requiredLinks

        List<Reduced> reduced = links
                .findAll { GraphNavigator.Linkable link -> !filter.id || link.value.contains(filter.id as CharSequence) }
                .collect { GraphNavigator.Linkable link ->
                    new Reduced(
                            link: link,
                            node: runGraph.navigator.nodes[link.value],
                            name: link.value.split(' ')[0].replace('[', '').replace(']', ''),
                            subId: link.value.split(' ')[1].replace('[', '').replace(']', ''))
                }
                .findAll { it.node }

        if (filter.owner)
            reduced = reduced.findAll { it.node.owner.contains(filter.owner as CharSequence) }

        if (filter.name)
            reduced = reduced.findAll { it.name.contains(filter.name as CharSequence) }

        List<String> names = reduced.collect { it.name }.unique()
        List<String> owners = reduced.collect { it.node.owner }.unique()

        return [
                total                : nodes.size(),
                count                : reduced.size(),
                selected             : (reduced.sum { (staged.containsKey(it.link.value) && staged[it.link.value].selected) ? 1 : 0 } as Integer) ?: 0,
                requiredByAssociation: (reduced.sum { (staged.containsKey(it.link.value) && staged[it.link.value].required) ? 1 : 0 } as Integer) ?: 0,
                names                : names,
                owners               : owners,
                nodes                : reduced.collect {

                    String value = it.link.value as String
                    String owner = it.node.owner as String

                    def scm = invOfScm[owner]

                    return [
                            required: staged[value] && staged[value].required,
                            selected: staged[value] && staged[value].selected,
                            owner   : owner,
                            name    : it.name,
                            id      : it.subId,
                            scm     : scm,
                            links   : [
                                    viewScm   : "/scms/view?name=${scm}",
                                    requiredBy: "/run/requiredBy?id=${value}",
                                    stage     : "/run/stage?id=${value}",
                                    unstage   : "/run/unstage?id=${value}"
                            ]
                    ]
                }
        ]
    }

    Map requireByToMap(String id) {

        def nodes = []
        def linkableToCheck = staged.find { it.value.link.value == id }

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
                    required: staged[node.id] && staged[node.id].required,
                    selected: staged[node.id] && staged[node.id].selected,
                ])
            }
        }

        return [
            nodes: nodes
        ]

    }

    private static class StagedId {
        final GraphNavigator.Linkable link

        boolean required
        boolean selected

        StagedId(GraphNavigator.Linkable link) {
            assert link, 'Link is required'

            this.link = link
        }
    }

    private static class Reduced {
        GraphNavigator.Linkable link
        GraphNavigator.Node node
        String name
        String subId
    }

}
