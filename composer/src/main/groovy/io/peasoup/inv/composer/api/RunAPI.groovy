package io.peasoup.inv.composer.api

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import io.peasoup.inv.composer.RunFile
import io.peasoup.inv.composer.WebServer
import io.peasoup.inv.graph.GraphNavigator
import io.peasoup.inv.graph.RunGraph
import spark.Request
import spark.Response

import static spark.Spark.get
import static spark.Spark.post

class RunAPI {

    private final WebServer webServer

    RunAPI(WebServer webServer) {
        this.webServer = webServer
    }

    void routes() {
        // General
        get("/run", { Request req, Response res ->
            if (!webServer.run)
                return WebServer.showError(res, "Run is not ready yet")

            def output = webServer.run.toMap(webServer.security.isRequestSecure(req))
            output.nodes = webServer.pagination.resolve(output.nodes)

            return JsonOutput.toJson(output)
        })

        get("/run/owners", { Request req, Response res ->
            if (!webServer.run)
                return WebServer.showError(res, "Run is not ready yet")


            def ownersMap = webServer.run.owners.collect { String owner, List<GraphNavigator.Id> ids ->
                [
                        owner     : owner,
                        stagedBy : ids.findAll { webServer.run.stagedIds[it.value] && webServer.run.stagedIds[it.value].staged }.size(),
                        requiredBy: ids.findAll { webServer.run.stagedIds[it.value] && webServer.run.stagedIds[it.value].required }.size(),
                        links     : [
                                stage  : WebServer.API_CONTEXT_ROOT + "/run/stage?owner=${owner}",
                                unstage: WebServer.API_CONTEXT_ROOT + "/run/unstage?owner=${owner}",
                                tree   : WebServer.API_CONTEXT_ROOT + "/run/tree?id=${owner}"
                        ]
                ]
            }

            if (!webServer.security.isRequestSecure(req)) {
                for (Map owner : ownersMap) {
                    owner.links.remove("stage")
                    owner.links.remove("unstage")
                }
            }

            return JsonOutput.toJson(ownersMap)
        })

        get("/run/names", { Request req, Response res ->
            if (!webServer.run)
                return WebServer.showError(res, "Run is not ready yet")


            def namesMap = webServer.run.names.keySet().collect {
                [
                        name : it,
                        links: [
                                stage  : WebServer.API_CONTEXT_ROOT + "/run/stage?name=${it}",
                                unstage: WebServer.API_CONTEXT_ROOT + "/run/unstage?name=${it}"
                        ]
                ]
            }

            if (!webServer.security.isRequestSecure(req)) {
                for (Map name : namesMap) {
                    name.links.remove("stage")
                    name.links.remove("unstage")
                }
            }

            return JsonOutput.toJson(namesMap)
        })

        get("/run/tags", { Request req, Response res ->
            if (!webServer.run)
                return WebServer.showError(res, "Run is not ready yet")

            return JsonOutput.toJson(webServer.run.tagsMap(webServer.security.isRequestSecure(req)))
        })

        post("/run", { Request req, Response res ->
            if (!webServer.run)
                return WebServer.showError(res, "Run is not ready yet")

            String body = req.body()
            Map filter = [:]

            if (body)
                filter = new JsonSlurper().parseText(body) as Map

            def output = webServer.run.toMap(webServer.security.isRequestSecure(req), filter)

            Integer from = filter.from as Integer
            Integer to = filter.to as Integer
            output.nodes = webServer.pagination.resolve(output.nodes, from, to)

            return JsonOutput.toJson(output)
        })

        post("/run/staged", { Request req, Response res ->
            if (!webServer.run)
                return WebServer.showError(res, "Run is not ready yet")

            String body = req.body()
            Map filter = [:]

            if (body)
                filter = new JsonSlurper().parseText(body) as Map

            filter.staged = true

            def output = webServer.run.toMap(webServer.security.isRequestSecure(req), filter)

            Integer from = filter.from as Integer
            Integer to = filter.to as Integer
            output.nodes = webServer.pagination.resolve(output.nodes, from, to)

            return JsonOutput.toJson(output)
        })

        post("/run/stageAll", { Request req, Response res ->
            if (!webServer.run)
                return WebServer.showError(res, "Run is not ready yet")

            webServer.run.stageAll()
            webServer.run.nodes.each {
                webServer.settings.stageId(it.value)
            }

            webServer.settings.save()

            return WebServer.showResult("Ok")
        })

        post("/run/unstageAll", { Request req, Response res ->
            if (!webServer.run)
                return WebServer.showError(res, "Run is not ready yet")

            webServer.run.unstageAll()
            webServer.settings.unstageAllIds()
            webServer.settings.save()

            return WebServer.showResult("Ok")
        })

        get("/run/tree", { Request req, Response res ->
            if (!webServer.run)
                return WebServer.showError(res, "Run is not ready yet")

            def id = req.queryParams("id")

            return JsonOutput.toJson(webServer.run.getPathWithRequired(id))
        })

        get("/run/requiredBy", { Request req, Response res ->
            if (!webServer.run)
                return WebServer.showError(res, "Run is not ready yet")

            def id = req.queryParams("id")
            if (!id)
                return WebServer.showError(res, "id is required")

            return JsonOutput.toJson(webServer.run.requiredByMap(id))
        })

        post("/run/tags/stage", { Request req, Response res ->
            if (!webServer.run)
                return WebServer.showError(res, "Run is not ready yet")

            def tag = req.queryParams("tag")
            if (!tag)
                return WebServer.showError(res, "Tag is missing")

            def subtag = req.queryParams("subtag")
            if (!subtag)
                return WebServer.showError(res, "Subtag is missing")

            def tags = webServer.run.runGraph.tags.get(tag)
            if (!tags)
                return WebServer.showError(res, "No matching tag")

            def subtags = tags.get(subtag)
            if (!subtags)
                return WebServer.showError(res, "No matching subtag")

            // Fetch all invs
            for (RunGraph.VirtualInv inv : subtags) {
                if (!webServer.run.owners[inv.name])
                    continue

                // Stage broadcasts
                webServer.run.owners[inv.name].each { GraphNavigator.Linkable graphId ->
                    webServer.run.stageWithoutPropagate(graphId.value)
                    webServer.settings.stageId(graphId.value)
                    webServer.settings.save()
                }
            }
            webServer.run.propagate()

            return WebServer.showResult("Ok")
        })

        post("/run/tags/unstage", { Request req, Response res ->
            if (!webServer.run)
                return WebServer.showError(res, "Run is not ready yet")

            def tag = req.queryParams("tag")
            if (!tag)
                return WebServer.showError(res, "Tag is missing")

            def subtag = req.queryParams("subtag")
            if (!subtag)
                return WebServer.showError(res, "Subtag is missing")

            def tags = webServer.run.runGraph.tags.get(tag)
            if (!tags)
                return WebServer.showError(res, "No matching tag")

            def subtags = tags.get(subtag)
            if (!subtags)
                return WebServer.showError(res, "No matching subtag")

            // Fetch all invs
            for (RunGraph.VirtualInv inv : subtags) {
                if (!webServer.run.owners[inv.name])
                    continue

                // Unstage broadcasts
                webServer.run.owners[inv.name].each { GraphNavigator.Linkable graphId ->
                    webServer.run.unstageWithoutPropagate(graphId.value)
                    webServer.settings.unstageId(graphId.value)
                    webServer.settings.save()
                }
            }
            webServer.run.propagate()

            return WebServer.showResult("Ok")
        })

        // Specific

        post("/run/stage", { Request req, Response res ->
            if (!webServer.run)
                return WebServer.showError(res, "Run is not ready yet")

            def id = req.queryParams("id")
            if (id) {
                webServer.run.stage(id)
                webServer.settings.stageId(id)
                webServer.settings.save()

                return WebServer.showResult("Ok")
            }

            def name = req.queryParams("name")
            if (name && webServer.run.names[name]) {
                webServer.run.names[name].each { GraphNavigator.Linkable graphId ->
                    webServer.run.stageWithoutPropagate(graphId.value)
                    webServer.settings.stageId(graphId.value)
                    webServer.settings.save()
                }
                webServer.run.propagate()

                return WebServer.showResult("Ok")
            }

            def owner = req.queryParams("owner")
            if (owner && webServer.run.owners[owner]) {
                webServer.run.owners[owner].each { GraphNavigator.Linkable graphId ->
                    webServer.run.stageWithoutPropagate(graphId.value)
                    webServer.settings.stageId(graphId.value)
                    webServer.settings.save()
                }
                webServer.run.propagate()

                return WebServer.showResult("Ok")
            }

            return WebServer.showError(res, "Nothing was done")
        })

        post("/run/unstage", { Request req, Response res ->
            if (!webServer.run)
                return WebServer.showError(res, "Run is not ready yet")

            def id = req.queryParams("id")
            if (id) {
                webServer.run.unstage(id)
                webServer.settings.unstageId(id)
                webServer.settings.save()

                return WebServer.showResult("Ok")
            }

            def name = req.queryParams("name")
            if (name && webServer.run.names[name]) {
                webServer.run.names[name].each { GraphNavigator.Linkable graphId ->
                    webServer.run.unstageWithoutPropagate(graphId.value)
                    webServer.settings.unstageId(graphId.value)
                    webServer.settings.save()
                }
                webServer.run.propagate()

                return WebServer.showResult("Ok")
            }

            def owner = req.queryParams("owner")
            if (owner && webServer.run.owners[owner]) {
                webServer.run.owners[owner].each { GraphNavigator.Linkable graphId ->
                    webServer.run.unstageWithoutPropagate(graphId.value)
                    webServer.settings.unstageId(graphId.value)
                    webServer.settings.save()
                }
                webServer.run.propagate()

                return WebServer.showResult("Ok")
            }

            return WebServer.showError(res, "Nothing was done")
        })

        // runfile
        get("/runfile", { Request req, Response res ->
            if (!webServer.run)
                return WebServer.showError(res, "Run is not ready yet")

            return webServer.run.runFile.text
        })

        post("/runfile", { Request req, Response res ->
            if (!webServer.security.isRequestSecure(req))
                return WebServer.notAvailable(res)

            String source = req.body()

            if (!source)
                return WebServer.showError(res, "Content is empty")

            // Replace run.txt
            webServer.run.runFile.delete()
            webServer.run.runFile << source
            webServer.run = new RunFile(webServer.run.runFile)

            // Restage INVs
            webServer.settings.stagedIds().each {
                webServer.run.stageWithoutPropagate(it)
            }

            return WebServer.showResult("Runfile updated")
        })
    }
}
