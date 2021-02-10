package io.peasoup.inv.composer.api

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import io.peasoup.inv.composer.RepoFile
import io.peasoup.inv.composer.WebServer
import io.peasoup.inv.io.FileUtils
import io.peasoup.inv.loader.GroovyLoader
import io.peasoup.inv.loader.YamlLoader
import io.peasoup.inv.repo.RepoDescriptor
import io.peasoup.inv.repo.RepoExecutor
import org.apache.commons.lang.StringUtils
import org.codehaus.groovy.control.MultipleCompilationErrorsException
import spark.Request
import spark.Response

import static spark.Spark.get
import static spark.Spark.post

class RepoAPI {

    private final WebServer webServer

    RepoAPI(WebServer webServer) {
        this.webServer = webServer
    }

    void routes() {
        // General
        get("/repos", { Request req, Response res ->
            Map output = webServer.repos.toMap(webServer.security.isRequestSecure(req), webServer.run)
            output.descriptors = webServer.pagination.resolve(output.descriptors) as List<Map>

            return JsonOutput.toJson(output)
        })

        post("/repos", { Request req, Response res ->

            Map filter = [:]
            String body = req.body()

            if (body)
                filter = new JsonSlurper().parseText(body) as Map

            Map output = webServer.repos.toMap(webServer.security.isRequestSecure(req), webServer.run, filter)
            output.descriptors = webServer.pagination.resolve(
                    output.descriptors,
                    filter.from as Integer,
                    filter.to as Integer) as List<Map>

            return JsonOutput.toJson(output)
        })

        get("/repos/metadata", { Request req, Response res ->
            Map output = webServer.repos.toMap(webServer.security.isRequestSecure(req), webServer.run)
            output.descriptors = null

            return JsonOutput.toJson(output)
        })

        post("/repos/stageAll", { Request req, Response res ->
            if (!webServer.security.isRequestSecure(req))
                return WebServer.notAvailable(res)

            webServer.settings.unstageAllREPOs()

            webServer.repos.elements.keySet().each {
                webServer.repos.stage(it)
                webServer.settings.stageREPO(it)
            }
            webServer.settings.save()

            return WebServer.showResult("staged all")
        })

        post("/repos/unstageAll", { Request req, Response res ->
            if (!webServer.security.isRequestSecure(req))
                return WebServer.notAvailable(res)

            webServer.settings.unstageAllREPOs()

            webServer.repos.elements.keySet().each {
                webServer.repos.unstage(it)
            }
            webServer.settings.save()

            return WebServer.showResult("unstaged all")
        })

        post("/repos/applyDefaultAll", { Request req, Response res ->
            if (!webServer.security.isRequestSecure(req))
                return WebServer.notAvailable(res)

            webServer.repos.elements.values().each { RepoFile.SourceFileElement element ->
                if (webServer.run != null && !webServer.run.isRepoRequired(element.descriptor.name))
                    return

                element.descriptor.ask.parameters.each { RepoDescriptor.AskParameter parameter ->
                    element.writeParameterDefaultValue(element.descriptor.name, parameter)
                }
            }

            return WebServer.showResult("Ok")
        })

        post("/repos/resetAll", { Request req, Response res ->
            if (!webServer.security.isRequestSecure(req))
                return WebServer.notAvailable(res)

            webServer.repos.elements.values().each { RepoFile.SourceFileElement element ->
                if (webServer.run != null && !webServer.run.isRepoRequired(element.descriptor.name))
                    return

                if (!element.repoFile.expectedParameterFile.exists())
                    return

                element.repoFile.expectedParameterFile.delete()
            }

            return WebServer.showResult("Ok")
        })

        // Specfic
        get("/repos/view", { Request req, Response res ->

            def name = req.queryParams("name")
            if (!name)
                return WebServer.showError(res, "name is required")

            // Make sure to get the latest information
            if (!webServer.repos.reload(name))
                return WebServer.showError(res, "No REPO found for the specified name")

            def element = webServer.repos.elements[name]
            def output = element.toMap(webServer.security.isRequestSecure(req), [:])

            return JsonOutput.toJson(output)
        })

        post("/repos/stage", { Request req, Response res ->
            if (!webServer.security.isRequestSecure(req))
                return WebServer.notAvailable(res)

            def name = req.queryParams("name")
            if (!name)
                return WebServer.showError(res, "name is required")

            webServer.repos.stage(name)
            webServer.settings.stageREPO(name)
            webServer.settings.save()

            return WebServer.showResult("staged")
        })

        post("/repos/unstage", { Request req, Response res ->
            if (!webServer.security.isRequestSecure(req))
                return WebServer.notAvailable(res)

            def name = req.queryParams("name")
            if (!name)
                return WebServer.showError(res, "name is required")

            webServer.repos.unstage(name)
            webServer.settings.unstageREPO(name)
            webServer.settings.save()

            return WebServer.showResult("unstaged")
        })

        post("/repos/add", { Request req, Response res ->
            if (!webServer.security.isRequestSecure(req))
                return WebServer.notAvailable(res)

            def name = req.queryParams("name")
            if (!name)
                return WebServer.showError(res, "name is required")

            def mimeType = req.queryParams("mimeType")
            if (!mimeType)
                return WebServer.showError(res, "mimeType is required")

            String source = req.body()
            Integer errorCount = 0
            List<String> exceptionMessages = []

            String fileExtension = ".groovy"

            try {

                switch (mimeType) {
                    case FileUtils.SCRIPT_YAML_TYPE:
                        new YamlLoader().parseYamlText(source)
                        fileExtension = ".yml"
                        break
                    case FileUtils.SCRIPT_GROOVY_TYPE:
                        GroovyLoader.newBuilder().build().parseClassText(source)
                        break
                }

            } catch (MultipleCompilationErrorsException ex) {
                errorCount = ex.errorCollector.errorCount
                exceptionMessages = ex.errorCollector.errors.collect { it.cause.toString() }
            }

            if (errorCount == 0) {
                def element = webServer.repos.elements[name]

                try {
                    // If existing, replace
                    if (element) {
                        element.repoFile.scriptFile.delete()
                        element.repoFile.scriptFile << source
                        webServer.repos.load(element.repoFile.scriptFile)
                    } else {
                        // Otherwise create new one
                        def newFile = new File(webServer.repos.repoFolder, name + fileExtension)
                        newFile << source
                        webServer.repos.load(newFile)
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

        post("/repos/remove", { Request req, Response res ->
            if (!webServer.security.isRequestSecure(req))
                return WebServer.notAvailable(res)

            def name = req.queryParams("name")
            if (!name)
                return WebServer.showError(res, "name is required")

            webServer.repos.remove(name)

            return WebServer.showResult("Deleted")
        })

        get("/repos/parametersValues", { Request req, Response res ->

            def name = req.queryParams("name")
            if (!name)
                return WebServer.showError(res, "name is required")

            if (!webServer.repos.reload(name))
                return JsonOutput.toJson([:])

            def latestElement = webServer.repos.elements[name]

            if (StringUtils.isNotEmpty(latestElement.descriptor.hooks.init)) {

                // Make sure we at least try to get repository with default or no parameters
                List<RepoExecutor.RepoHookExecutionReport> report = new RepoExecutor().with {
                    add(latestElement.descriptor)
                    return execute()
                }

                if (!report[0].isOk())
                    return WebServer.showError(res, "could not extract REPO")
            }

            return JsonOutput.toJson(latestElement.getParametersValues())
        })

        post("/repos/parameters", { Request req, Response res ->
            if (!webServer.security.isRequestSecure(req))
                return WebServer.notAvailable(res)

            def name = req.queryParams("name")
            if (!name)
                return WebServer.showError(res, "name is required")

            def parameter = req.queryParams("parameter")
            if (!parameter)
                return WebServer.showError(res, "parameter is required")

            def element = webServer.repos.elements[name]
            if (!element)
                return WebServer.showError(res, "No parameter found for the specified name")

            def payload = req.body()
            def parameterValue = payload

            if (payload)
                parameterValue = new JsonSlurper().parseText(payload).parameterValue

            element.writeParameterValue(
                    element.descriptor.name,
                    parameter,
                    parameterValue.toString())

            return WebServer.showResult("Updated")
        })
    }
}
