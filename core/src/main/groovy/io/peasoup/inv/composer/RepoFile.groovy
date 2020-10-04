package io.peasoup.inv.composer

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.transform.CompileStatic
import io.peasoup.inv.repo.RepoDescriptor
import io.peasoup.inv.repo.RepoExecutor
import io.peasoup.inv.repo.RepoInvoker
import io.peasoup.inv.run.Logger
import io.peasoup.inv.utils.Regexes

@CompileStatic
class RepoFile {

    final Map<String, SourceFileElement> elements = [:]

    final File scriptFile
    final String text
    final Long lastEdit

    final File expectedParameterFile

    RepoFile(File scriptFile) {
        assert scriptFile, 'ScriptFile is required.'
        assert scriptFile.exists(), 'ScriptFile must be present on current filesystem.'

        this.scriptFile = scriptFile
        this.text = scriptFile.text
        this.lastEdit = scriptFile.lastModified()

        // Resolve expected parameter file
        this.expectedParameterFile = RepoInvoker.expectedParametersfileLocation(scriptFile)

        // Do the actual parsing
        new RepoExecutor().with {

            if(expectedParameterFile && expectedParameterFile.exists())
               parse(scriptFile, expectedParameterFile)
            else
               parse(scriptFile)

            RepoFile myself = this
            repos.each { String name, RepoDescriptor desc ->
                elements[name] = new SourceFileElement(myself, desc)
            }
        }
    }

    /**
     Get filename without extension
     */
    String simpleName() {
        return scriptFile.name.split('\\.')[0]
    }

    static class SourceFileElement {
        final RepoDescriptor descriptor
        final RepoFile repoFile

        SourceFileElement(RepoFile repoFile, RepoDescriptor descriptor) {
            assert repoFile, 'RepoFile is required'
            assert descriptor, 'REPO descriptor is required'

            this.descriptor = descriptor
            this.repoFile = repoFile
        }

        /**
         Get filename without extension
         */
        String simpleName() {
            return repoFile.simpleName()
        }

        /**
         * Returns a Map representation of this RepoFile
         *
         * @param filter filter results, including [name, src, entry]
         * @param selectionState selection results [selected, staged]
         * @param parametersFile Optional parameters file to parse its values on parameters
         * @return Map object
         */
        Map toMap(Map filter = [:]) {

            if (filter.name && !descriptor.name.contains(filter.name as CharSequence)) return null
            if (filter.src && !descriptor.src.contains(filter.src as CharSequence)) return null
            if (filter.entry && !descriptor.entry.contains(filter.entry as CharSequence)) return null

            Map externalProperties
            def lastModified = 0
            def saved = false

            if (repoFile.expectedParameterFile.exists()) {
                String parametersFileText = repoFile.expectedParameterFile.text

                if (parametersFileText) {
                    saved = true
                    lastModified = repoFile.expectedParameterFile.lastModified()

                    externalProperties = new JsonSlurper().parseText(parametersFileText) as Map
                }
            }

            def parameters = []
            def completedParameters = 0

            descriptor.ask.parameters.each { RepoDescriptor.AskParameter parameter ->

                String savedValue

                if (externalProperties) {
                    savedValue = externalProperties[descriptor.name]?[parameter.name]
                }

                if (savedValue) {
                    completedParameters++
                }

                parameters << [
                        name        : parameter.name,
                        usage       : parameter.usage,
                        value       : savedValue,
                        defaultValue: parameter.defaultValue,
                        values      : [],
                        required    : parameter.required,
                        links       : [
                                values: WebServer.API_CONTEXT_ROOT + "/repos/parameters/values?name=${descriptor.name}&parameter=${parameter.name}",
                                save  : WebServer.API_CONTEXT_ROOT + "/repos/parameters?name=${descriptor.name}&parameter=${parameter.name}"
                        ]
                ]
            }

            if (externalProperties)
                externalProperties = null

            return [
                    name        : descriptor.name,
                    script      : [
                            source  : simpleName(),
                            text    : repoFile.text,
                            lastEdit: repoFile.lastEdit
                    ],
                    parameters  : parameters,
                    lastModified: lastModified,
                    saved       : saved,
                    completed   : completedParameters == parameters.size(),
                    staged      : false,
                    selected    : false,
                    descriptor  : [
                            name         : descriptor.name,
                            entry        : descriptor.entry,
                            hooks        : descriptor.hooks,
                            hasParameters: !descriptor.ask.parameters.isEmpty(),
                            src          : descriptor.src,
                            timeout      : descriptor.timeout
                    ],
                    links       : [
                            default   : WebServer.API_CONTEXT_ROOT + "/repos/view?name=${descriptor.name}",
                            stage     : WebServer.API_CONTEXT_ROOT + "/repos/stage?name=${descriptor.name}",
                            unstage   : WebServer.API_CONTEXT_ROOT + "/repos/unstage?name=${descriptor.name}",
                            save      : WebServer.API_CONTEXT_ROOT + "/repos/source?name=${descriptor.name}",
                            remove    : WebServer.API_CONTEXT_ROOT + "/repos/remove?name=${descriptor.name}",
                            parameters: WebServer.API_CONTEXT_ROOT + "/repos/parametersValues?name=${descriptor.name}"
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

            descriptor.ask.parameters.each { RepoDescriptor.AskParameter parameter ->
                Logger.system "[PARAMETER] repo: ${descriptor.name}, name: '${parameter.name}'"

                Collection<String> values = parameter.values ?: []

                if (parameter.command && descriptor.path.exists()) {
                    String stdout = parameter.command.execute(descriptor.set, descriptor.path).in.text

                    Logger.system "[PARAMETER] command: ${parameter.command}:${System.lineSeparator()}${stdout}"

                    values = stdout.split(Regexes.NEWLINES) as Collection<String>
                }

                Boolean hasFilter = parameter.filter != null
                def hasRegexFilter = parameter.filterRegex

                if (hasFilter || hasRegexFilter) {
                    Collection<String> copy = values.collect()
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
         * Write (or update) this RepoFile parameter file for a specific parameter with its default value
         *
         * @param parametersFile The writable parameters file
         * @param descriptorName The specific REPO descriptor
         * @param parameter The parameter
         */
        void writeParameterDefaultValue(String descriptorName, RepoDescriptor.AskParameter parameter) {
            assert descriptorName, "DescriptorName is required"
            assert parameter, "Parameter is required"

            if (!parameter.defaultValue)
                return

            writeParameterValue(
                    descriptorName,
                    parameter.name,
                    parameter.defaultValue)
        }

        /**
         * Write (or update) this RepoFile parameter file for a specific parameter with a specific value
         *
         * @param descriptorName The specific REPO descriptor
         * @param parameter The parameter
         * @param value The value
         */
        void writeParameterValue(String descriptorName, String parameter, String value) {
            assert descriptorName, "DescriptorName is required"
            assert parameter, "Parameter (name) is required"
            assert value != null, "Parameter (value) is required"

            def output = [
                    (descriptorName): [
                            (parameter): value
                    ]
            ]

            if (repoFile.expectedParameterFile.exists()) {
                String parametersFileText = repoFile.expectedParameterFile.text

                if (parametersFileText) {
                    Map existing = new JsonSlurper().parseText(parametersFileText) as Map

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

                    repoFile.expectedParameterFile.delete()
                }
            }

            repoFile.expectedParameterFile << JsonOutput.prettyPrint(JsonOutput.toJson(output))
        }
    }
}
