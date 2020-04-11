package io.peasoup.inv.composer

import groovy.transform.CompileStatic

@CompileStatic
class ScmFileCollection {


    private final List<ScmFile> scms = [].asSynchronized() as List<ScmFile>

    final File scmFolder
    final Map<String, ScmFile.SourceFileElement> elements = [:]
    final Set<String> staged = new HashSet<>()

    ScmFileCollection(File scmFolder) {
        assert scmFolder != null, 'SCM folder is required'
        assert scmFolder.exists(), "SCM folder must exist on filesystem"

        this.scmFolder = scmFolder
    }

    void load(File file) {
        assert file != null, 'File is required'
        assert file.exists(), 'File must exist on filesystem'

        // Check for duplicates
        scms.removeAll {
            if (it.sourceFile != file)
                return

            it.elements.keySet().each {
                elements.remove(it)
            }

            return true
        }

        def scm = new ScmFile(file)
        scms << scm
        elements.putAll(scm.elements)
    }

    /*
        Reload file to make sure latest changes are available
     */

    boolean reload(String name) {
        assert name, 'Name is required'

        def existingElement = elements[name]
        if (!existingElement)
            return false

        load(existingElement.scriptFile)

        def latestElement = scms.elements[name]
        if (!latestElement)
            return false

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

    List<File> toFiles(List<String> names = null) {
        if (!names)
            return elements.values().collect { it.scriptFile }

        return names
                .findAll { elements.containsKey(it) }
                .collect { elements[it].scriptFile }
    }

    Map toMap(RunFile runFile = null, Map filter = [:], String parametersLocation = null) {

        List<Map> filtered = []
        Integer selectedCount = 0
        Integer stagedCount = 0

        String filterName = filter.name as String

        elements.values().each {
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

            File parameterLocation
            if (parametersLocation)
                parameterLocation = new File(parametersLocation, it.simpleName() + ".json")

            def scm = it.toMap(filter, parameterLocation)
            if (!scm)
                return

            boolean filteredOutHideOnComplete = filter.hideOnComplete && scm.completed
            if (filteredOutHideOnComplete)
                return

            scm.selected = isSelected
            scm.staged = isStaged

            filtered.add(scm)
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
