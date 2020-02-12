package io.peasoup.inv.composer


import groovy.transform.CompileStatic
import io.peasoup.inv.utils.Progressbar

@CompileStatic
class ScmFileCollection {

    private final List<ScmFile> scms = [].asSynchronized() as List<ScmFile>

    final Map<String, ScmFile.SourceFileElement> elements = [:]

    final Set<String> staged = new HashSet<>()

    ScmFileCollection(File scmFolder) {
        assert scmFolder != null, 'SCM folder is required'
        assert scmFolder.exists(), "SCM folder must exist on filesystem"

        def files = scmFolder.listFiles()
        def progress = new Progressbar("Reading from '${scmFolder.absolutePath}'".toString(), files.size(), false)
        progress.start {
            files.each {
                load(it)
                progress.step()
            }
        }
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


        elements.values().each {

            boolean selected = false
            boolean staged = staged.contains(it.descriptor.name)


            if (runFile)
                selected = runFile.isSelected(it.descriptor.name)

            boolean filteredOutSelected = filter.selected && !selected
            boolean filteredOutStaged = filter.staged && !staged


            if (selected)
                selectedCount++

            if (staged)
                stagedCount++

            if (filteredOutSelected && filteredOutStaged)
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

            scm.selected = selected
            scm.staged = staged

            filtered.add(scm)
        }

        return [
            descriptors: filtered,
            total: filtered.size(),
            selected: selectedCount,
            staged: stagedCount
        ]
    }

}
