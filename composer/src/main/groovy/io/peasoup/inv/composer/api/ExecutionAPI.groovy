package io.peasoup.inv.composer.api

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import io.peasoup.inv.Logger
import io.peasoup.inv.composer.RepoFile
import io.peasoup.inv.composer.WebServer
import spark.Request
import spark.Response

import static spark.Spark.get
import static spark.Spark.post

class ExecutionAPI {

    private final WebServer webServer

    ExecutionAPI(WebServer webServer) {
        this.webServer = webServer
    }

    void routes() {
        get("/execution", { Request req, Response res ->

            Map executionMap = webServer.exec.toMap()

            if (!webServer.security.isRequestSecure(req)) {
                executionMap.links.remove("stop")
                executionMap.links.remove("start")
            }

            return JsonOutput.toJson(executionMap)
        })

        get("/execution/latest/download", { Request req, Response res ->
            File latestLog = webServer.exec.latestLog()

            if (!latestLog.exists())
                return WebServer.showError(res, "Latest log file is not present on filesystem")

            if (latestLog.size() == 0)
                return WebServer.showError(res, "Latest log file is empty")

            res.raw().setContentType("application/octet-stream")
            res.raw().setHeader("Content-Disposition", "attachment; filename=${latestLog.parentFile.name}-log.txt")

            res.raw().getOutputStream().write(latestLog.bytes)

            return res.raw()
        })

        post("/execution/start", { Request req, Response res ->
            if (!webServer.security.isRequestSecure(req))
                return WebServer.notAvailable(res)

            if (webServer.exec.isRunning())
                return "Already webServer.running"

            List<String> toExecute = []
            toExecute += webServer.repos.staged

            boolean debugMode = false
            boolean systemMode = false
            boolean secureMode = false

            def body = req.body()
            if (body) {
                def options = new JsonSlurper().parseText(body) as Map
                debugMode = options.debugMode
                systemMode = options.systemMode
                secureMode = options.secureMode
            }

            // Add selected repos
            if (webServer.run)
                toExecute += webServer.run.requiredRepos()

            List<RepoFile> repoFiles = webServer.repos.toFiles(toExecute.unique()) as List<RepoFile>

            // Do the actual execution
            def error = webServer.exec.start(
                    debugMode,
                    systemMode,
                    secureMode,
                    repoFiles)
            Thread.sleep(50)

            // If an error occurred, send detail to user.
            if (error) {
                Logger.warn(error.message)
                return WebServer.showError(res, error.message)
            }

            return JsonOutput.toJson([
                    files: repoFiles.collect { it.simpleName() }
            ])
        })

        post("/execution/stop", { Request req, Response res ->
            if (!webServer.security.isRequestSecure(req))
                return WebServer.notAvailable(res)

            if (!webServer.exec.isRunning())
                return WebServer.showError(res, "Already stopped")

            webServer.exec.stop()
            Thread.sleep(50)

            return WebServer.showResult("Stopped")
        })
    }
}
