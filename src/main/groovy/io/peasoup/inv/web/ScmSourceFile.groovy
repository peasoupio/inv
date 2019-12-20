package io.peasoup.inv.web

import io.peasoup.inv.scm.ScmDescriptor

class ScmSourceFile {

    final static Map<String, ScmSourceFile.SourceFileElement> scmCache = [:]

    final File sourceFile
    final ScmDescriptor scmDescriptor
    final Map<String, ScmSourceFile.SourceFileElement> scmElements = [:]

    String text
    Long lastEdit

    ScmSourceFile(File file) {

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

    Collection<ScmSourceFile.SourceFileElement> elements() {
        return scmElements.values()
    }

    static class SourceFileElement {
        ScmDescriptor.MainDescriptor descriptor
        String script

        String simpleName() {
            return script.split('\\.')[0]
        }

        Map toMap(Map filter = [:]) {

            if (filter.name && !descriptor.name.contains(filter.name)) return null
            if (filter.src && !descriptor.src.contains(filter.src)) return null
            if (filter.entry && !descriptor.entry.contains(filter.entry)) return null

            return [
                    name      : descriptor.name,
                    source    : script,
                    descriptor: [
                            name         : descriptor.name,
                            entry        : descriptor.entry,
                            hooks        : descriptor.hooks,
                            hasParameters: !descriptor.ask.parameters.isEmpty(),
                            src          : descriptor.src,
                            timeout      : descriptor.timeout
                    ],
                    links     : [
                            save      : "/scms/source?name=${descriptor.name}",
                            parameters: "/scms/parameters?name=${descriptor.name}"
                    ]
            ]
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
