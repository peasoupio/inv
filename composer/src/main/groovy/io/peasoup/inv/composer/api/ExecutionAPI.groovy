package io.peasoup.inv.composer.api

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
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
            return JsonOutput.toJson(webServer.exec.toMap())
        })

        get("/execution/latest/download", { Request req, Response res ->
            File latestLog = webServer.exec.latestLog()

            if (!latestLog.exists())
                return webServer.showError(res, "Latest log file is not present on filesystem")

            if (latestLog.size() == 0)
                return webServer.showError(res, "Latest log file is empty")

            res.raw().setContentType("application/octet-stream")
            res.raw().setHeader("Content-Disposition", "attachment; filename=${latestLog.parentFile.name}-log.txt")

            res.raw().getOutputStream().write(latestLog.bytes)

            return res.raw()
        })

        post("/execution/start", { Request req, Response res ->
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
            }

            // Add selected repos
            if (webServer.run)
                toExecute += webServer.run.selectedRepos()

            List<RepoFile> repoFiles = webServer.repos.toFiles(toExecute.unique()) as List<RepoFile>

            webServer.exec.start(
                    debugMode,
                    systemMode,
                    secureMode,
                    repoFiles)
            Thread.sleep(50)

            return JsonOutput.toJson([
                    files: repoFiles.collect { it.simpleName() }
            ])
        })

        post("/execution/stop", { Request req, Response res ->
            if (!webServer.exec.isRunning())
                return webServer.showError(res, "Already stopped")

            webServer.exec.stop()
            Thread.sleep(50)

            return webServer.showResult("Stopped")
        })
    }
}
