package io.peasoup.inv.web

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import io.peasoup.inv.scm.ScmDescriptor

import static spark.Spark.*;

class Routes {

    final String WEB = "../web"
    final String RUNS = System.getenv('INV_RUNS') ?: "../runs"
    final String SCMS = System.getenv('INV_SCMS') ?: "../scms"
    final String PARAMETERS = System.getenv('INV_PARAMETERS') ?: "../parameters"

    final Map<String, ScmSourceFile.SourceFileElement> scmCache = [:]

    final Execution exec = new Execution(new File(SCMS), new File(PARAMETERS))

    Routes() {

        // Browser cnofigs
        port(8080);

        // Static files
        staticFiles.externalLocation(WEB)
        staticFiles.externalLocation(RUNS)

        // Exception handling
        exception(Exception.class, { e, request, response ->
            final StringWriter sw = new StringWriter();
            final PrintWriter pw = new PrintWriter(sw, true);
            e.printStackTrace(pw);
            System.err.println(sw.getBuffer().toString());
        })

        // Init
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
        get("/runs", { req, res ->

            String test

            new File(RUNS).eachFileRecurse {
                test = new Run(it).toJson()
            }

            return test
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
                scmCache[name] = new ScmSourceFile.SourceFileElement(
                    descriptor: desc,
                    script: element.script
                )

                output.registry[name] = ScmSourceFile.toMap(element.script, desc)
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

            def payload = new JsonSlurper().parseText(req.body())
            assert payload

            def parameterValue = payload.parameterValue
            assert parameterValue

            def propertyFile = new File(PARAMETERS, element.simpleName() + ".properties")

            if (!propertyFile.exists()) {
                propertyFile << "${name}.${parameterName}=${parameterValue}".toString()
                return "ok"
            }

            def propertyFileObject = new Properties()
            propertyFileObject.load(propertyFile.newReader())

            propertyFileObject[parameterName] = parameterValue

            propertyFileObject.store(propertyFile.newWriter(), "My comments")

            return "ok"
        })
    }

    //Executions
    void execution() {

        get("/execution", { req, res ->
            return JsonOutput.toJson(exec.toMap())
        })

        post("/execution/start", { req, res ->
            if (exec.isRunning())
                return "Already running"

            exec.start()

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
