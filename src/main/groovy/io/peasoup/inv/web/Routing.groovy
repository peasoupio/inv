package io.peasoup.inv.web

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import io.peasoup.inv.scm.ScmDescriptor

import static spark.Spark.*

class Routing {

    private final String webLocation
    private final String runLocation
    private final String scmsLocation
    private final String parametersLocation
    private final String executionsLocation

    final private Settings settings
    final private RunFile run
    final private ScmFileCollection scms
    final private Execution exec

    Routing(Map args) {

         def configs = [
            port: 8080
        ] + args

        assert configs.workspace, "Args must include a 'workspace' (String) property."
        assert configs.workspace instanceof CharSequence, "Args.workspace must be a String type"

        runLocation = configs.workspace as String
        scmsLocation = configs.workspace + "/scms" as String
        parametersLocation = configs.workspace + "/parameters" as String
        executionsLocation = configs.workspace + "/executions" as String

        // Browser configs
        port(configs.port)

        // Static files
        def localWeb = System.getenv()["INV_LOCAL_WEB"]
        if (localWeb)
            staticFiles.externalLocation(localWeb)
        else
            staticFiles.location("/public")

        // Exception handling
        exception(Exception.class, { e, request, response ->
            final StringWriter sw = new StringWriter()
            final PrintWriter pw = new PrintWriter(sw, true)
            e.printStackTrace(pw)
            System.err.println(sw.getBuffer().toString())
        })

        // Init
        settings = new Settings(new File(runLocation, "settings.json"))
        run = new RunFile(new File(runLocation, "run.txt"))
        scms = new ScmFileCollection(new File(scmsLocation))
        exec = new Execution(new File(executionsLocation), new File(scmsLocation), new File(parametersLocation))


        // Process SETTINGS
        settings.staged().each {
            run.stageWithoutPropagate(it)
        }

        run.propagate()

        println "Ready!"
    }

    /**
     * Map available routes
     * @return
     */
    int map() {

        system()

        runsMany()
        runsSpecific()

        scmsMany()
        scmsSpecific()

        execution()

        review()

        return 0
    }

    // System-wise
    void system() {
        get("/stop", { req, res -> stop() })

        get("/api", { req, res ->
            return JsonOutput.toJson([
                links: [
                    run: [
                        default: "/run",
                        search: "/run",
                        owners: "/run/owners",
                        names: "/run/names",
                        selected: "/run/selected",
                        stageAll: "/run/stageAll",
                        unstageAll: "/run/unstageAll",
                    ],
                    scms: [
                        default: "/scms",
                        search: "/scms",
                        selected: "/scms/selected",
                        applyDefaultAll: "/scms/applyDefaultAll",
                        resetAll: "/scms/resetAll"
                    ],
                    execution: [
                        default: "/execution"
                    ],
                    review: [
                        default: "/review"
                    ]
                ]
            ])
        })
    }

    // Runs
    void runsMany() {
        get("/run", { req, res ->
            return JsonOutput.toJson(run.nodesToMap())
        })

        get("/run/owners", { req, res ->
            return JsonOutput.toJson(run.owners.keySet().collect {
                [
                        owner: it,
                        links: [
                                stage: "/run/stage?owner=${it}",
                                unstage: "/run/unstage?owner=${it}"
                        ]
                ]
            })
        })

        get("/run/names", { req, res ->
            return JsonOutput.toJson(run.names.keySet().collect {
                [
                        name: it,
                        links: [
                                stage: "/run/stage?name=${it}",
                                unstage: "/run/unstage?name=${it}"
                        ]
                ]
            })
        })

        post("/run", { req, res ->

            String body = req.body()
            Map filter = [:]

            if (body)
                filter = new JsonSlurper().parseText(body) as Map

            return JsonOutput.toJson(run.nodesToMap(
                    filter,
                    filter.from as Integer ?: 0,
                    filter.step as Integer ?: settings.filters().defaultStep))
        })

        post("/run/selected", { req, res ->

            String body = req.body()
            Map filter = [:]

            if (body)
                filter = new JsonSlurper().parseText(body) as Map

            filter.selected = true

            return JsonOutput.toJson(run.nodesToMap(
                    filter,
                    filter.from as Integer ?: 0,
                    filter.step as Integer ?: settings.filters().defaultStep))
        })

        post("/run/stageAll", { req, res ->

            run.stageAll()
            run.nodes.each {
                settings.stage(it.value)
            }

            settings.save()

            return showResult("Ok")
        })

        post("/run/unstageAll", { req, res ->

            run.unstageAll()
            settings.unstageAll()
            settings.save()

            return showResult("Ok")
        })
    }

    void runsSpecific() {
        get("/run/requiredBy", { req, res ->

            def id = req.queryParams("id")
            if (!id)
                return showError("id is required")

            return JsonOutput.toJson(run.requireByToMap(id))
        })

        post("/run/stage", { req, res ->

            def id = req.queryParams("id")
            if (!id)
                return showError("id is required")

            run.stage(id)
            settings.stage(id)
            settings.save()

            return showResult("Ok")
        })

        post("/run/unstage", { req, res ->

            def id = req.queryParams("id")
            if (!id)
                return showError("id is required")

            run.unstage(id)
            settings.unstage(id)
            settings.save()

            return showResult("Ok")
        })
    }

