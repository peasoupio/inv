package io.peasoup.inv.composer.api

import io.peasoup.inv.Logger
import io.peasoup.inv.composer.Security
import io.peasoup.inv.composer.WebServer
import org.apache.commons.lang.StringUtils
import spark.Request
import spark.Response

import static spark.Spark.get

class AuthorizationAPI {

    private final WebServer webServer
    private final Map initInfo

    AuthorizationAPI(WebServer webServer) {
        this.webServer = webServer
    }

    void routes() {
        get("/security/apply", { Request req, Response res ->
            res.status(200)

            def token = req.queryParams("t")
            if (StringUtils.isEmpty(token))
                return ""

            if (!webServer.security.isTokenValid(token))
                return ""

            Logger.info("${req.ip()} has requested administratice accesses.")
            res.cookie("localhost", "/", Security.COOKIE_AUTH_NAME, token, 604800, false, true)
            res.redirect( "/")
        })
    }
}
