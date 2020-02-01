package io.peasoup.inv.composer

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import io.peasoup.inv.graph.GraphNavigator
import io.peasoup.inv.scm.ScmDescriptor
import io.peasoup.inv.scm.ScmExecutor
import io.peasoup.inv.utils.Progressbar
import org.codehaus.groovy.control.MultipleCompilationErrorsException

import static spark.Spark.*

class WebServer {

    private final String webLocation
    private final String runLocation
    private final String scmsLocation
    private final String parametersLocation

    final private Settings settings
    private RunFile run
    final private ScmFileCollection scms
    final private Execution exec
    final private Review review

    final private Pagination pagination

    WebServer(Map args) {

         def configs = [
            port: 8080
        ] + args

        assert configs.workspace, "Args must include a 'workspace' (String) property."
        assert configs.workspace instanceof CharSequence, "Args.workspace must be a String type"

        runLocation = configs.workspace as String
        scmsLocation = configs.workspace + "/scms" as String
        parametersLocation = configs.workspace + "/parameters" as String

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
        exec = new Execution(new File(scmsLocation), new File(parametersLocation))
        review = new Review(new File(runLocation, "run.txt"), exec.latestLog())

        pagination = new Pagination(settings)

        // Process SETTINGS
        def staged = settings.staged()
        new Progressbar("Staging from 'settings.xml'", staged.size(), false).start {
            staged.each {
                run.stageWithoutPropagate(it)
                step()
            }
        }

        run.propagate()

        println "Ready!"
    }

