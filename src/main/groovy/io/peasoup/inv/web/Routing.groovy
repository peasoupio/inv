package io.peasoup.inv.web

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import io.peasoup.inv.scm.ScmDescriptor
import me.tongfei.progressbar.ProgressBar

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

        assert configs.workspace

        runLocation = configs.workspace
        scmsLocation = configs.workspace + "/scms"
        parametersLocation = configs.workspace + "/parameters"
        executionsLocation = configs.workspace + "/executions"

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
            final StringWriter sw = new StringWriter();
            final PrintWriter pw = new PrintWriter(sw, true);
            e.printStackTrace(pw);
            System.err.println(sw.getBuffer().toString());
        })

        // Init
        settings = new Settings(new File(runLocation, "settings.json"))
        run = new RunFile(new File(runLocation, "run.txt"))
        scms = new ScmFileCollection(new File(scmsLocation))
        exec = new Execution(new File(executionsLocation), new File(scmsLocation), new File(parametersLocation))


        // Process SETTINGS
        ProgressBar pb = new ProgressBar("Calculating pre-staged", settings.staged().size())
        settings.staged().each {
            run.stageWithoutPropagate(it)
            pb.step()
        }

        run.propagate()

        pb.close()
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
            return JsonOutput.toJson(run.owners)
        })

        get("/run/names", { req, res ->
            return JsonOutput.toJson(run.names)
        })

        post("/run", { req, res ->

            String body = req.body()
            Map filter = [:]

            if (body)
                filter = new JsonSlurper().parseText(body)

            return JsonOutput.toJson(run.nodesToMap(filter, filter.from ?: 0, filter.step ?: settings.filters().defaultStep))
        })

        post("/run/selected", { req, res ->

            String body = req.body()
            Map filter = [:]

            if (body)
                filter = new JsonSlurper().parseText(body)

            filter.selected = true

            return JsonOutput.toJson(run.nodesToMap(filter,  filter.from ?: 0, filter.step ?: settings.filters().defaultStep))
        })

        post("/run/stageAll", { req, res ->

            run.stageAll()
            run.nodes.each {
                settings.stage(it.value)
            }

            settings.save()

            return "Ok"
        })

        post("/run/unstageAll", { req, res ->

            run.unstageAll()
            settings.unstageAll()
            settings.save()

            return "Ok"
        })
    }

    void runsSpecific() {
        get("/run/requiredBy", { req, res ->

            def id = req.queryParams("id")
            assert id

            return JsonOutput.toJson(run.requireByToMap(id))
        })

        post("/run/stage", { req, res ->

            def id = req.queryParams("id")
            assert id

            run.stage(id)
            settings.stage(id)
            settings.save()

            return "Ok"
        })

        post("/run/unstage", { req, res ->

            def id = req.queryParams("id")
            assert id

            run.unstage(id)
            settings.unstage(id)
            settings.save()

            return "Ok"
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
                filter = new JsonSlurper().parseText(body)

            Integer from = filter.from ?: 0
            Integer to = filter.to ?: settings.filters().defaultStep

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

            return "Ok"
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

            return "Ok"

        })
    }

    void scmsSpecific() {

        get("/scms/view", { req, res ->

            def name = req.queryParams("name")
            assert name

            def element = scms.elements[name]

            if (!element)
                return "Oups"

            def output = element.toMap([:], new File(parametersLocation, element.simpleName() + ".json"))

            return JsonOutput.toJson(output)
        })

        post("/scms/source", { req, res ->

            def name = req.queryParams("name")
            assert name

            def element = scms.elements[name]

            element.script.delete()
            element.script << req.body()

            scms.load(element.script)

            return "Ok"
        })

        get("/scms/parametersValues", { req, res ->

            def name = req.queryParams("name")
            assert name

            def element = scms.elements[name]

            if (!element)
                return JsonOutput.toJson([:])

            return JsonOutput.toJson(element.getParametersValues())
        })

        post("/scms/parameters", { req, res ->

            def name = req.queryParams("name")
            assert name

            def parameter = req.queryParams("parameter")
            assert parameter

            def element = scms.elements[name]
            assert element

            def payload = req.body()
            def parameterValue = payload

            if (payload)
                parameterValue = new JsonSlurper().parseText(payload).parameterValue

            def parametersFile = new File(parametersLocation, element.simpleName() + ".json")

            element.writeParameterValue(parametersFile, element, parameter, parameterValue.toString())

            return "ok"
        })
    }

    //Executions
    void execution() {

        get("/execution", { req, res ->
            return JsonOutput.toJson(exec.toMap())
        })

        get("/execution/logs/:ìndex", { req, res ->

            def index = req.params("ìndex")
            assert index

            return JsonOutput.toJson(exec.messages[Integer.parseInt(index)])
        })

        post("/execution/start", { req, res ->
            if (exec.isRunning())
                return "Already running"

            List<File> scmFiles = run.selected.values()
                    .findAll { it.link.isOwner() }
                    .collect { run.invOfScm[it.link.value] }
                    .unique()
                    .collect { scms.elements[it].script } as List<File>

            exec.start(scmFiles)

            return JsonOutput.toJson([
                    files: scmFiles.collect { it.name }
            ])
        })

        post("/execution/stop", { req, res ->
            if (!exec.isRunning())
                return "Already stopped"

            exec.stop()

            return "Stopped"
        })
    }

    void review() {
        get("/review", { req, res ->

            if (!exec.latestLog().exists())
                return "Oups"

            def review = new Review(new File(runLocation, "run.txt"), exec.latestLog())

            return JsonOutput.toJson(review.toMap())
        })
    }

}
