package io.peasoup.inv.composer.api

import groovy.json.JsonOutput
import io.peasoup.inv.composer.Review
import io.peasoup.inv.composer.RunFile
import io.peasoup.inv.composer.WebServer
import io.peasoup.inv.run.RunsRoller
import spark.Request
import spark.Response

import java.nio.file.Files

import static spark.Spark.get
import static spark.Spark.post

class ReviewAPI {

    private final WebServer webServer

    ReviewAPI(WebServer webServer) {
        this.webServer = webServer
    }

    void routes() {
        get("/review", { Request req, Response res ->
            if (!webServer.exec.latestRun().exists())
                return webServer.showError(res, "Latest execution log does not exists on filesystem")

            def base = webServer.baseFile()

            if (!base.exists()) {
                if (!webServer.exec.latestRun().exists())
                    return webServer.showError(res, "Review is not ready yet")
                else {
                    base = new File(RunsRoller.latest.folder(), ".base.tmp")
                    base.delete()
                    base << "Generated automatically by Composer" + System.lineSeparator()
                    base << "This file is used to allow review on a first run since no promoted run.txt exists" + System.lineSeparator()

                    Files.copy(base.toPath(), webServer.baseFile().toPath())
                }
            }

            def review = new Review(base, webServer.exec.latestRun(), webServer.scms)
            return JsonOutput.toJson(review.compare())
        })

        post("/review/promote", { Request req, Response res ->
            if (!webServer.baseFile().exists())
                return webServer.showError(res, "Promote is not ready yet")

            if (!webServer.exec.latestRun().exists())
                return webServer.showError(res, "Promote is not ready yet")

            def review = new Review(webServer.baseFile(), webServer.exec.latestRun(), webServer.scms)

            if (webServer.baseFile().exists())
                review.merge()

            if (!review.promote())
                return webServer.showError(res, "failed to promote")

            // Recalculate RunFile
            webServer.run = new RunFile(webServer.baseFile())
            webServer.scms.elements.keySet().each {
                webServer.scms.unstage(it)
            }
            webServer.settings.unstageAllIds()
            webServer.settings.unstageAllSCMs()
            webServer.settings.save()

            return webServer.showResult("promoted")
        })
    }
}
