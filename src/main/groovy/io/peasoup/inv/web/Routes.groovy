package io.peasoup.inv.web

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import io.peasoup.inv.scm.ScmDescriptor

import static spark.Spark.*;

class Routes {

    final String WEB = "../web"
    final String RUN = System.getenv('INV_RUN') ?: "../runs"
    final String SCMS = System.getenv('INV_SCMS') ?: "../scms"
    final String PARAMETERS = System.getenv('INV_PARAMETERS') ?: "../parameters"

    final Run baseRun
    final Map<String, ScmSourceFile.SourceFileElement> scmCache = [:]

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
            return JsonOutput.toJson(baseRun.toMap())
        })

        post("/run/stage/:name", { req, res ->

            def name = req.params("name")
            assert name

            baseRun.stage(name)

            return JsonOutput.toJson(baseRun.toMap())
        })

        post("/run/unstage/:name", { req, res ->

            def name = req.params("name")
            assert name

            baseRun.unstage(name)

            return JsonOutput.toJson(baseRun.toMap())
        })
    }

    // Scms
    void scms() {
        get("/scms", { req, res ->

            Map output = [
                scripts:[:],
                registry: [:]
            ]

            new File(SCMS).eachFileRecurse {
                def file = it
                def scmReader = new ScmDescriptor(file.newReader())

                output.scripts[file.name] = [
                    text: file.text,
                    lastEdit: file.lastModified()
                ]

                scmReader.scms().each { String name, ScmDescriptor.MainDescriptor desc ->
                    scmCache[name] = new ScmSourceFile.SourceFileElement(
                        descriptor: desc,
                        script: file.name
                    )

                    output.registry[name] = ScmSourceFile.toMap(file.name, desc)
                }
            }

            return JsonOutput.toJson(output)
        })

        post("/scms/:name/source", { req, res ->

            def name = req.params("name")
            assert name

            def element = scmCache[name]

            def sourceFile = new File(SCMS, element.script)
            sourceFile.delete()
            sourceFile << req.body()

            scmCache.removeAll { String otherName, ScmSourceFile.SourceFileElement otherElement ->
                otherElement.script == element.script
            }

            def scmReader = new ScmDescriptor(sourceFile.newReader())

            def output = [
                registry:[:],
                scripts: [(sourceFile.name): [
                        lastEdit: sourceFile.lastModified()]
                ]
            ]

            scmReader.scms().each { String otherName, ScmDescriptor.MainDescriptor desc ->
                scmCache[otherName] = new ScmSourceFile.SourceFileElement(
                    descriptor: desc,
                    script: element.script
                )

                output.registry[otherName] = ScmSourceFile.toMap(element.script, desc)
            }

            return JsonOutput.toJson(output)
        })
    }

    void scmsParameters() {

        get("/scms/:name/parameters", { req, res ->

            def name = req.params("name")
            assert name

            def element = scmCache[name]

            if (!element)
                return JsonOutput.toJson([:])

            return JsonOutput.toJson(ScmSourceFile.getParameters(
                    element.descriptor,
                    new File(PARAMETERS, element.simpleName() + ".properties")))
        })

        post("/scms/:name/parameters/:parameterName", { req, res ->

            def name = req.params("name")
            assert name

            def element = scmCache[name]
            assert element

            def parameterName = req.params("parameterName")
            assert parameterName

            def payload = req.body()
            def parameterValue = payload

            if (payload)
                parameterValue = new JsonSlurper().parseText(payload).parameterValue

            def propertyFile = new File(PARAMETERS, element.simpleName() + ".properties")

            if (!propertyFile.exists()) {
                propertyFile << "${name}.${parameterName}=${parameterValue}".toString()
                return "ok"
            }

            def propertyFileObject = new Properties()
            propertyFileObject.load(propertyFile.newReader())

            propertyFileObject["${name}.${parameterName}".toString()] = parameterValue

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
