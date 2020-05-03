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
        get("/run/file", { Request req, Response res ->
            if (!webServer.run)
                return webServer.showError(res, "Run is not ready yet")

            return webServer.run.runFile.text
        })

        post("/run/file", { Request req, Response res ->
            String source = req.body()

            if (!source)
                return webServer.showError(res, "Content is empty")

            // Replace run.txt
            webServer.run.runFile.delete()
            webServer.run.runFile << source
            webServer.run = new RunFile(webServer.run.runFile)

            return webServer.showResult("Runfile updated")
        })

        // General
        get("/run", { Request req, Response res ->
            if (!webServer.run)
                return webServer.showError(res, "Run is not ready yet")

            def output = webServer.run.nodesToMap()
            output.nodes = webServer.pagination.resolve(output.nodes)

            return JsonOutput.toJson(output)
        })

        post("/run/owners", { Request req, Response res ->
            if (!webServer.run)
                return webServer.showError(res, "Run is not ready yet")

            String body = req.body()
            Map filter = [:]

            if (body)
                filter = new JsonSlurper().parseText(body) as Map

            // TODO Rework webServer.pagination/propagation
            //def owners = webServer.run.owners.findAll { !filter.owner || it.key.contains(filter.owner) }

            return JsonOutput.toJson(webServer.run.owners.collect { String owner, List<GraphNavigator.Id> ids ->
                [
                        owner     : owner,
                        selectedBy: ids.findAll { webServer.run.staged[it.value] && webServer.run.staged[it.value].selected }.size(),
                        requiredBy: ids.findAll { webServer.run.staged[it.value] && webServer.run.staged[it.value].required }.size(),
                        links     : [
                                stage  : "/run/stage?owner=${owner}",
                                unstage: "/run/unstage?owner=${owner}",
                                tree: "/run/tree?id=${owner}"
                        ]
                ]
            })
        })

        get("/run/names", { Request req, Response res ->
            if (!webServer.run)
                return webServer.showError(res, "Run is not ready yet")

            return JsonOutput.toJson(webServer.run.names.keySet().collect {
                [
                        name : it,
                        links: [
                                stage  : "/run/stage?name=${it}",
                                unstage: "/run/unstage?name=${it}"
                        ]
                ]
            })
        })

        get("/run/tags", { Request req, Response res ->
            if (!webServer.run)
                return webServer.showError(res, "Run is not ready yet")

            def output = webServer.run.tagsMap()
            //output.nodes = webServer.pagination.resolve(output.nodes)

            return JsonOutput.toJson(output)
        })

        post("/run", { Request req, Response res ->
            if (!webServer.run)
                return webServer.showError(res, "Run is not ready yet")

            String body = req.body()
            Map filter = [:]

            if (body)
                filter = new JsonSlurper().parseText(body) as Map

            def output = webServer.run.nodesToMap(filter)

            Integer from = filter.from as Integer
            Integer to = filter.to as Integer
            output.nodes = webServer.pagination.resolve(output.nodes, from, to)

            return JsonOutput.toJson(output)
        })

        post("/run/selected", { Request req, Response res ->
            if (!webServer.run)
                return webServer.showError(res, "Run is not ready yet")

            String body = req.body()
            Map filter = [:]

            if (body)
                filter = new JsonSlurper().parseText(body) as Map

            filter.selected = true

            def output = webServer.run.nodesToMap(filter)

            Integer from = filter.from as Integer
            Integer to = filter.to as Integer
            output.nodes = webServer.pagination.resolve(output.nodes, from, to)

            return JsonOutput.toJson(output)
        })

        post("/run/stageAll", { Request req, Response res ->
            if (!webServer.run)
                return webServer.showError(res, "Run is not ready yet")

            webServer.run.stageAll()
            webServer.run.nodes.each {
                webServer.settings.stageId(it.value)
            }

            webServer.settings.save()

            return webServer.showResult("Ok")
        })

        post("/run/unstageAll", { Request req, Response res ->
            if (!webServer.run)
                return webServer.showError(res, "Run is not ready yet")

            webServer.run.unstageAll()
            webServer.settings.unstageAllIds()
            webServer.settings.save()

            return webServer.showResult("Ok")
        })

        get("/run/tree", { Request req, Response res ->
            if (!webServer.run)
                return webServer.showError(res, "Run is not ready yet")

            def id = req.queryParams("id")

            return JsonOutput.toJson(webServer.run.getPathWithRequired(id))
        })

        get("/run/requiredBy", { Request req, Response res ->
            if (!webServer.run)
                return webServer.showError(res, "Run is not ready yet")

            def id = req.queryParams("id")
            if (!id)
                return webServer.showError(res, "id is required")

            return JsonOutput.toJson(webServer.run.requiredByMap(id))
        })

        post("/run/tags/stage", { Request req, Response res ->
            if (!webServer.run)
                return webServer.showError(res, "Run is not ready yet")

            def tag = req.queryParams("tag")
            if (!tag)
                return webServer.showError(res, "Tag is missing")

            def subtag = req.queryParams("subtag")
            if (!subtag)
                return webServer.showError(res, "Subtag is missing")

            def tags = webServer.run.runGraph.tags.get(tag)
            if (!tags)
                return webServer.showError(res, "No matching tag")

            def subtags = tags.get(subtag)
            if (!subtags)
                return webServer.showError(res, "No matching subtag")

            // Fetch all invs
            for(RunGraph.VirtualInv inv : subtags) {
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

            return webServer.showResult("Ok")
        })

        post("/run/tags/unstage", { Request req, Response res ->
            if (!webServer.run)
                return webServer.showError(res, "Run is not ready yet")

            def tag = req.queryParams("tag")
            if (!tag)
                return webServer.showError(res, "Tag is missing")

            def subtag = req.queryParams("subtag")
            if (!subtag)
                return webServer.showError(res, "Subtag is missing")

            def tags = webServer.run.runGraph.tags.get(tag)
            if (!tags)
                return webServer.showError(res, "No matching tag")

            def subtags = tags.get(subtag)
            if (!subtags)
                return webServer.showError(res, "No matching subtag")

            // Fetch all invs
            for(RunGraph.VirtualInv inv : subtags) {
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

            return webServer.showResult("Ok")
        })

        // Specific

        post("/run/stage", { Request req, Response res ->
            if (!webServer.run)
                return webServer.showError(res, "Run is not ready yet")

            def id = req.queryParams("id")
            if (id) {
                webServer.run.stage(id)
                webServer.settings.stageId(id)
                webServer.settings.save()

                return webServer.showResult("Ok")
            }

            def owner = req.queryParams("owner")
            if (owner && webServer.run.owners[owner]) {
                webServer.run.owners[owner].each { GraphNavigator.Linkable graphId ->
                    webServer.run.stageWithoutPropagate(graphId.value)
                    webServer.settings.stageId(graphId.value)
                    webServer.settings.save()
                }
                webServer.run.propagate()

                return webServer.showResult("Ok")
            }


            return webServer.showError(res, "Nothing was done")
        })

        post("/run/unstage", { Request req, Response res ->
            if (!webServer.run)
                return webServer.showError(res, "Run is not ready yet")

            def id = req.queryParams("id")
            if (id) {
                webServer.run.unstage(id)
                webServer.settings.unstageId(id)
                webServer.settings.save()

                return webServer.showResult("Ok")
            }

            def owner = req.queryParams("owner")
            if (owner && webServer.run.owners[owner]) {
                webServer.run.owners[owner].each { GraphNavigator.Linkable graphId ->
                    webServer.run.unstageWithoutPropagate(graphId.value)
                    webServer.settings.unstageId(graphId.value)
                    webServer.settings.save()
                }
                webServer.run.propagate()

                return webServer.showResult("Ok")
            }

            return webServer.showError(res, "Nothing was done")
        })
    }

}
