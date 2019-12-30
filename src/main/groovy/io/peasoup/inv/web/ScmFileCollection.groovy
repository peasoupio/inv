package io.peasoup.inv.web

import groovy.transform.CompileStatic
import me.tongfei.progressbar.ProgressBar

@CompileStatic
class ScmFileCollection {

    private final List<ScmFile> scms = [].asSynchronized()

    final Map<String, ScmFile.SourceFileElement> elements = [:]

    ScmFileCollection(File scmFolder) {
        assert scmFolder
        assert scmFolder.exists()

        def files = scmFolder.listFiles()

        ProgressBar pb = new ProgressBar("Reading scm files", files.size())

        try {
            files.each {
                load(it)
                pb.step()
            }
        } finally {
            pb.close()
        }
    }

    void load(File file) {
        assert file
        assert file.exists()

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

    Map toMap(Map filter = [:], Integer from = 0, Integer to = 0) {
        // Prepare output object

        List<Map> registry = []
        Map<String, Map> scripts = [:]

        Map output = [
                scripts : scripts,
                registry: registry,
                total: scms.sum { it.elements.size() },
                links: [
                        search: "/scms",
                        selected: "/scms/selected"
                ]
        ]

        // Process complex search
        scms.each {

            scripts[it.sourceFile.name] = [
                    text    : it.text,
                    lastEdit: it.lastEdit
            ]

            it.elements.values().each {
                def element = it.toMap(filter)

                if (!element)
                    return

                registry.add(element)
            }
        }

        if (registry.size() > from + to)
            output.registry = registry[from..(from + to - 1)]

        return output
    }

}
