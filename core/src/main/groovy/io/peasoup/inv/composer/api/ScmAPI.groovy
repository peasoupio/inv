package io.peasoup.inv.composer.api

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import io.peasoup.inv.composer.ScmFile
import io.peasoup.inv.composer.WebServer
import io.peasoup.inv.scm.ScmDescriptor
import io.peasoup.inv.scm.ScmExecutor
import io.peasoup.inv.security.CommonLoader
import org.codehaus.groovy.control.MultipleCompilationErrorsException
import spark.Request
import spark.Response

import static spark.Spark.get
import static spark.Spark.post

class ScmAPI {

    private final WebServer webServer

    ScmAPI(WebServer webServer) {
        this.webServer = webServer
    }

    void routes() {
        // General
        get("/scms", { Request req, Response res ->
            Map output = webServer.scms.toMap(webServer.run)
            output.descriptors = webServer.pagination.resolve(output.descriptors)

            return JsonOutput.toJson(output)
        })

        post("/scms", { Request req, Response res ->

            Map filter = [:]
            String body = req.body()

            if (body)
                filter = new JsonSlurper().parseText(body) as Map

            Map output = webServer.scms.toMap(webServer.run, filter, webServer.parametersLocation)
            output.descriptors = webServer.pagination.resolve(
                    output.descriptors,
                    filter.from as Integer,
                    filter.to as Integer)

            return JsonOutput.toJson(output)
        })

        get("/scms/metadata", { Request req, Response res ->
            Map output = webServer.scms.toMap(webServer.run)
            output.descriptors = null

            return JsonOutput.toJson(output)
        })

        post("/scms/stageAll", { Request req, Response res ->
            webServer.settings.unstageAllSCMs()

            webServer.scms.elements.keySet().each {
                webServer.scms.stage(it)
                webServer.settings.stageSCM(it)
            }
            webServer.settings.save()

            return webServer.showResult("staged all")
        })

        post("/scms/unstageAll", { Request req, Response res ->
            webServer.settings.unstageAllSCMs()

            webServer.scms.elements.keySet().each {
                webServer.scms.unstage(it)
            }
            webServer.settings.save()

            return webServer.showResult("unstaged all")
        })

        post("/scms/applyDefaultAll", { Request req, Response res ->
            webServer.scms.elements.values().each { ScmFile.SourceFileElement element ->
                if (webServer.run != null && !webServer.run.isSelected(element.descriptor.name))
                    return

                def parametersFile = new File(webServer.parametersLocation, element.simpleName() + ".json")

                element.descriptor.ask.parameters.each { ScmDescriptor.AskParameter parameter ->
                    element.writeParameterDefaultValue(parametersFile, element.descriptor.name, parameter)
                }
            }

            return webServer.showResult("Ok")
        })

        post("/scms/resetAll", { Request req, Response res ->

            webServer.scms.elements.values().each { ScmFile.SourceFileElement element ->
                if (webServer.run != null && !webServer.run.isSelected(element.descriptor.name))
                    return

                def parametersFile = new File(webServer.parametersLocation, element.simpleName() + ".json")

                if (!parametersFile.exists())
                    return

                parametersFile.delete()
            }

            return webServer.showResult("Ok")
        })

        // Specfic
        get("/scms/view", { Request req, Response res ->

            def name = req.queryParams("name")
            if (!name)
                return webServer.showError(res, "name is required")

            // Make sure to get the latest information
            if (!webServer.scms.reload(name))
                return webServer.showError(res, "No SCM found for the specified name")

            def element = webServer.scms.elements[name]
            def output = element.toMap([:], new File(webServer.parametersLocation, element.simpleName() + ".json"))

            return JsonOutput.toJson(output)
        })

        post("/scms/stage", { Request req, Response res ->

            def name = req.queryParams("name")
            if (!name)
                return webServer.showError(res, "name is required")

            webServer.scms.stage(name)
            webServer.settings.stageSCM(name)
            webServer.settings.save()

            return webServer.showResult("staged")
        })

        post("/scms/unstage", { Request req, Response res ->

            def name = req.queryParams("name")
            if (!name)
                return webServer.showError(res, "name is required")

            webServer.scms.unstage(name)
            webServer.settings.unstageSCM(name)
            webServer.settings.save()

            return webServer.showResult("unstaged")
        })

        post("/scms/source", { Request req, Response res ->

            def name = req.queryParams("name")
            if (!name)
                return webServer.showError(res, "name is required")

            String source = req.body()
            Integer errorCount = 0
            List<String> exceptionMessages = []

            try {
                new CommonLoader().compile(source)
            } catch (MultipleCompilationErrorsException ex) {
                errorCount = ex.errorCollector.errorCount
                exceptionMessages = ex.errorCollector.errors.collect { it.cause.toString() }
            }

            if (errorCount == 0) {
                def element = webServer.scms.elements[name]

                try {
                    // If existing, replace
                    if (element) {
                        element.scmFile.scriptFile.delete()
                        element.scmFile.scriptFile << req.body()

                        webServer.scms.load(element.scmFile.scriptFile)
                    } else {
                        // Otherwise create new one
                        def newFile = new File(webServer.scms.scmFolder, name + ".groovy")
                        newFile << req.body()

                        webServer.scms.load(newFile)
                    }
                } catch (Exception ex) {
                    errorCount = 1
                    exceptionMessages = [ex.getMessage()]
                }
            }

            return JsonOutput.toJson([
                    errorCount: errorCount,
                    errors: exceptionMessages
            ])
        })

        post("/scms/remove", { Request req, Response res ->

            def name = req.queryParams("name")
            if (!name)
                return webServer.showError(res, "name is required")

            webServer.scms.remove(name)

            return webServer.showResult("Deleted")
        })

        get("/scms/parametersValues", { Request req, Response res ->

            def name = req.queryParams("name")
            if (!name)
                return webServer.showError(res, "name is required")

            if (!webServer.scms.reload(name))
                return JsonOutput.toJson([:])

            def latestElement = webServer.scms.elements[name]

            // Make sure we at least try to get repository with default or no parameters
            List<ScmExecutor.SCMExecutionReport> report = new ScmExecutor().with {
                add(latestElement.descriptor)
                return execute()
            }

            if (report[0].isOk)
                return JsonOutput.toJson(latestElement.getParametersValues())
            else
                return webServer.showError(res, "could not extract SCM")
        })

        post("/scms/parameters", { Request req, Response res ->

            def name = req.queryParams("name")
            if (!name)
                return webServer.showError(res, "name is required")

            def parameter = req.queryParams("parameter")
            if (!parameter)
                return webServer.showError(res, "parameter is required")

            def element = webServer.scms.elements[name]
            if (!element)
                return webServer.showError(res, "No parameter found for the specified name")

            def payload = req.body()
            def parameterValue = payload

            if (payload)
                parameterValue = new JsonSlurper().parseText(payload).parameterValue

            def parametersFile = new File(webServer.parametersLocation, element.simpleName() + ".json")

            element.writeParameterValue(
                    parametersFile,
                    element.descriptor.name,
                    parameter,
                    parameterValue.toString())

            return webServer.showResult("Ok")
        })
    }
}
