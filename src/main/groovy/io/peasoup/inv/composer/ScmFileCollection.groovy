package io.peasoup.inv.composer


import groovy.transform.CompileStatic
import io.peasoup.inv.utils.Progressbar

@CompileStatic
class ScmFileCollection {

    private final List<ScmFile> scms = [].asSynchronized() as List<ScmFile>

    final Map<String, ScmFile.SourceFileElement> elements = [:]

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

    List<File> toFiles(List<String> names = null) {
        if (!names)
            return elements.values().collect { it.scriptFile }

        return names
            .findAll { elements.containsKey(it) }
            .collect { elements[it].scriptFile }
    }

    Map toMap(Map filter = [:]) {

        List<Map> filtered = []

        Map output = [
                descriptors: filtered,
                total: scms.sum { it.elements.size() }
        ]

        elements.values().each {
            def element = it.toMap(filter)

            if (!element)
                return

            filtered.add(element)
        }

        return output
    }

}
