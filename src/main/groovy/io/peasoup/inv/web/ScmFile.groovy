package io.peasoup.inv.web

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.transform.CompileStatic
import io.peasoup.inv.scm.ScmDescriptor
import io.peasoup.inv.scm.ScmDescriptor.AskParameter

import java.util.regex.Matcher

@CompileStatic
class ScmFile {

    final File sourceFile
    final ScmDescriptor scmDescriptor
    final Map<String, ScmFile.SourceFileElement> elements = [:]

    String text
    Long lastEdit

    ScmFile(File file) {

        sourceFile = file
        scmDescriptor = new ScmDescriptor(file.newReader())

        text = file.text
        lastEdit = file.lastModified()

        scmDescriptor.scms().each { String name, ScmDescriptor.MainDescriptor desc ->
            def element = new ScmFile.SourceFileElement(
                descriptor: desc,
                script: file.name
            )

            elements[name] = element
        }
    }

    static class SourceFileElement {
        ScmDescriptor.MainDescriptor descriptor
        String script

        String simpleName() {
            return script.split('\\.')[0]
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
                    source    : script,
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
                            view       : "/scm/view?name=${descriptor.name}",
                            save      : "/scms/source?name=${descriptor.name}",
                            parameters: "/scms/parametersValues?name=${descriptor.name}"
                    ]
            ]
        }

        void writeParameterDefaultValue(File parametersFile, SourceFileElement element, AskParameter parameter) {
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

                List<String> values = parameter.staticValues

                if (parameter.commandValues) {
                    String stdout = parameter.commandValues.execute().in.text

                    if (parameter.filter) {
                        def matches = stdout =~ parameter.filter
                        matches.each { Matcher match ->
                            values.add(match[1] as String)
                        }
                    } else {
                        values = stdout.split() as List<String>
                    }
                }

                output.put(parameter.name as String, values)
            }

            return output
        }
    }
}
