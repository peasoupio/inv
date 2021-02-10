package io.peasoup.inv.composer.api

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import io.peasoup.inv.Logger
import io.peasoup.inv.composer.MainHelper
import io.peasoup.inv.composer.WebServer
import io.peasoup.inv.composer.utils.MapUtils
import io.peasoup.inv.io.FileUtils
import io.peasoup.inv.loader.GroovyLoader
import io.peasoup.inv.repo.HookExecutor
import io.peasoup.inv.repo.RepoExecutor
import org.codehaus.groovy.control.MultipleCompilationErrorsException
import spark.Request
import spark.Response

import static spark.Spark.*

class SystemAPI {

    private final WebServer webServer
    private final Map initInfo

    SystemAPI(WebServer webServer) {
        this.webServer = webServer
        this.initInfo = initInfo()
    }

    void routes() {
        post("/stop", { Request req, Response res ->
            if (!webServer.security.isRequestSecure(req))
                return WebServer.notAvailable(res)

            stop()
            awaitStop()
        })

        get("/v1", { Request req, Response res ->

            def apis = [
                    links: [
                            setup    : webServer.API_CONTEXT_ROOT + "/setup",
                            initFile : [
                                    default: webServer.API_CONTEXT_ROOT + "/initfile"
                            ],
                            run      : [
                                    default: webServer.API_CONTEXT_ROOT + "/run",
                                    search : webServer.API_CONTEXT_ROOT + "/run",
                                    owners : webServer.API_CONTEXT_ROOT + "/run/owners",
                                    names  : webServer.API_CONTEXT_ROOT + "/run/names",
                                    staged : webServer.API_CONTEXT_ROOT + "/run/staged",
                                    tree   : webServer.API_CONTEXT_ROOT + "/run/tree",
                                    tags   : webServer.API_CONTEXT_ROOT + "/run/tags",
                            ],
                            runFile  : [
                                    default: webServer.API_CONTEXT_ROOT + "/runfile"
                            ],
                            repos    : [
                                    default : webServer.API_CONTEXT_ROOT + "/repos",
                                    search  : webServer.API_CONTEXT_ROOT + "/repos",
                                    metadata: webServer.API_CONTEXT_ROOT + "/repos/metadata",
                            ],
                            execution: [
                                    default: webServer.API_CONTEXT_ROOT + "/execution"
                            ],
                            review   : [
                                    default: webServer.API_CONTEXT_ROOT + "/review",
                            ]
                    ]
            ]

            if (webServer.security.isRequestSecure(req)) {
                MapUtils.merge(apis, [
                        links: [
                                stop    : webServer.API_CONTEXT_ROOT + "/stop",
                                settings: [
                                        default: webServer.API_CONTEXT_ROOT + "/settings",
                                        save   : webServer.API_CONTEXT_ROOT + "/settings"
                                ],
                                run     : [
                                        stageAll  : webServer.API_CONTEXT_ROOT + "/run/stageAll",
                                        unstageAll: webServer.API_CONTEXT_ROOT + "/run/unstageAll",
                                ],
                                repos   : [
                                        add            : webServer.API_CONTEXT_ROOT + "/repos/add",
                                        stageAll       : webServer.API_CONTEXT_ROOT + "/repos/stageAll",
                                        unstageAll     : webServer.API_CONTEXT_ROOT + "/repos/unstageAll",
                                        applyDefaultAll: webServer.API_CONTEXT_ROOT + "/repos/applyDefaultAll",
                                        resetAll       : webServer.API_CONTEXT_ROOT + "/repos/resetAll"
                                ],
                                initFile: [
                                        save: webServer.API_CONTEXT_ROOT + "/initfile",
                                        pull: webServer.API_CONTEXT_ROOT + "/initfile/pull",
                                        push: webServer.API_CONTEXT_ROOT + "/initfile/push"
                                ],
                                runFile : [
                                        save: webServer.API_CONTEXT_ROOT + "/runfile"
                                ],
                                review  : [
                                        promote: webServer.API_CONTEXT_ROOT + "/review/promote"
                                ]
                        ]
                ])
            }

            return JsonOutput.toJson(apis)
        })

        get("/setup", { Request req, Response res ->

            return JsonOutput.toJson([
                    booted        : webServer.boot.isDone(),
                    releaseVersion: webServer.version(),
                    initInfo      : initInfo,
                    firstTime     : webServer.run == null,
                    secured       : webServer.security.isRequestSecure(req),
                    links         : [
                            stream: "/boot/stream"
                    ]
            ])
        })

        get("/settings", { Request req, Response res ->
            if (!webServer.security.isRequestSecure(req))
                return WebServer.notAvailable(res)

            return webServer.settings.toJson()
        })

        post("/settings", { Request req, Response res ->
            if (!webServer.security.isRequestSecure(req))
                return WebServer.notAvailable(res)

            String body = req.body()
            Map values = new JsonSlurper().parseText(body) as Map

            webServer.settings.apply(values)
            webServer.settings.save()

            return WebServer.showResult("Values parsed")
        })

        get("/initfile", { Request req, Response res ->
            def initFile = webServer.initFile()
            if (!initFile)
                return WebServer.showError(res, "Missing init file")

            return JsonOutput.toJson([
                    text    : initFile.text,
                    mimeType: FileUtils.getMimeType(initFile.name)
            ])
        })

        post("/initfile", { Request req, Response res ->
            if (!webServer.security.isRequestSecure(req))
                return WebServer.notAvailable(res)

            String fileContent = req.body()

            if (fileContent.isEmpty())
                return WebServer.showError(res, "Content is empty")

            Integer errorCount = 0
            List<String> exceptionMessages = []

            try {
                new GroovyLoader().parseClassText(fileContent)
            } catch (MultipleCompilationErrorsException ex) {
                errorCount = ex.errorCollector.errorCount
                exceptionMessages = ex.errorCollector.errors.collect { it.cause.toString() }
            }

            if (errorCount == 0) {
                File initFile = webServer.initFile()

                // Create an init file if it does not exists
                if (initFile == null)
                    initFile = new File(webServer.webServerConfigs.workspace as String, "init.groovy")

                initFile.delete()
                initFile << fileContent
            }

            return JsonOutput.toJson([
                    errorCount: errorCount,
                    errors    : exceptionMessages
            ])
        })

        post("/initfile/pull", { Request req, Response res ->
            if (!webServer.security.isRequestSecure(req))
                return WebServer.notAvailable(res)

            def initFile = webServer.initFile()
            if (!initFile)
                return WebServer.showError(res, "Missing init file")

            // Reuse InitRunCommand to pull init file
            def exitValue = MainHelper.execute(
                    webServer.appLauncher(),
                    ["repo-run", initFile.absolutePath])

            if (exitValue != 0)
                return WebServer.showError(res, "Could not pull init")

            return WebServer.showResult("Pulled init successfully")
        })

        post("/initfile/push", { Request req, Response res ->
            if (!webServer.security.isRequestSecure(req))
                return WebServer.notAvailable(res)

            def initFile = webServer.initFile()
            if (!initFile)
                return WebServer.showError(res, "Missing init file")

            // Invoke push hook manually
            RepoExecutor.RepoHookExecutionReport report = new RepoExecutor().with {
                addScript(initFile)

                if (!repos.containsKey("main"))
                    return null

                def report = new RepoExecutor.RepoHookExecutionReport("main", repos.get("main"))
                HookExecutor.push(report)

                return report
            }

            if (!report || !report.isOk())
                return WebServer.showError(res, "Could not push init")

            return WebServer.showResult("Pushed init successfully")
        })
    }


    private Map initInfo() {
        File initFile = webServer.initFile()

        if (!initFile)
            return [standalone: true]

        RepoExecutor.RepoHookExecutionReport report = new RepoExecutor().with {
            addScript(initFile)

            if (!repos.containsKey("main"))
                return null

            def report = new RepoExecutor.RepoHookExecutionReport("main", repos.get("main"))
            HookExecutor.version(report)

            return report
        }

        if (!report) {
            Logger.warn("Could not read init file to get info")
            return [standalone: true]
        }

        return [
                source : report.descriptor.src,
                version: report.stdout ? report.stdout.trim() : "undefined"
        ]
    }
}