    // Scms
    void scmsMany() {

        get("/scms", { req, res ->

            return JsonOutput.toJson(scms.toMap([:], 0, settings.filters().defaultStep))
        })

        post("/scms", { req, res ->

            Map filter = [:]
            String body = req.body()

            if (body)
                filter = new JsonSlurper().parseText(body) as Map

            Integer from = filter.from as Integer ?: 0
            Integer to = filter.to as Integer ?: settings.filters().defaultStep

            return JsonOutput.toJson(scms.toMap(filter, from, to))
        })

        get("/scms/selected", { req, res ->

            Map output = [
                    descriptors: []
            ]

            scms.elements.values().each {
                if (!run.isSelected(it.descriptor.name))
                    return

                output.descriptors << it.toMap([:], new File(parametersLocation, it.simpleName() + ".json"))
            }

            return JsonOutput.toJson(output)
        })

        post("/scms/applyDefaultAll", { req, res ->
            scms.elements.values().each { ScmFile.SourceFileElement element ->

                if (!run.isSelected(element.descriptor.name))
                    return

                def parametersFile = new File(parametersLocation, element.simpleName() + ".json")

                element.descriptor.ask.parameters.each { ScmDescriptor.AskParameter parameter ->
                    element.writeParameterDefaultValue(parametersFile, element, parameter)
                }
            }

            return showResult("Ok")
        })

        post("/scms/resetAll", { req, res ->

            scms.elements.values().each { ScmFile.SourceFileElement element ->

                if (!run.isSelected(element.descriptor.name))
                    return

                def parametersFile = new File(parametersLocation, element.simpleName() + ".json")

                if (!parametersFile.exists())
                    return

                parametersFile.delete()
            }

            return showResult("Ok")
        })
    }

    void scmsSpecific() {

        get("/scms/view", { req, res ->

            def name = req.queryParams("name")
            if (!name)
                return showError("name is required")

            def element = scms.elements[name]
            if (!element)
                return showError("No parameter found for the specified name")

            def output = element.toMap([:], new File(parametersLocation, element.simpleName() + ".json"))

            return JsonOutput.toJson(output)
        })

        post("/scms/source", { req, res ->

            def name = req.queryParams("name")
            if (!name)
                return showError("name is required")

            def element = scms.elements[name]
            if (!element)
                return showError("No parameter found for the specified name")

            element.scriptFile.delete()
            element.scriptFile << req.body()

            scms.load(element.scriptFile)

            return showResult("Ok")
        })

        get("/scms/parametersValues", { req, res ->

            def name = req.queryParams("name")
            if (!name)
                return showError("name is required")

            def element = scms.elements[name]
            if (!element)
                return JsonOutput.toJson([:])

            return JsonOutput.toJson(element.getParametersValues())
        })

        post("/scms/parameters", { req, res ->

            def name = req.queryParams("name")
            if (!name)
                return showError("name is required")

            def parameter = req.queryParams("parameter")
            if (!parameter)
                return showError("parameter is required")

            def element = scms.elements[name]
            if (!element)
                return showError("No parameter found for the specified name")

            def payload = req.body()
            def parameterValue = payload

            if (payload)
                parameterValue = new JsonSlurper().parseText(payload).parameterValue

            def parametersFile = new File(parametersLocation, element.simpleName() + ".json")

            element.writeParameterValue(parametersFile, element, parameter, parameterValue.toString())

            return showResult("Ok")
        })
    }

    //Executions
    @SuppressWarnings("GroovyAssignabilityCheck")
    void execution() {

        get("/execution", { req, res ->
            return JsonOutput.toJson(exec.toMap())
        })

        get("/execution/logs/:ìndex", { req, res ->

            def index = req.params("ìndex")
            if (!index)
                return showError("ìndex is required")

            return JsonOutput.toJson(exec.messages[Integer.parseInt(index)])
        })

        post("/execution/start", { req, res ->
            if (exec.isRunning())
                return "Already running"

            List<File> scmFiles = run.selected.values()
                    .collect { run.runGraph.navigator.nodes[it.link.value] }
                    .findAll { it }
                    .collect { run.invOfScm[it.owner] }
                    .unique()
                    .collect { scms.elements[it].scriptFile } as List<File>

            exec.start(scmFiles)
            Thread.sleep(50)

            return JsonOutput.toJson([
                    files: scmFiles.collect { it.name }
            ])
        })

        post("/execution/stop", { req, res ->
            if (!exec.isRunning())
                return "Already stopped"

            exec.stop()
            Thread.sleep(50)

            return showResult("Stopped")
        })
    }

    void review() {
        get("/review", { req, res ->

            if (!exec.latestLog().exists())
                return showError("Latest execution log does not exists on filesystem")

            def review = new Review(new File(runLocation, "run.txt"), exec.latestLog())

            return JsonOutput.toJson(review.toMap())
        })
    }

    static String showError(String message) {
        return JsonOutput.toJson([
                error: message
        ])
    }

    static String showResult(String message) {
        return JsonOutput.toJson([
            result: message
        ])
    }

}
