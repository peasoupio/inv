package io.peasoup.inv.composer

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.transform.CompileStatic
import io.peasoup.inv.run.Logger
import io.peasoup.inv.scm.ScmDescriptor
import io.peasoup.inv.scm.ScmExecutor
import io.peasoup.inv.utils.Regexes

@CompileStatic
class ScmFile {

    final File sourceFile
    final Map<String, SourceFileElement> elements = [:]

    String text
    Long lastEdit

    ScmFile(File file) {

        sourceFile = file

        text = file.text
        lastEdit = file.lastModified()

        new ScmExecutor().with {
            read(file)

            scms.each { String name, ScmDescriptor desc ->
                elements[name] = new SourceFileElement(desc, file)
            }
        }
    }

    static class SourceFileElement {
        final ScmDescriptor descriptor
        final File scriptFile

        SourceFileElement(ScmDescriptor descriptor, File scripFile) {
            assert descriptor, 'SCM descriptor is required'
            assert scripFile, 'Script file is required'
            assert scripFile.exists(), 'Script file must exist on filesystem'

            this.descriptor = descriptor
            this.scriptFile = scripFile
        }

        /**
            Get filename without extension
         */
        String simpleName() {
            return scriptFile.name.split('\\.')[0]
        }

        /**
         * Returns a Map representation of this ScmFile
         *
         * @param filter filter results, including [name, src, entry]
         * @param selectionState selection results [selected, staged]
         * @param parametersFile Optional parameters file to parse its values on parameters
         * @return Map object
         */
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

                if (externalProperties) {
                    savedValue = externalProperties[descriptor.name][parameter.name]
                }

                if (savedValue) {
                    completedParameters++
                }

                parameters << [
                        name: parameter.name,
                        usage: parameter.usage,
                        value: savedValue,
                        defaultValue: parameter.defaultValue,
                        values: [],
                        required: parameter.required,
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
                    staged: false,
                    selected: false,
                    descriptor: [
                            name         : descriptor.name,
                            entry        : descriptor.entry,
                            hooks        : descriptor.hooks,
                            hasParameters: !descriptor.ask.parameters.isEmpty(),
                            src          : descriptor.src,
                            timeout      : descriptor.timeout
                    ],
                    links     : [
                            default   : "/scms/view?name=${descriptor.name}",
                            stage     : "/scms/stage?name=${descriptor.name}",
                            unstage   : "/scms/unstage?name=${descriptor.name}",
                            save      : "/scms/source?name=${descriptor.name}",
                            parameters: "/scms/parametersValues?name=${descriptor.name}"
                    ]
            ]
        }

        /**
         *  Resolved parameters values based on which type it is.
         *  NOTE: For commandValues this method IS STARTING the actual process.
         *
         * @return A Map representation of the parameters values
         */
        Map<String, List<String>> getParametersValues() {

            Map<String, List<String>> output = [:]

            descriptor.ask.parameters.each { ScmDescriptor.AskParameter parameter ->
                Logger.debug "[PARAMETER] scm: ${descriptor.name}, name: '${parameter.name}'"

                List<String> values = parameter.values ?: []

                if (parameter.command) {
                    String stdout = parameter.command.execute(descriptor.set, descriptor.path).in.text

                    Logger.debug "[PARAMETER] command: ${parameter.command}:${System.lineSeparator()}${stdout}"

                    values = stdout.split(Regexes.NEWLINES) as List<String>
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
                        }
                    }
                }

                output.put(
                    parameter.name as String,
                    values
                        .findAll()
                        .collect { it.trim() }
                        .unique())
            }

            return output
        }


        /**
         * Write (or update) this ScmFile parameter file for a specific parameter with its default value
         *
         * @param parametersFile The writable parameters file
         * @param descriptorName The specific SCM descriptor
         * @param parameter The parameter
         */
        void writeParameterDefaultValue(File parametersFile, String descriptorName, ScmDescriptor.AskParameter parameter) {
            assert parametersFile, 'ParametersFile is required'
            assert descriptorName, "DescriptorName is required"
            assert parameter, "Parameter is required"

            if (!parameter.defaultValue)
                return

            writeParameterValue(
                    parametersFile,
                    descriptorName,
                    parameter.name,
                    parameter.defaultValue)
        }

        /**
         * Write (or update) this ScmFile parameter file for a specific parameter with a specific value
         *
         * @param parametersFile The writable parameters file
         * @param descriptorName The specific SCM descriptor
         * @param parameter The parameter
         * @param value The value
         */
        void writeParameterValue(File parametersFile, String descriptorName, String parameter, String value) {
            assert parametersFile, 'ParametersFile is required'
            assert descriptorName, "DescriptorName is required"
            assert parameter, "Parameter (name) is required"
            assert value != null, "Parameter (value) is required"

            def output = [
                    (descriptorName): [
                            (parameter): value
                    ]
            ]

            if (parametersFile.exists()) {
                Map existing  = new JsonSlurper().parseText(parametersFile.text) as Map

                Closure merge
                merge = { Map base, Map extend ->
                    extend.each {
                        if (base.containsKey(it.key) && it.value instanceof Map) {
                            merge(base[it.key] as Map, it.value as Map)
                            return
                        }

                        base.put(it.key, it.value)
                    }
                }

                merge(existing, output)

                output = existing

                parametersFile.delete()
            }

            parametersFile << JsonOutput.prettyPrint(JsonOutput.toJson(output))
        }
    }
}
