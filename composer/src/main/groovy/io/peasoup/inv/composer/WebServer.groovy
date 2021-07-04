package io.peasoup.inv.composer

import groovy.json.JsonOutput
import groovy.transform.CompileDynamic
import io.peasoup.inv.Home
import io.peasoup.inv.Logger
import io.peasoup.inv.composer.api.*
import spark.Response

import static spark.Spark.*

class WebServer {
    final static String API_CONTEXT_ROOT = "/api"

    final static String CONFIG_LOCAL_WEB = "INV_LOCAL_WEB"

    final static String CONFIG_SSL_KEYSTORE = "INV_SSL_KEYSTORE"
    final static String CONFIG_SSL_PASSWORD = "INV_SSL_PASSWORD"

    Map webServerConfigs
    String runLocation
    String reposLocation

    Boot boot
    Settings settings
    Security security
    Pagination pagination

    RepoFileCollection repos
    Execution exec

    RunFile run

    boolean usingSsl = false

    WebServer(Map args) {
        if (!(args.appLauncher instanceof CharSequence))
            throw new IllegalArgumentException("Args must include a 'appLauncher' (String) property.")

        if (!(args.version instanceof CharSequence))
            throw new IllegalArgumentException("Args must include a 'version' (String) property.")

        webServerConfigs = [
                port: 18080,
                workspace: Home.getCurrent().absolutePath
        ] + args

        Logger.system("[COMPOSER] settings: ${webServerConfigs}")

        runLocation = webServerConfigs.workspace as String
        reposLocation = webServerConfigs.workspace + "/repos" as String

        // Create required folders
        // ./target/runs
        if (!Home.getRunsFolder().exists())
            Home.getRunsFolder().mkdirs()

        // ./repos
        def reposLocationFolder = new File(reposLocation)
        if (!reposLocationFolder.exists())
            reposLocationFolder.mkdirs()

        boot = new Boot(this)
        settings = new Settings(new File(runLocation, Settings.DEFAULT_SETTINGS_NAME))
        security = new Security(settings)
        pagination = new Pagination(settings)

        repos = new RepoFileCollection(reposLocationFolder)
        exec = new Execution(appLauncher(), reposLocationFolder)
    }

    /**
     * Map available routes
     * @return
     */
    @CompileDynamic
    int routes() {
        // Setup Spark
        setupSpark()

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
            new AuthorizationAPI(this).routes()
        })

        // Wait for initialization
        awaitInitialization()

        security.print(webServerConfigs.port as Integer, usingSsl)

        // Execute boot sequence
        boot.run()

        return 0
    }

    File baseFile() {
        return new File(runLocation, "run.txt")
    }

    File initFile() {
        if (!webServerConfigs.initFile)
            return null

        if (!(webServerConfigs.initFile instanceof String))
            throw new IllegalStateException("InitFile is corrupted. Contact your administrator.")

        return new File(webServerConfigs.initFile as String)
    }

    String appLauncher() {
        return webServerConfigs.appLauncher
    }

    String version() {
        return webServerConfigs.version
    }

    private void setupSpark() {
        def actualPort = webServerConfigs.port as int

        // Browser configs
        if (!isTcpPortAvailable(actualPort))
            throw new IllegalStateException("port already in use")

        port(actualPort)

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

        // SSL configuration
        if (configSslKeystore) {
            secure(configSslKeystore, configSslPass, null, null)
            usingSsl = true
        }

        // Exception handling
        exception(Exception.class, { e, request, response ->
            Logger.error(e)
        })
    }

    static String notAvailable(Response res) {
        return showError(res, "Not available")
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

    private static boolean isTcpPortAvailable(int port) {
        // https://stackoverflow.com/a/48828373
        try (ServerSocket serverSocket = new ServerSocket()) {
            serverSocket.setReuseAddress(false)
            serverSocket.bind(new InetSocketAddress(InetAddress.getByName("localhost"), port), 1)
            return true
        } catch (Exception ex) {
            return false
        }
    }
}
