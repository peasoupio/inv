package io.peasoup.inv.web

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import me.tongfei.progressbar.ProgressBar

import static spark.Spark.*

class Routes {

    final String WEB = "../web"
    final String RUN = System.getenv('INV_RUN') ?: "../runs"
    final String SCMS = System.getenv('INV_SCMS') ?: "../scms"
    final String PARAMETERS = System.getenv('INV_PARAMETERS') ?: "../parameters"

    final Run baseRun
    final List<ScmSourceFile> scms = []

    final Execution exec = new Execution(new File(SCMS), new File(PARAMETERS))

    Routes() {

        // Browser cnofigs
        port(8080);

        // Static files
        staticFiles.externalLocation(WEB)

        // Exception handling
        exception(Exception.class, { e, request, response ->
            final StringWriter sw = new StringWriter();
            final PrintWriter pw = new PrintWriter(sw, true);
            e.printStackTrace(pw);
            System.err.println(sw.getBuffer().toString());
        })

        // Init
        baseRun = new Run(new File(RUN, "run.txt"))

        def files = new File(SCMS).listFiles()

        ProgressBar pb = new ProgressBar("Reading scm files", files.size())

        files.each {
            pb.step()
            scms << new ScmSourceFile(it)
        }

        pb.close()
    }

    /**
     * Map available routes
     * @return
     */
    int map() {

        system()

        runs()

        scms()
        scmsParameters()

        execution()

        return 0
    }

    // System-wise
    void system() {
        get("/stop", { req, res -> stop() })
    }

    // Runs
    void runs() {
        get("/run", { req, res ->
            return JsonOutput.toJson(baseRun.getNodes())
        })

        post("/run", { req, res ->

            String body = req.body()
            Map filter = [:]

            if (body)
                filter = new JsonSlurper().parseText(body)

            return JsonOutput.toJson(baseRun.getNodes('all', filter, filter.from ?: 0, filter.step ?: 20))
        })

        get("/run/requiredBy", { req, res ->

            def id = req.queryParams("id")
            assert id

            return JsonOutput.toJson(baseRun.getRequiredBy(id))
        })

        post("/run/selected", { req, res ->

            String body = req.body()
            Map filter = [:]

            if (body)
                filter = new JsonSlurper().parseText(body)

            return JsonOutput.toJson(baseRun.getNodes('selected', filter,  filter.from ?: 0, filter.step ?: 20))
        })

        post("/run/stage", { req, res ->

            def id = req.queryParams("id")
            assert id

            baseRun.stage(id)

            return "Ok"
        })

        post("/run/unstage", { req, res ->

            def id = req.queryParams("id")
            assert id

            baseRun.unstage(id)

            return "Ok"
        })
    }

    // Scms
    void scms() {
        get("/scms", { req, res ->

            Map output = [
                scripts : [:],
                registry: [],
                total: scms.sum { it.scmElements.size() },
                links: [
                    search: "/scms",
                    selected: "/scms/selected"
                ]
            ]

            scms.each {
                output.scripts[it.sourceFile.name] = [
                    text    : it.text,
                    lastEdit: it.lastEdit
                ]

                it.elements().each {
                    output.registry << it.toMap()
                }
            }

            if (output.registry.size() > 20)
                output.registry = output.registry[0..19]

            return JsonOutput.toJson(output)
        })

        post("/scms", { req, res ->

            Map output = [
                scripts : [:],
                registry: [],
                total: scms.sum { it.scmElements.size() },
                links: [
                    search: "/scms",
                    selected: "/scms/selected"
                ]
            ]

            String body = req.body()
            Map filter = [:]

            if (body)
                filter = new JsonSlurper().parseText(body)

            Integer from = filter.from ?: 0
            Integer to = filter.to ?: 20

            scms.each {
                if (output.registry.size() > from + to)
                    return

                output.scripts[it.sourceFile.name] = [
                    text    : it.text,
                    lastEdit: it.lastEdit
                ]

                it.elements().each {
                    def element = it.toMap(filter)

                    if (!element)
                        return

                    output.registry << element
                }
            }

            if (output.registry.size() > from + to)
                output.registry = output.registry[from..to - 1]

            return JsonOutput.toJson(output)
        })

        get("/scm/view", { req, res ->

            def name = req.queryParams("name")
            assert name

            def output = ScmSourceFile.scmCache[name].toMap()

            return JsonOutput.toJson(output)
        })

        get("/scms/selected", { req, res ->

            Map output = [
                scms: []
            ]

            scms.each {
                it.elements().each {
                    if (!baseRun.isSelected(it.descriptor.name))
                        return

                    output.scms << it.toMap()
                }
            }

            return JsonOutput.toJson(output)
        })

        post("/scms/source", { req, res ->

            def name = req.queryParams("name")
            assert name

            def currentElement = ScmSourceFile.scmCache[name]

            def sourceFile = new File(SCMS, currentElement.script)
            sourceFile.delete()
            sourceFile << req.body()

            scms.removeAll {
                it.sourceFile == sourceFile
            }

            scms << new ScmSourceFile(sourceFile, baseRun)

            return "Ok"
        })
    }

    void scmsParameters() {

        get("/scms/parameters", { req, res ->

            def name = req.queryParams("name")
            assert name

            def element = ScmSourceFile.scmCache[name]

            if (!element)
                return JsonOutput.toJson([:])

            return JsonOutput.toJson(element.getParameters(new File(PARAMETERS, element.simpleName() + ".properties")))
        })

        post("/scms/parameters", { req, res ->

            def name = req.queryParams("name")
            assert name

            def parameter = req.queryParams("parameter")
            assert parameter

            def element = ScmSourceFile.scmCache[name]
            assert element

            def payload = req.body()
            def parameterValue = payload

            if (payload)
                parameterValue = new JsonSlurper().parseText(payload).parameterValue

            def propertyFile = new File(PARAMETERS, element.simpleName() + ".properties")

            if (!propertyFile.exists()) {
                propertyFile << "${name}.${parameter}=${parameterValue}".toString()
                return "ok"
            }

            def propertyFileObject = new Properties()
            propertyFileObject.load(propertyFile.newReader())

            propertyFileObject["${name}.${parameter}".toString()] = parameterValue

            propertyFileObject.store(propertyFile.newWriter(), "My comments")

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

            def scmFiles = (baseRun.staging + baseRun.propagatedStaging)
                    .collect { baseRun.invs[it].scm }
                    .collect { new File(SCMS, scmCache[it].script) }

            exec.start(scmFiles)

            return "Started"
        })

        post("/execution/stop", { req, res ->
            if (!exec.isRunning())
                return "Already stopped"

            exec.stop()

            return "Stopped"
        })
    }

}
