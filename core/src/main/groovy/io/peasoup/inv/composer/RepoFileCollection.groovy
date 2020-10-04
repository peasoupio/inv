package io.peasoup.inv.composer

import groovy.transform.CompileStatic

@CompileStatic
class RepoFileCollection {

    private final List<RepoFile> repos = [].asSynchronized() as List<RepoFile>

    final File repoFolder
    final Map<String, RepoFile.SourceFileElement> elements = [:]
    final Set<String> staged = new HashSet<>()

    RepoFileCollection(File repoFolder) {
        assert repoFolder != null, 'REPO folder is required'
        assert repoFolder.exists(), "REPO folder must exist on filesystem"

        this.repoFolder = repoFolder
    }

    void load(File file) {
        assert file != null, 'File is required'
        assert file.exists(), 'File must exist on filesystem'

        // Check for duplicates
        repos.removeAll {
            if (it.scriptFile != file)
                return false

            it.elements.keySet().each {
                elements.remove(it)
            }

            return true
        }

        def repo = new RepoFile(file)
        repos << repo
        elements.putAll(repo.elements)
    }

    /*
        Reload file to make sure latest changes are available
     */

    boolean reload(String name) {
        assert name, 'Name is required'

        def existingElement = elements[name]
        if (!existingElement)
            return false

        load(existingElement.repoFile.scriptFile)

        def latestElement = repos.elements[name]
        if (!latestElement)
            return false

        return true
    }

    boolean remove(String name) {
        assert name, 'Name is required'

        def existingElement = elements[name]
        if (!existingElement)
            return false

        // Delete the actual file
        existingElement.repoFile.scriptFile.delete()

        // Remove its elements
        repos.removeAll {
            if (it.scriptFile != existingElement.repoFile.scriptFile)
                return false

            it.elements.keySet().each {
                elements.remove(it)
            }

            return true
        }

        return true
    }

    void stage(String name) {
        if (!elements.containsKey(name))
            return

        staged.add(name)
    }

    void unstage(String name) {
        if (!elements.containsKey(name))
            return

        staged.remove(name)
    }

    List<RepoFile> toFiles(List<String> names = null) {
        if (!names)
            return elements.values().collect { it.repoFile }

        return names
                .findAll { elements.containsKey(it) }
                .collect { elements[it].repoFile }
    }

    Map toMap(RunFile runFile = null, Map filter = [:]) {

        List<Map> filtered = []
        Integer selectedCount = 0
        Integer stagedCount = 0

        String filterName = filter.name as String

        elements
            .values()
            .sort { it.descriptor.name }
            .each {
                if (filterName && !it.descriptor.name.contains(filterName))
                    return

                boolean isStaged = staged.contains(it.descriptor.name)
                if (isStaged)
                    stagedCount++

                boolean isSelected = false

                if (runFile) {
                    isSelected = runFile.isSelected(it.descriptor.name)
                    if (isSelected)
                        selectedCount++
                }

                if (filter.selected && !isSelected)
                    return

                if (filter.staged && !isStaged)
                    return

                def repo = it.toMap(filter)
                if (!repo)
                    return

                boolean filteredOutHideOnComplete = filter.hideOnComplete && repo.completed
                if (filteredOutHideOnComplete)
                    return

                repo.selected = isSelected
                repo.staged = isStaged

                filtered.add(repo)
            }

        return [
                descriptors: filtered,
                total      : elements.size(),
                count      : filtered.size(),
                selected   : selectedCount,
                staged     : stagedCount
        ]
    }
}
