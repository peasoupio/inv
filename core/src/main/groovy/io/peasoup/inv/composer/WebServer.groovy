package io.peasoup.inv.composer

import groovy.json.JsonOutput
import groovy.transform.CompileDynamic
import io.peasoup.inv.Logger
import io.peasoup.inv.composer.api.*
import spark.Response

import static spark.Spark.*

class WebServer {
    final static String API_CONTEXT_ROOT = "/api"

    final static String CONFIG_LOCAL_WEB = "INV_LOCAL_WEB"
    final static String CONFIG_SSL_KEYSTORE = "INV_SSL_KEYSTORE"
    final static String CONFIG_SSL_PASSWORD = "INV_SSL_PASSWORD"

    final Map webServerConfigs
    final String runLocation
    final String reposLocation
    final String hrefsLocation

    final Boot boot
    final Pagination pagination

    final Settings settings
    final RepoFileCollection repos
    final Execution exec
    RunFile run

    WebServer(Map args) {
        if (!args.workspace)
            throw new IllegalArgumentException("Args must include a 'workspace' (String) property.")

        if (args.workspace !instanceof CharSequence)
            throw new IllegalArgumentException("Args.workspace must be a String type")

        webServerConfigs = [
                port: 8080
        ] + args

        runLocation = webServerConfigs.workspace as String
        reposLocation = webServerConfigs.workspace + "/.repos" as String
        hrefsLocation = webServerConfigs.workspace + "/hrefs" as String

        // Browser configs
        port(webServerConfigs.port as int)

        // Get environment configs
        def env = System.getenv()
        def configLocalWeb = env[CONFIG_LOCAL_WEB]
        def configSslKeystore = env[CONFIG_SSL_KEYSTORE]
        def configSslPass = env[CONFIG_SSL_PASSWORD]

        // Static files
        if (configLocalWeb)
            staticFiles.externalLocation(configLocalWeb)
        else
            staticFiles.location("/public")

        // SSL configuratio
        if (configSslKeystore) {
            secure(configSslKeystore, configSslPass, null, null)
        }

        // Exception handling
        exception(Exception.class, { e, request, response ->
            Logger.error(e)
        })

        def reposLocationFolder = new File(reposLocation)
        if (!reposLocationFolder.exists())
            reposLocationFolder.mkdirs()

        def hrefsLocationFolder = new File(hrefsLocation)
        if (!hrefsLocationFolder.exists())
            hrefsLocationFolder.mkdirs()

        // Init
        settings = new Settings(new File(runLocation, "settings.json"))
        repos = new RepoFileCollection(reposLocationFolder, hrefsLocationFolder)
        exec = new Execution(webServerConfigs.appLauncher as String, reposLocationFolder)

        boot = new Boot(this)
        pagination = new Pagination(settings)
    }

    /**
     * Map available routes
     * @return
     */
    @CompileDynamic
    int routes() {

        // Register websockets
        webSocket("/execution/log/stream", Execution.MessageStreamer.class)
        webSocket("/boot/stream", Boot.MessageStreamer.class)

        // Set /api as context root
        path(API_CONTEXT_ROOT, {
            new SystemAPI(this).routes()
            new RunAPI(this).routes()
            new RepoAPI(this).routes()
            new ExecutionAPI(this).routes()
            new ReviewAPI(this).routes()
        })


        // Boot
        boot.run()

        return 0
    }

    File baseFile() {
        return new File(runLocation, "run.txt")
    }

    static String showError(Response res, String message) {
        res.status(500)

        return JsonOutput.toJson([
                message: message,
                when: System.currentTimeMillis()
        ])
    }

    static String showResult(String message) {
        return JsonOutput.toJson([
                result: message
        ])
    }
}
