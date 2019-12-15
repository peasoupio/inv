package io.peasoup.inv.web

import io.peasoup.inv.scm.ScmDescriptor

class ScmSourceFile {

    static Map toMap(String sourceFile,  ScmDescriptor.MainDescriptor descriptor) {
        return [
            name: descriptor.name,
            source: sourceFile,
            descriptor: [
                name: descriptor.name,
                entry: descriptor.entry,
                hooks: descriptor.hooks,
                hasParameters: !descriptor.ask.parameters.isEmpty(),
                src: descriptor.src,
                timeout: descriptor.timeout
            ],
            links: [
                save: "/scms/${descriptor.name}/source",
                parameters: "/scms/${descriptor.name}/parameters",
            ]
        ]
    }

    static Map getParameters(ScmDescriptor.MainDescriptor descriptor, File externalPropertyFile = null) {
        assert descriptor

        def output = [
            owner: descriptor.name,
            parameters: []
        ]

        def externalProperties = [:]

        if (externalPropertyFile && externalPropertyFile.exists()) {
            def props = new Properties()
            props.load(externalPropertyFile.newReader())

            externalProperties = props
        }

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

            output.parameters << [
                name: parameter.name,
                usage: parameter.usage,
                value: externalProperties[(descriptor.name + "." + parameter.name)],
                defaultValue: parameter.defaultValue,
                values: values,
                links: [
                    save: "/scms/${descriptor.name}/parameters/${parameter.name}"
                ]
            ]
        }

        return output
    }

    static class SourceFileElement {
        ScmDescriptor.MainDescriptor descriptor
        String script

        String simpleName() {
            return script.split('\\.')[0]
        }
    }
}
