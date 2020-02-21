package io.peasoup.inv.composer

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import io.peasoup.inv.graph.GraphNavigator
import io.peasoup.inv.run.RunsRoller
import io.peasoup.inv.scm.ScmDescriptor
import io.peasoup.inv.scm.ScmExecutor
import io.peasoup.inv.security.CommonLoader
import io.peasoup.inv.utils.Progressbar
import org.codehaus.groovy.control.MultipleCompilationErrorsException
import spark.Request
import spark.Response

import static spark.Spark.*

@CompileStatic
class WebServer {

    Map configs = [:]

    private final String webLocation
    private final String runLocation
    private final String scmsLocation
    private final String parametersLocation

    final private Settings settings
    final private ScmFileCollection scms
    final private Execution exec

    private RunFile run
    private Review review

    final private Pagination pagination

    @CompileDynamic
    WebServer(Map args) {

        configs = [
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

        def scmsLocationFolder = new File(scmsLocation)
        if (!scmsLocationFolder.exists())
            scmsLocationFolder.mkdirs()

        // Init
        settings = new Settings(new File(runLocation, "settings.json"))
        scms = new ScmFileCollection(scmsLocationFolder)
        exec = new Execution(scmsLocationFolder, new File(parametersLocation))
        review = new Review()

        pagination = new Pagination(settings)

        def runFile = baseFile()
        if (runFile.exists())
            run = new RunFile(runFile)

        // Process SETTINGS
        def stagedIds = settings.stagedIds()
        def stagedScms = settings.stagedSCMs()
        new Progressbar("Staging from 'settings.xml'", stagedIds.size() + stagedScms.size(), false).start {
            stagedIds.each {
                run.stageWithoutPropagate(it)
                step()
            }

            stagedScms.each {
                scms.stage(it)
                step()
            }
        }

        println "Checking for 'run.txt'..."

        if (run) {
            run.propagate()

            println "Found ${run.owners.size()} INV(s)"
            println "Found ${run.names.size()} unique name(s)"
            println "Found ${run.nodes.size()} broadcast(s)"
        } else {
            println "Not present right now."
        }

        println "Ready and listening on http://localhost:${configs.port}"
    }

    /**
     * Map available routes
     * @return
     */
    @CompileDynamic
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
        get("/stop", { Request req, Response res -> stop() })

        get("/api", { Request req, Response res ->
            return JsonOutput.toJson([
                links: [
                    setup: "/setup",
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
                        stageAll: "/scms/stageAll",
                        unstageAll: "/scms/unstageAll",
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

        get("/setup", { Request req, Response res ->
            return JsonOutput.toJson([
                firstTime: run == null,
                configs: configs
            ])
        })
    }

    // Runs
    @CompileDynamic
    void runsMany() {
        get("/run", { Request req, Response res ->
            if (!run)
                return showError(res, "Run is not ready yet")

            def output = run.nodesToMap()
            output.nodes = pagination.resolve(output.nodes)

            return JsonOutput.toJson(output)
        })

        get("/run/owners", { Request req, Response res ->
            if (!run)
                return showError(res, "Run is not ready yet")

            return JsonOutput.toJson(run.owners.collect { String owner, List<GraphNavigator.Id> ids ->
                [
                    owner: owner,
                    selectedBy: ids.findAll { run.staged[it.value] && run.staged[it.value].selected }.size(),
                    requiredBy: ids.findAll { run.staged[it.value] && run.staged[it.value].required }.size(),
                    links: [
                            stage: "/run/stage?owner=${owner}",
                            unstage: "/run/unstage?owner=${owner}"
                    ]
                ]
            })
        })

        get("/run/names", { Request req, Response res ->
            if (!run)
                return showError(res, "Run is not ready yet")

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

        post("/run", { Request req, Response res ->
            if (!run)
                return showError(res, "Run is not ready yet")

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

        post("/run/selected", { Request req, Response res ->
            if (!run)
                return showError(res, "Run is not ready yet")

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

        post("/run/stageAll", { Request req, Response res ->
            if (!run)
                return showError(res, "Run is not ready yet")

            run.stageAll()
            run.nodes.each {
                settings.stageId(it.value)
            }

            settings.save()

            return showResult("Ok")
        })

        post("/run/unstageAll", { Request req, Response res ->
            if (!run)
                return showError(res, "Run is not ready yet")

            run.unstageAll()
            settings.unstageAllIds()
            settings.save()

            return showResult("Ok")
        })
    }

    void runsSpecific() {
        get("/run/requiredBy", { Request req, Response res ->
            if (!run)
                return showError(res, "Run is not ready yet")

            def id = req.queryParams("id")
            if (!id)
                return showError(res, "id is required")

            return JsonOutput.toJson(run.requireByToMap(id))
        })

        post("/run/stage", { Request req, Response res ->
            if (!run)
                return showError(res, "Run is not ready yet")

            def id = req.queryParams("id")
            if (id) {
                run.stage(id)
                settings.stageId(id)
                settings.save()

                return showResult("Ok")
            }

            def owner = req.queryParams("owner")
            if (owner && run.owners[owner]) {
                run.owners[owner].each { GraphNavigator.Linkable graphId ->
                    run.stage(graphId.value)
                    settings.stageId(graphId.value)
                    settings.save()
                }

                return showResult("Ok")
            }


            return showError(res, "Nothing was done")
        })

        post("/run/unstage", { Request req, Response res ->
            if (!run)
                return showError(res, "Run is not ready yet")

            def id = req.queryParams("id")
            if (id) {
                run.unstage(id)
                settings.unstageId(id)
                settings.save()

                return showResult("Ok")
            }

            def owner = req.queryParams("owner")
            if (owner && run.owners[owner]) {
                run.owners[owner].each { GraphNavigator.Linkable graphId ->
                    run.unstage(graphId.value)
                    settings.unstageId(graphId.value)
                    settings.save()
                }

                return showResult("Ok")
            }

            return showError(res, "Nothing was done")
        })
    }

    // Scms
    @CompileDynamic
    void scmsMany() {

        get("/scms", { Request req, Response res ->
            Map output = scms.toMap(run)
            output.descriptors = pagination.resolve(output.descriptors)

            return JsonOutput.toJson(output)
        })

        post("/scms", { Request req, Response res ->

            Map filter = [:]
            String body = req.body()

            if (body)
                filter = new JsonSlurper().parseText(body) as Map

            Map output = scms.toMap(run, filter, parametersLocation)
            output.descriptors = pagination.resolve(
                    output.descriptors,
                    filter.from as Integer,
                    filter.to as Integer)

            return JsonOutput.toJson(output)
        })

        post("/scms/stageAll", { Request req, Response res ->
            settings.unstageAllSCMs()

            scms.elements.keySet().each {
                scms.stage(it)
                settings.stageSCM(it)
            }
            settings.save()

            return showResult("staged all")
        })

        post("/scms/unstageAll", { Request req, Response res ->
            settings.unstageAllSCMs()

            scms.elements.keySet().each {
                scms.unstage(it)
            }
            settings.save()

            return showResult("unstaged all")
        })

        post("/scms/applyDefaultAll", { Request req, Response res ->
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

        post("/scms/resetAll", { Request req, Response res ->

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

    @CompileDynamic
    void scmsSpecific() {

        get("/scms/view", { Request req, Response res ->

            def name = req.queryParams("name")
            if (!name)
                return showError(res, "name is required")

            // Make sure to get the latest information
            if (!scms.reload(name))
                return showError(res, "No SCM found for the specified name")

            def element = scms.elements[name]
            def output = element.toMap([:], new File(parametersLocation, element.simpleName() + ".json"))

            return JsonOutput.toJson(output)
        })

        post("/scms/stage", { Request req, Response res ->

            def name = req.queryParams("name")
            if (!name)
                return showError(res, "name is required")

            scms.stage(name)
            settings.stageSCM(name)
            settings.save()

            return showResult("staged")
        })

        post("/scms/unstage", { Request req, Response res ->

            def name = req.queryParams("name")
            if (!name)
                return showError(res, "name is required")

            scms.unstage(name)
            settings.unstageSCM(name)
            settings.save()

            return showResult("unstaged")
        })

        post("/scms/source", { Request req, Response res ->

            def name = req.queryParams("name")
            if (!name)
                return showError(res, "name is required")

            def element = scms.elements[name]
            if (!element)
                return showError(res, "No descriptor found for the specified name")

            String source = req.body()
            Integer errorCount = 0
            List<String> exceptionMessages = []


            try {
                new CommonLoader().parseClass(source)
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

        get("/scms/parametersValues", { Request req, Response res ->

            def name = req.queryParams("name")
            if (!name)
                return showError(res, "name is required")

            if (!scms.reload(name))
                return JsonOutput.toJson([:])

            def latestElement = scms.elements[name]

            // Make sure we at least try to get repository with default or no parameters
            List<ScmExecutor.SCMReport> report = new ScmExecutor().with {
                add(latestElement.descriptor)
                return execute()
            }

            if (report[0].isOk)
                return JsonOutput.toJson(latestElement.getParametersValues())
            else
                return showError(res, "could not extract SCM")
        })

        post("/scms/parameters", { Request req, Response res ->

            def name = req.queryParams("name")
            if (!name)
                return showError(res, "name is required")

            def parameter = req.queryParams("parameter")
            if (!parameter)
                return showError(res, "parameter is required")

            def element = scms.elements[name]
            if (!element)
                return showError(res, "No parameter found for the specified name")

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
    @CompileDynamic
    @SuppressWarnings("GroovyAssignabilityCheck")
    void execution() {
        get("/execution", { Request req, Response res ->
            return JsonOutput.toJson(exec.toMap())
        })

        get("/execution/logs/:ìndex", { Request req, Response res ->

            def index = req.params("ìndex")
            if (!index)
                return showError("ìndex is required")

            return JsonOutput.toJson(exec.messages[Integer.parseInt(index)])
        })

        post("/execution/start", { Request req, Response res ->
            if (exec.isRunning())
                return "Already running"

            List<String> toExecute = []
            toExecute += scms.staged

            def debugMode = false
            def secureMode = false

            def body = req.body()
            if (body) {
                def options = new JsonSlurper().parseText(body) as Map
                debugMode = options.debugMode
                secureMode = options.secureMode
            }

            if (run)
                toExecute += run.selectedScms()

            List<File> scmFiles = scms.toFiles(toExecute.unique()) as List<File>

            exec.start(
                    debugMode,
                    secureMode,
                    scmFiles)
            Thread.sleep(50)

            return JsonOutput.toJson([
                    files: scmFiles.collect { it.name }
            ])
        })

        post("/execution/stop", { Request req, Response res ->
            if (!exec.isRunning())
                return "Already stopped"

            exec.stop()
            Thread.sleep(50)

            return showResult("Stopped")
        })
    }

    void review() {
        get("/review", { Request req, Response res ->
            if (!exec.latestLog().exists())
                return showError(res, "Latest execution log does not exists on filesystem")

            def base = baseFile()

            if (!base.exists()) {
                if (!exec.latestLog().exists())
                    return showError(res, "Review is not ready yet")
                else {
                    base = new File(RunsRoller.latest.folder(), ".base.tmp")
                    base.delete()
                    base << "Generated automatically by Composer" + System.lineSeparator()
                    base << "This file is used to allow review on a first run since no promoted run.txt exists" + System.lineSeparator()
                }
            }

            return JsonOutput.toJson(review.compare(base, exec.latestLog()))
        })

        post("/review/promote", { Request req, Response res ->
            if (baseFile().exists())
                review.mergeWithBase(baseFile())

            if (!review.promote())
                return showError(res, "failed to promote")

            // Recalculate RunFile
            run = new RunFile(baseFile())
            scms.elements.keySet().each {
                scms.unstage(it)
            }
            settings.unstageAllIds()
            settings.unstageAllSCMs()
            settings.save()

            return showResult("promoted")
        })
    }

    File baseFile() {
        return new File(runLocation, "run.txt")
    }

    static String showError(Response res, String message) {
        res.status(500)

        return JsonOutput.toJson([
                message: message,
                when: System.currentTimeMillis()
        ])
    }

    static String showResult(String message) {
        return JsonOutput.toJson([
            result: message
        ])
    }
}
