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
        private File scriptFile

        SourceFileElement(ScmDescriptor descriptor, File scripFile) {
            assert descriptor, 'SCM descriptor is required'
            assert scripFile, 'Script file is required'
            assert scripFile.exists(), 'Script file must exist on filesystem'

            this.descriptor = descriptor
            this.scriptFile = scripFile
        }

        String simpleName() {
            return scriptFile.name.split('\\.')[0]
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
                        text      : scriptFile.text,
                        lastEdit  : scriptFile.lastModified()
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

                List<String> values = parameter.values ?: []

                if (parameter.command) {
                    String stdout = parameter.command.execute(descriptor.env2, descriptor.path).in.text

                    Logger.debug "Command: ${parameter.command}:\n${stdout}"

                    values = stdout.split(System.lineSeparator()) as List<String>
                }

                Boolean hasFilter = parameter.filter != null
                def hasRegexFilter = parameter.filterRegex

                if (hasFilter || hasRegexFilter) {
                    List<String> copy = values.collect()
                    values.clear()

                    for (String value : copy) {

                        if (!value)
                            continue

                        if (hasFilter) {
                            String result = parameter.filter.call(value) as String

                            if (!result)
                                continue

                            values.add(result)
                            continue
                        }

                        if (hasRegexFilter) {
                            def matches = value =~ parameter.filterRegex

                            if (!matches.matches())
                                continue

                            if (matches.groupCount() == 0)
                                values.add(matches.group(0) as String)

                            if (matches.groupCount() == 1) {
                                values.add(matches.group(1) as String)
                            }

                            continue
                        }
                    }
                }

                output.put(parameter.name as String, values)
            }

            return output
        }
    }
}
