package io.peasoup.inv.web

import io.peasoup.inv.scm.ScmDescriptor

class ScmSourceFile {

    final static Map<String, ScmSourceFile.SourceFileElement> scmCache = [:]

    final Run baseRun
    final File sourceFile
    final ScmDescriptor scmDescriptor
    final Map<String, ScmSourceFile.SourceFileElement> scmElements = [:]

    String text
    Long lastEdit

    ScmSourceFile(File file, Run baseRun) {

        this.baseRun = baseRun
        sourceFile = file
        scmDescriptor = new ScmDescriptor(file.newReader())

        text = file.text
        lastEdit = file.lastModified()

        scmDescriptor.scms().each { String name, ScmDescriptor.MainDescriptor desc ->
            def element = new ScmSourceFile.SourceFileElement(
                descriptor: desc,
                script: file.name
            )

            scmCache[name] = element
            scmElements[name] = element
        }
    }

    Map toMap(Map filter = [:], Integer from = 0, Integer to = 20) {

        def filtered = scmElements.values()
            .findAll { !filter.name || it.descriptor.name.contains(filter.name) }
            .findAll { !filter.src || it.descriptor.src.contains(filter.src) }
            .findAll { !filter.entry || it.descriptor.entry.contains(filter.entry) }

        filtered.collectEntries {[
                (it.descriptor.name): [
                    name: it.descriptor.name,
                    source: sourceFile.name,
                    descriptor: [
                        selected: baseRun.isSelected(it.descriptor.name),
                        name: it.descriptor.name,
                        entry: it.descriptor.entry,
                        hooks: it.descriptor.hooks,
                        hasParameters: !it.descriptor.ask.parameters.isEmpty(),
                        src: it.descriptor.src,
                        timeout: it.descriptor.timeout
                    ],
                    links: [
                        save: "/scms/source?name=${it.descriptor.name}",
                        parameters: "/scms/parameters?name=${it.descriptor.name}"
                    ]
                ]
            ]}
    }

    static class SourceFileElement {
        ScmDescriptor.MainDescriptor descriptor
        String script

        String simpleName() {
            return script.split('\\.')[0]
        }

        Map getParameters(File externalPropertyFile = null) {

            def externalProperties = [:]

            if (externalPropertyFile && externalPropertyFile.exists()) {
                def props = new Properties()
                props.load(externalPropertyFile.newReader())

                externalProperties = props
            }

            def parameters = []

            descriptor.ask.parameters.each { Map parameter ->

                List<String> values = []

                if (parameter.values instanceof CharSequence) {
                    String stdout = parameter.values.execute().in.text

                    if (parameter.filter) {
                        def matches = stdout =~ parameter.filter
                        matches.each {
                            values << it[1]
                        }
                    } else {
                        values = stdout.split()
                    }
                } else if(parameter.values instanceof Collection<String>) {
                    values = parameter.values
                }

                parameters << [
                        name: parameter.name,
                        usage: parameter.usage,
                        value: externalProperties[(descriptor.name + "." + parameter.name)],
                        defaultValue: parameter.defaultValue,
                        values: values,
                        links: [
                            save: "/scms/parameters?name=${descriptor.name}&parameter=${parameter.name}"
                        ]
                ]
            }

            return [
                owner: descriptor.name,
                parameters: parameters
            ]
        }
    }
}
