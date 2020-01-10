package io.peasoup.inv.web

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.transform.CompileStatic
import io.peasoup.inv.Logger
import io.peasoup.inv.scm.ScmDescriptor
import io.peasoup.inv.scm.ScmExecutor

@CompileStatic
class ScmFile {

    final File sourceFile
    final Map<String, ScmFile.SourceFileElement> elements = [:]

    String text
    Long lastEdit

    ScmFile(File file) {

        sourceFile = file

        text = file.text
        lastEdit = file.lastModified()

        new ScmExecutor().with {
            read(file)

            scms.each { String name, ScmDescriptor desc ->
                elements[name] = new ScmFile.SourceFileElement(desc, file)
            }
        }
    }

    static class SourceFileElement {
        private ScmDescriptor descriptor
        private File script

        SourceFileElement(ScmDescriptor descriptor, File script) {
            assert descriptor
            assert script
            assert script.exists()

            this.descriptor = descriptor
            this.script = script
        }

        String simpleName() {
            return script.name.split('\\.')[0]
        }

        Map toMap(Map filter = [:], File parametersFile = null) {

            if (filter.name && !descriptor.name.contains(filter.name as CharSequence)) return null
            if (filter.src && !descriptor.src.contains(filter.src as CharSequence)) return null
            if (filter.entry && !descriptor.entry.contains(filter.entry as CharSequence)) return null

            Map externalProperties
            def lastModified = 0
            def saved = false

            if (parametersFile && parametersFile.exists()) {

                saved = true
                lastModified = parametersFile.lastModified()

                externalProperties = new JsonSlurper().parseText(parametersFile.text) as Map
            }

            def parameters = []
            def completedParameters = 0

            descriptor.ask.parameters.each { ScmDescriptor.AskParameter parameter ->

                String savedValue

                if (externalProperties)
                    savedValue = externalProperties?[descriptor.name]?[parameter.name]

                if (savedValue)
                    completedParameters++

                parameters << [
                        name: parameter.name,
                        usage: parameter.usage,
                        value: savedValue,
                        defaultValue: parameter.defaultValue,
                        values: [],
                        links: [
                                values: "/scms/parameters/values?name=${descriptor.name}&parameter=${parameter.name}",
                                save: "/scms/parameters?name=${descriptor.name}&parameter=${parameter.name}"
                        ]
                ]
            }

            if (externalProperties)
                externalProperties = null

            return [
                    name      : descriptor.name,
                    script: [
                        source    : simpleName(),
                        text      : script.text,
                        lastEdit  : script.lastModified()
                    ],
                    parameters: parameters,
                    lastModified: lastModified,
                    saved: saved,
                    completed: completedParameters == parameters.size(),
                    descriptor: [
                            name         : descriptor.name,
                            entry        : descriptor.entry,
                            hooks        : descriptor.hooks,
                            hasParameters: !descriptor.ask.parameters.isEmpty(),
                            src          : descriptor.src,
                            timeout      : descriptor.timeout
                    ],
                    links     : [
                            view       : "/scms/view?name=${descriptor.name}",
                            save      : "/scms/source?name=${descriptor.name}",
                            parameters: "/scms/parametersValues?name=${descriptor.name}"
                    ]
            ]
        }

        void writeParameterDefaultValue(File parametersFile, SourceFileElement element, ScmDescriptor.AskParameter parameter) {
            if (!parameter.defaultValue)
                return

            writeParameterValue(parametersFile, element, parameter.name, parameter.defaultValue)
        }

        void writeParameterValue(File parametersFile, SourceFileElement element, String parameter, String value) {

            def output = [
                    (element.descriptor.name): [
                            (parameter): value
                    ]
            ]

            if (parametersFile.exists()) {
                Map existing  = new JsonSlurper().parseText(parametersFile.text) as Map

                Closure merge
                merge = { Map base, Map extend ->
                    extend.each {
                        if (!base.containsKey(it.key)) {
                            base.put(it.key, it.value)
                            return
                        }

                        if (it.value instanceof Map)
                            merge(base[it.key] as Map, it.value as Map)
                    }
                }

                merge(existing, output)

                output = existing

                parametersFile.delete()
            }

            parametersFile << JsonOutput.prettyPrint(JsonOutput.toJson(output))
        }

        Map<String, List<String>> getParametersValues() {

            Map<String, List<String>> output = [:]

            descriptor.ask.parameters.each { ScmDescriptor.AskParameter parameter ->
                Logger.debug "Resolving '${parameter.name}' for ${descriptor.name}"

                List<String> values = parameter.staticValues ?: []

                if (parameter.commandValues) {
                    String stdout = parameter.commandValues.execute(descriptor.env2, descriptor.path).in.text

                    Logger.debug "Command: ${parameter.commandValues}:\n${stdout}"

                    if (!parameter.commandFilter)
                        values = stdout.split() as List<String>
                    else {
                        def matches = stdout =~ parameter.commandFilter

                        matches.each { match ->
                            if (match instanceof String) {
                                values.add(match.toString())
                            }

                            if (match instanceof ArrayList) {
                                values.add((match as ArrayList<String>)[1].toString())
                            }
                        }
                    }
                }

                output.put(parameter.name as String, values)
            }

            return output
        }
    }
}
