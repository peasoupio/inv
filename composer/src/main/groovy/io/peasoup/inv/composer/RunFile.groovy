package io.peasoup.inv.composer

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import io.peasoup.inv.composer.utils.MapUtils
import io.peasoup.inv.graph.GraphNavigator
import io.peasoup.inv.graph.RunGraph

import java.util.regex.Matcher
import java.util.regex.Pattern

@CompileStatic
class RunFile {

    final File runFile
    final private RunGraph runGraph

    final private Map<String, List<String>> ownerOfRepo
    final private Map<String, RunGraph.FileStatement> invOfRepo

    final Map<String, StagedId> stagedIds = [:]
    final Set<GraphNavigator.Linkable> nodes

    final Map<String, List<GraphNavigator.Linkable>> owners
    final Map<String, List<GraphNavigator.Linkable>> names

    final static Pattern ID_PATTERN = Pattern.compile('^\\[(.*)\\]\\s(.*)$')

    RunFile(File runFile) {
        assert runFile != null, 'Run file is required'
        assert runFile.exists(), 'Run file must exist on filesystem'

        this.runFile = runFile
        runGraph = new RunGraph(runFile.newReader())

        ownerOfRepo = (Map<String, List<String>>) runGraph.files.groupBy { it.repo }.collectEntries { [(it.key): it.value.unique { it.inv }.collect { it.inv }] }
        invOfRepo = runGraph.files.collectEntries { [(it.inv): it.repo] }

        // Get Id only and filter out unbloated ones
        nodes = runGraph.navigator.links().findAll { it.isId() && runGraph.navigator.nodes.containsKey(it.value) }

        owners = (Map<String, List<GraphNavigator.Linkable>>) nodes.groupBy { runGraph.navigator.nodes[it.value].owner }
        names = (Map<String, List<GraphNavigator.Linkable>>) nodes.groupBy { it.value.split(' ')[0].replace('[', '').replace(']', '') }
    }

    List<List<GraphNavigator.Linkable>> getPathWithRequired(String destination) {
        assert destination, 'Source is required'

        def output = []
        def owner = new GraphNavigator.Owner(value: destination)

        // Get owners of required staged
        def stagedOwners = stagedIds
                .values()
                .findAll { it.staged }
                .collect {
                    def node = runGraph.navigator.nodes[it.link.value]

                    if (!node)
                        return null

                    return node.owner
                }
                .findAll {
                    it && it != destination
                }
                .unique()

        // Travel staged owners to get their dependency path
        for (String source : stagedOwners) {
            def path = runGraph.navigator.getPaths(new GraphNavigator.Owner(value: source), owner)

            // If path empty, skip
            if (!path)
                continue

            output.add(path)
        }

        return output
    }

    synchronized void stage(String id) {
        assert id, 'Id is required'

        if (stagedIds.containsKey(id))
            return

        def stagedId = new StagedId(new GraphNavigator.Id(value: id))
        stagedId.staged = true

        stagedIds.put(id, stagedId)

        propagate()
    }

    synchronized void stageWithoutPropagate(String id) {
        assert id, 'Id is required'

        if (stagedIds.containsKey(id))
            return

        def stagedId = new StagedId(new GraphNavigator.Id(value: id))
        stagedId.staged = true

        stagedIds.put(id, stagedId)
    }

    synchronized void stageAll() {
        nodes.each { GraphNavigator.Linkable link ->
            def stagedId = new StagedId(link)
            stagedId.staged = true

            stagedIds.put(link.value, stagedId)
        }
    }

    synchronized void unstage(String id) {
        assert id, 'Id is required'

        if (!stagedIds.containsKey(id))
            return

        stagedIds.remove(id)

        propagate()
    }

    synchronized void unstageWithoutPropagate(String id) {
        assert id, 'Id is required'

        if (!stagedIds.containsKey(id))
            return

        stagedIds.remove(id)
    }

    synchronized void unstageAll() {
        stagedIds.clear()
    }

