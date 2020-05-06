package io.peasoup.inv.composer

import groovy.transform.CompileStatic

@CompileStatic
class ScmFileCollection {

    private final List<ScmFile> scms = [].asSynchronized() as List<ScmFile>

    final File scmFolder
    final File parametersFolder
    final Map<String, ScmFile.SourceFileElement> elements = [:]
    final Set<String> staged = new HashSet<>()

    ScmFileCollection(File scmFolder, File parametersFolder) {
        assert scmFolder != null, 'SCM folder is required'
        assert scmFolder.exists(), "SCM folder must exist on filesystem"

        assert parametersFolder != null, 'Parameters folder is required'
        if (!parametersFolder.exists())
            parametersFolder.mkdirs()

        this.scmFolder = scmFolder
        this.parametersFolder = parametersFolder
    }

    void load(File file) {
        assert file != null, 'File is required'
        assert file.exists(), 'File must exist on filesystem'

        // Check for duplicates
        scms.removeAll {
            if (it.scriptFile != file)
                return false

            it.elements.keySet().each {
                elements.remove(it)
            }

            return true
        }

        def scm = new ScmFile(file, parametersFolder)
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

        load(existingElement.scmFile.scriptFile)

        def latestElement = scms.elements[name]
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
        existingElement.scmFile.scriptFile.delete()

        // Remove its elements
        scms.removeAll {
            if (it.scriptFile != existingElement.scmFile.scriptFile)
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

    List<File> toFiles(List<String> names = null) {
        if (!names)
            return elements.values().collect { it.scmFile.scriptFile }

        return names
                .findAll { elements.containsKey(it) }
                .collect { elements[it].scmFile.scriptFile }
    }

    Map toMap(RunFile runFile = null, Map filter = [:], String parametersLocation = null) {

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