    /**
     * Map available routes
     * @return
     */
    int map() {

        webSocket("/execution/log/stream", Execution.MessageStreamer.class)

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
                        default: "/review",
                        promote: "/review/promote"
                    ]
                ]
            ])
        })
    }

    // Runs
    void runsMany() {
        get("/run", { req, res ->
            def output = run.nodesToMap()
            output.nodes = pagination.resolve(output.nodes)

            return JsonOutput.toJson(output)
        })

        get("/run/owners", { req, res ->
            return JsonOutput.toJson(run.owners.collect { String owner, List<GraphNavigator.Id> ids ->
                [
                    owner: owner,
                    selectedBy: ids.findAll { run.selected[it.value] && run.selected[it.value].selected }.size(),
                    requiredBy: ids.findAll { run.selected[it.value] && run.selected[it.value].required }.size(),
                    links: [
                            stage: "/run/stage?owner=${owner}",
                            unstage: "/run/unstage?owner=${owner}"
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

            def output = run.nodesToMap(filter)

            Integer from = filter.from as Integer
            Integer to = filter.to as Integer
            output.nodes = pagination.resolve(output.nodes, from, to)

            return JsonOutput.toJson(output)
        })

        post("/run/selected", { req, res ->

            String body = req.body()
            Map filter = [:]

            if (body)
                filter = new JsonSlurper().parseText(body) as Map

            filter.selected = true

            def output = run.nodesToMap(filter)

            Integer from = filter.from as Integer
            Integer to = filter.to as Integer
            output.nodes = pagination.resolve(output.nodes, from, to)

            return JsonOutput.toJson(output)
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
            if (id) {
                run.stage(id)
                settings.stage(id)
                settings.save()

                return showResult("Ok")
            }

            def owner = req.queryParams("owner")
            if (owner && run.owners[owner]) {
                run.owners[owner].each {
                    run.stage(it.value)
                    settings.stage(it.value)
                    settings.save()
                }

                return showResult("Ok")
            }


            return showError("Nothing was done")
        })

        post("/run/unstage", { req, res ->

            def id = req.queryParams("id")
            if (id) {
                run.unstage(id)
                settings.unstage(id)
                settings.save()

                return showResult("Ok")
            }

            def owner = req.queryParams("owner")
            if (owner && run.owners[owner]) {
                run.owners[owner].each {
                    run.unstage(it.value)
                    settings.unstage(it.value)
                    settings.save()
                }

                return showResult("Ok")
            }

            return showError("Nothing was done")
        })
    }

    // Scms
    void scmsMany() {

        get("/scms", { req, res ->
            def output = scms.toMap()
            output.descriptors = pagination.resolve(output.descriptors)

            return JsonOutput.toJson(output)
        })

        post("/scms", { req, res ->

            Map filter = [:]
            String body = req.body()

            if (body)
                filter = new JsonSlurper().parseText(body) as Map

            def output = scms.toMap(filter)

            Integer from = filter.from as Integer
            Integer to = filter.to as Integer
            output.descriptors = pagination.resolve(output.descriptors, from, to)

            return JsonOutput.toJson(output)
        })

        post("/scms/selected", { req, res ->

            Map filter = [:]
            String body = req.body()

            if (body)
                filter = new JsonSlurper().parseText(body) as Map

            Map output = [
                    descriptors: []
            ]

            scms.elements.values().each {
                if (!run.isSelected(it.descriptor.name))
                    return

                output.descriptors << it.toMap(filter, new File(parametersLocation, it.simpleName() + ".json"))
            }

            output.total = output.descriptors.size()
            output.descriptors = pagination.resolve(
                    output.descriptors,
                    filter.from as Integer,
                    filter.to as Integer)

            return JsonOutput.toJson(output)
        })

        post("/scms/applyDefaultAll", { req, res ->
            scms.elements.values().each { ScmFile.SourceFileElement element ->

                if (!run.isSelected(element.descriptor.name))
                    return

                def parametersFile = new File(parametersLocation, element.simpleName() + ".json")

                element.descriptor.ask.parameters.each { ScmDescriptor.AskParameter parameter ->
                    element.writeParameterDefaultValue(parametersFile, element.descriptor.name, parameter)
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

            // Make sure to get the latest information
            if (!scms.reload(name))
                return showError("No SCM found for the specified name")

            def element = scms.elements[name]
            def output = element.toMap([:], new File(parametersLocation, element.simpleName() + ".json"))

            return JsonOutput.toJson(output)
        })

        post("/scms/source", { req, res ->

            def name = req.queryParams("name")
            if (!name)
                return showError("name is required")

            def element = scms.elements[name]
            if (!element)
                return showError("No descriptor found for the specified name")

            String source = req.body()
            Integer errorCount = 0
            List<String> exceptionMessages = []


            try {
                new GroovyShell().parse(source)
            } catch(MultipleCompilationErrorsException ex) {
                errorCount = ex.errorCollector.errorCount
                exceptionMessages = ex.errorCollector.errors.collect { it.cause.toString() }
            }

            if (errorCount == 0) {
                element.scriptFile.delete()
                element.scriptFile << req.body()

                scms.load(element.scriptFile)
            }

            return JsonOutput.toJson([
                    errorCount: errorCount,
                    errors: exceptionMessages
            ])
        })

        get("/scms/parametersValues", { req, res ->

            def name = req.queryParams("name")
            if (!name)
                return showError("name is required")

            if (!scms.reload(name))
                return JsonOutput.toJson([:])

            def latestElement = scms.elements[name]

            // Make sure we atleast try to get repository with default or no parameters
            List<ScmExecutor.SCMReport> report = new ScmExecutor().with {
                add(latestElement.descriptor)
                return execute()
            }

            if (report[0].isOk)
                return JsonOutput.toJson(latestElement.getParametersValues())
            else
                return showError("could not extract SCM")
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

            element.writeParameterValue(
                    parametersFile,
                    element.descriptor.name,
                    parameter,
                    parameterValue.toString())

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

            List<File> scmFiles = scms.toFiles(run.selectedScms()) as List<File>

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

            return JsonOutput.toJson(review.toMap())
        })

        post("/review/promote", { req, res ->
            if (!exec.latestLog().exists())
                return showError("Latest execution log does not exists on filesystem")

            if (!review.promote())
                return showError("failed to promote")


            // Recalculate RunFile
            run = new RunFile(new File(runLocation, "run.txt"))
            settings.unstageAll()
            settings.save()

            return showResult("promoted")

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