    /**
     * Iterate through the nodes to get the required ones by association
     * @return Number of required added
     */
    @CompileDynamic
    synchronized Map propagate() {

        Map<String, StagedId> stagedIds = stagedIds
                .findAll { it.value.staged }

        Map<String, Integer> output = [
                all    : this.stagedIds.size() == nodes.size(),
                checked: stagedIds.size(),
                added  : 0
        ] as Map<String, Integer>

        // Are all nodes required ?
        if (output.all)
            return output

        // (Re)init with staged only
        this.stagedIds.clear()
        this.stagedIds.putAll(stagedIds)

        List<GraphNavigator.Linkable> alreadyStaged = stagedIds.values().collect { it.link }

        // If there are more staged than remaining, we'll seek from non-staged (reverse mode)
        boolean reverseMode = this.stagedIds.size() > (nodes.size() / 2)
        List<StagedId> stegedIdsToCheck

        if (!reverseMode)
            stegedIdsToCheck = stagedIds.values().toList() as List<StagedId>
        else
            stegedIdsToCheck = nodes.findAll { !stagedIds.containsKey(it.value) }.collect {
                new StagedId(new GraphNavigator.Id(value: it.value))
            }

        while (!stegedIdsToCheck.isEmpty()) {

            def stageIdToCheck = stegedIdsToCheck.pop()
            GraphNavigator.Linkable linkToCheck = stageIdToCheck.link

            // In reverse mode, since all staged are to check,
            // We only get the requiredBy, instead of the requireByAll
            if (reverseMode) {

                //Determine if stage ID to check has a staged dependency
                def matched = runGraph.navigator.requiredBy(linkToCheck)
                        .stream()
                        .anyMatch(owner -> {
                            def node = runGraph.navigator.nodes[owner.value]
                            return stagedIds.containsKey(node.id)
                        })

                if (!matched)
                    continue

                stageIdToCheck.required = true
                this.stagedIds.put(linkToCheck.value, stageIdToCheck)

                alreadyStaged.add(linkToCheck)
                output.added++
            } else {

                // Get all dependencies of the link to check, excluding already staged links
                Map<GraphNavigator.Linkable, Integer> links = runGraph.navigator.requiresAll(
                        linkToCheck,
                        alreadyStaged)

                if (links == null) {
                    println "${stageIdToCheck.link.value} is not a valid id"
                    continue
                }

                // Stage all found links
                for (GraphNavigator.Linkable link : links.keySet()) {
                    if (!link.isId())
                        continue

                    StagedId stagedId = new StagedId(link)
                    stagedId.required = true

                    this.stagedIds.put(link.value, stagedId)

                    alreadyStaged.add(link)
                    output.added++
                }
            }
        }

        return output
    }

    /**
     * Determines if a REPO is required or not
     * @param repo Repo name
     * @return True if required, otherwise false
     */
    boolean isRepoRequired(String repo) {
        if (!ownerOfRepo.containsKey(repo))
            return false

        return ownerOfRepo[repo].any { String inv ->
            def statements = owners.get(inv)
            if (!statements)
                return false

            return statements.any { GraphNavigator.Linkable link -> stagedIds.containsKey(link.value) }
        }
    }

    /**
     * Gets required REPOs
     * @return List of required REPOs
     */
    List<String> requiredRepos() {
        return stagedIds.values()
                .collect { runGraph.navigator.nodes[it.link.value] }
                .findAll { it }
                .collect { invOfRepo[it.owner] }
                .findAll { it }
                .unique() as List<String>
    }

