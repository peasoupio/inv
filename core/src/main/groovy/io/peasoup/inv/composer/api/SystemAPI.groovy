package io.peasoup.inv.composer.api

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import io.peasoup.inv.Logger
import io.peasoup.inv.SystemInfo
import io.peasoup.inv.cli.InitRunCommand
import io.peasoup.inv.composer.WebServer
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
        get("/stop", { Request req, Response res -> stop() })

        get("/v1", { Request req, Response res ->
            return JsonOutput.toJson([
                    links: [
                            setup    : webServer.API_CONTEXT_ROOT + "/setup",
                            settings : [
                                    default: webServer.API_CONTEXT_ROOT + "/settings",
                                    save: webServer.API_CONTEXT_ROOT + "/settings"
                            ],
                            initFile : [
                                    default: webServer.API_CONTEXT_ROOT + "/initfile",
                                    save   : webServer.API_CONTEXT_ROOT + "/initfile",
                                    pull   : webServer.API_CONTEXT_ROOT + "/initfile/pull",
                                    push   : webServer.API_CONTEXT_ROOT + "/initfile/push"
                            ],
                            run      : [
                                    default   : webServer.API_CONTEXT_ROOT + "/run",
                                    search    : webServer.API_CONTEXT_ROOT + "/run",
                                    owners    : webServer.API_CONTEXT_ROOT + "/run/owners",
                                    names     : webServer.API_CONTEXT_ROOT + "/run/names",
                                    selected  : webServer.API_CONTEXT_ROOT + "/run/selected",
                                    stageAll  : webServer.API_CONTEXT_ROOT + "/run/stageAll",
                                    unstageAll: webServer.API_CONTEXT_ROOT + "/run/unstageAll",
                                    tree      : webServer.API_CONTEXT_ROOT + "/run/tree",
                                    tags      : webServer.API_CONTEXT_ROOT + "/run/tags",
                                    runFile   : webServer.API_CONTEXT_ROOT + "/run/file"
                            ],
                            repos     : [
                                    default        : webServer.API_CONTEXT_ROOT + "/repos",
                                    search         : webServer.API_CONTEXT_ROOT + "/repos",
                                    add            : webServer.API_CONTEXT_ROOT + "/repos/source",
                                    stageAll       : webServer.API_CONTEXT_ROOT + "/repos/stageAll",
                                    unstageAll     : webServer.API_CONTEXT_ROOT + "/repos/unstageAll",
                                    applyDefaultAll: webServer.API_CONTEXT_ROOT + "/repos/applyDefaultAll",
                                    resetAll       : webServer.API_CONTEXT_ROOT + "/repos/resetAll",
                                    metadata       : webServer.API_CONTEXT_ROOT + "/repos/metadata",
                            ],
                            execution: [
                                    default: webServer.API_CONTEXT_ROOT + "/execution"
                            ],
                            review   : [
                                    default: webServer.API_CONTEXT_ROOT + "/review",
                                    promote: webServer.API_CONTEXT_ROOT + "/review/promote"
                            ]
                    ]
            ])
        })

        get("/setup", { Request req, Response res ->

            return JsonOutput.toJson([
                    booted        : webServer.boot.isDone(),
                    releaseVersion: SystemInfo.version(),
                    initInfo      : initInfo,
                    firstTime     : webServer.run == null,
                    links         : [
                            stream: "/boot/stream"
                    ]
            ])
        })

        get("/settings", { Request req, Response res ->
            return webServer.settings
        })

        post("/settings", { Request req, Response res ->
            String body = req.body()
            Map values = new JsonSlurper().parseText(body) as Map

            webServer.settings.apply(values)

            return webServer.showResult("Values parsed")
        })

        get("/initfile", { Request req, Response res ->
            if (!webServer.webServerConfigs.initFile)
                return webServer.showError(res, "Missing init file")

            if (!(webServer.webServerConfigs.initFile instanceof String))
                return webServer.showError(res, "InitFile is corrupted. Contact your administrator.")

            return new File(webServer.webServerConfigs.initFile as String).text
        })

        post("/initfile", { Request req, Response res ->
            String fileContent = req.body()

            if (fileContent.isEmpty())
                return webServer.showError(res, "Content is empty")

            Integer errorCount = 0
            List<String> exceptionMessages = []

            try {
                new GroovyLoader().parseClassText(fileContent)
            } catch (MultipleCompilationErrorsException ex) {
                errorCount = ex.errorCollector.errorCount
                exceptionMessages = ex.errorCollector.errors.collect { it.cause.toString() }
            }

            if (errorCount == 0) {
                def initFilePath = webServer.webServerConfigs.initFile
                File initFile

                if (!(initFilePath instanceof String))
                    initFile = new File(webServer.webServerConfigs.workspace as String, "init.groovy")
                else
                    initFile = new File(initFilePath as String)

                initFile.delete()
                initFile << fileContent
            }

            return JsonOutput.toJson([
                    errorCount: errorCount,
                    errors: exceptionMessages
            ])
        })

        post("/initfile/pull", { Request req, Response res ->
            if (!webServer.webServerConfigs.initFile)
                return webServer.showError(res, "Missing init file")

            if (!(webServer.webServerConfigs.initFile instanceof String))
                return webServer.showError(res, "InitFile is corrupted. Contact your administrator.")

            // Reuse InitRunCommand to pull init file
            InitRunCommand initCommand = new InitRunCommand(initRepoFileLocation: webServer.webServerConfigs.initFile as String)
            def report = initCommand.processREPO()

            if (!report)
                return webServer.showError(res, "Could not pull init")

            return webServer.showResult("Pulled init successfully")
        })

        post("/initfile/push", { Request req, Response res ->
            if (!webServer.webServerConfigs.initFile)
                return webServer.showError(res, "Missing init file")

            if (!(webServer.webServerConfigs.initFile instanceof String))
                return webServer.showError(res, "InitFile is corrupted. Contact your administrator.")

            // Invoke push hook manually
            def initFile = new File(webServer.webServerConfigs.initFile as String)
            RepoExecutor.RepoHookExecutionReport report = new RepoExecutor().with {
                parse(initFile)

                if (!repos.containsKey("main"))
                    return null

                def report = new RepoExecutor.RepoHookExecutionReport("main", repos.get("main"))
                HookExecutor.push(report)

                return report
            }

            if (!report || !report.isOk)
                return webServer.showError(res, "Could not push init")

            return webServer.showResult("Pushed init successfully")
        })
    }


    private Map initInfo() {
        if (!webServer.webServerConfigs.initFile)
            return [standalone: true]

        def initFile = new File(webServer.webServerConfigs.initFile as String)
        RepoExecutor.RepoHookExecutionReport report = new RepoExecutor().with {
            parse(initFile)

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
                source: report.descriptor.src,
                version: report.stdout.trim()
        ]
    }
}
