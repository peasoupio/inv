package io.peasoup.inv.composer

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.transform.CompileStatic
import io.peasoup.inv.Logger
import io.peasoup.inv.composer.utils.MapUtils
import io.peasoup.inv.repo.RepoDescriptor
import io.peasoup.inv.repo.RepoExecutor
import io.peasoup.inv.repo.RepoInvoker
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

            if (expectedParameterFile && expectedParameterFile.exists())
                addScript(scriptFile, expectedParameterFile)
            else
                addScript(scriptFile)

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
         * @param secure Indicate if the calling request is secure or not
         * @param filter filter results, including [name, src, entry]
         * @return Map object
         */
        Map toMap(boolean secure, Map filter = [:]) {

            if (filter.name && !descriptor.name.contains(filter.name as CharSequence)) return null
            if (filter.src && !descriptor.src.contains(filter.src as CharSequence)) return null

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
                    savedValue = externalProperties[descriptor.name] ?[parameter.name]
                }

                if (savedValue) {
                    completedParameters++
                }

                def parameterMap = [
                        name        : parameter.name,
                        usage       : parameter.usage,
                        value       : savedValue,
                        defaultValue: parameter.defaultValue,
                        values      : [],
                        required    : parameter.required,
                        links       : []
                ]

                if (secure)
                    MapUtils.merge(parameterMap, [
                            links: [
                                    save: WebServer.API_CONTEXT_ROOT + "/repos/parameters?name=${descriptor.name}&parameter=${parameter.name}"
                            ]
                    ])

                parameters.add(parameterMap)
            }

            if (externalProperties)
                externalProperties = null

            def repoMap = [
                    name        : descriptor.name,
                    script      : [
                            source  : simpleName(),
                            mimeType: descriptor.mimeType(),
                            text    : repoFile.text,
                            lastEdit: repoFile.lastEdit
                    ],
                    parameters  : parameters,
                    lastModified: lastModified,
                    saved       : saved,
                    completed   : completedParameters == parameters.size(),
                    staged      : false,
                    required    : false,
                    descriptor  : [
                            name         : descriptor.name,
                            hooks        : descriptor.hooks,
                            hasParameters: !descriptor.ask.parameters.isEmpty(),
                            src          : descriptor.src,
                            timeout      : descriptor.timeout
                    ],
                    links       : [
                            default         : WebServer.API_CONTEXT_ROOT + "/repos/view?name=${descriptor.name}",
                            parametersValues: WebServer.API_CONTEXT_ROOT + "/repos/parametersValues?name=${descriptor.name}"
                    ]
            ]

            if (secure)
                MapUtils.merge(repoMap, [
                        links: [
                                stage  : WebServer.API_CONTEXT_ROOT + "/repos/stage?name=${descriptor.name}",
                                unstage: WebServer.API_CONTEXT_ROOT + "/repos/unstage?name=${descriptor.name}",
                                save   : WebServer.API_CONTEXT_ROOT + "/repos/source?name=${descriptor.name}&mimeType=${descriptor.mimeType()}",
                                remove : WebServer.API_CONTEXT_ROOT + "/repos/remove?name=${descriptor.name}",
                        ]
                ])

            return repoMap
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

                if (parameter.command && descriptor.repoPath.exists()) {
                    String stdout = parameter.command.execute(descriptor.currentOSSet, descriptor.repoPath).in.text

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

                    MapUtils.merge(existing, output)
                    output = existing

                    repoFile.expectedParameterFile.delete()
                }
            }

            repoFile.expectedParameterFile << JsonOutput.prettyPrint(JsonOutput.toJson(output))
        }
    }
}