    Map toMap(boolean secure = true, Map filter = [:]) {

        Collection<GraphNavigator.Linkable> links = nodes
        Collection<GraphNavigator.Linkable> stagedLinks = stagedIds.values()
                .findAll { it.staged }
                .collect { it.link }
                .findAll { it.isId() }

        Collection<GraphNavigator.Linkable> requiredLinks = stagedIds.values()
                .findAll { it.required }
                .collect { it.link }
                .findAll { it.isId() }

        if (filter.staged)
            links = stagedLinks

        if (filter.required)
            links = requiredLinks

        List<Reduced> reduced = links
                .findAll { GraphNavigator.Linkable link -> !filter.id || link.value.contains(filter.id as CharSequence) }
                .findAll { GraphNavigator.Linkable link -> !filter.owner || runGraph.navigator.nodes[link.value].owner.contains(filter.owner as CharSequence) }
                .collect { GraphNavigator.Linkable link ->
                    Matcher match = link.value =~ ID_PATTERN

                    if (!match.matches())
                        return null

                    new Reduced(
                            link: link,
                            node: runGraph.navigator.nodes[link.value],
                            name: match.group(1) as String ?: 'none',
                            subId: match.group(2) as String ?: 'none')
                }
                .findAll { it && it.node }

        if (filter.name)
            reduced = reduced.findAll { it.name.contains(filter.name as CharSequence) }

        List<String> names = reduced.collect { it.name }.unique()
        List<String> owners = reduced.collect { it.node.owner }.unique()

        return [
                total   : nodes.size(),
                count   : reduced.size(),
                staged  : (reduced.sum { (stagedIds.containsKey(it.link.value) && stagedIds[it.link.value].staged) ? 1 : 0 } as Integer) ?: 0,
                required: (reduced.sum { (stagedIds.containsKey(it.link.value) && stagedIds[it.link.value].required) ? 1 : 0 } as Integer) ?: 0,
                names   : names,
                owners  : owners,
                nodes   : reduced.collect {

                    String value = it.link.value as String
                    String owner = it.node.owner as String

                    String repo = invOfRepo[owner]

                    String urlifiedValue = URLUtils.urlify(value)

                    def invMap = [
                            required: stagedIds[value] && stagedIds[value].required,
                            staged  : stagedIds[value] && stagedIds[value].staged,
                            owner   : owner,
                            name    : it.name,
                            id      : it.subId,
                            repo    : repo,
                            links   : [
                                    viewRepo  : WebServer.API_CONTEXT_ROOT + "/repos/view?name=${repo}",
                                    requiredBy: WebServer.API_CONTEXT_ROOT + "/run/requiredBy?id=${urlifiedValue}"
                            ]
                    ]

                    if (secure) {
                        MapUtils.merge(invMap, [
                                links: [
                                        stage  : WebServer.API_CONTEXT_ROOT + "/run/stage?id=${urlifiedValue}",
                                        unstage: WebServer.API_CONTEXT_ROOT + "/run/unstage?id=${urlifiedValue}"
                                ]
                        ])
                    }

                    return invMap
                }
        ]
    }

    Map requiredByMap(String id) {

        def nodes = []
        def linkableToCheck = stagedIds.find { it.value.link.value == id }

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
                        name     : name,
                        id       : subId,
                        owner    : node.owner,
                        iteration: iteration,
                        required : stagedIds[node.id] && stagedIds[node.id].required,
                        staged   : stagedIds[node.id] && stagedIds[node.id].staged,
                ])
            }
        }

        return [
                nodes: nodes
        ]
    }

    Map tagsMap(boolean secure = true) {
        List<Map> tags = []
        Map output = [
                tags: tags
        ]

        for (Map.Entry<String, Map<String, List<RunGraph.VirtualInv>>> tag : runGraph.tags) {
            List<Map> subTags = []
            Map tagOutput = [
                    label  : tag.key,
                    subTags: subTags
            ]

            for (Map.Entry<String, List<RunGraph.VirtualInv>> subTag : tag.value) {
                List<Map> invs = []
                Map subTagOutput = [
                        label: subTag.key,
                        invs : invs,
                        links: [:]

                ]

                if (secure)
                    MapUtils.merge(subTagOutput, [
                            links: [
                                    stageAll  : WebServer.API_CONTEXT_ROOT + "/run/tags/stage?tag=${tag.key}&subtag=${subTag.key}",
                                    unstageAll: WebServer.API_CONTEXT_ROOT + "/run/tags/unstage?tag=${tag.key}&subtag=${subTag.key}"
                            ]
                    ])

                for (RunGraph.VirtualInv inv : subTag.value) {
                    def ids = owners.get(inv.name)

                    def invMap = [
                            label   : inv.name,
                            staged  : !ids ? 0 : ids.findAll { stagedIds[it.value] && stagedIds[it.value].staged }.size(),
                            required: !ids ? 0 : ids.findAll { stagedIds[it.value] && stagedIds[it.value].required }.size(),
                            links   : [:]
                    ]

                    if (secure)
                        MapUtils.merge(invMap, [
                                links: [
                                        stage  : WebServer.API_CONTEXT_ROOT + "/run/stage?owner=${inv.name}",
                                        unstage: WebServer.API_CONTEXT_ROOT + "/run/unstage?owner=${inv.name}"
                                ]
                        ])

                    invs.add(invMap)
                }

                subTags.add(subTagOutput)
            }

            tags.add(tagOutput)
        }

        return output
    }

    private static class StagedId {
        final GraphNavigator.Linkable link

        /**
         * True when another ID needs this one to work.
         */
        boolean required

        /**
         * True when this ID is staged manually by the end-user.
         */
        boolean staged

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
