package io.peasoup.inv.composer

import groovy.json.JsonOutput
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import io.peasoup.inv.Home
import io.peasoup.inv.Logger
import io.peasoup.inv.run.RunsRoller
import org.eclipse.jetty.websocket.api.Session
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect
import org.eclipse.jetty.websocket.api.annotations.WebSocket

import java.nio.file.Files
import java.nio.file.attribute.FileTime
import java.util.concurrent.ConcurrentLinkedQueue

@CompileStatic
class Execution {

    private final String appLauncher
    private final File repoFolder

    private Process currentProcess

    private long lastExecution = 0
    private long lastExecutionStartedOn = 0

    Execution(String appLauncher, File repoFolder) {
        if (!appLauncher) {
            Logger.warn("AppLauncher is not defined. Classic execution will be used. Classic is for test-purposes only.")
        }

        assert repoFolder, 'REPO location (folder) is required'
        if (!repoFolder.exists())
            repoFolder.mkdir()
        assert repoFolder.isDirectory(), 'REPO location must be a directory'

        this.appLauncher = appLauncher
        this.repoFolder = repoFolder

        // Set initial last execution times
        if (latestLog().exists()) {
            lastExecution = latestLog().lastModified()
            lastExecutionStartedOn = (Files.getAttribute(latestLog().toPath(), "creationTime") as FileTime).toMillis()
        }
    }



    boolean isRunning() {
        currentProcess && currentProcess.isAlive()
    }

    @CompileDynamic
    void start(boolean debugMode, boolean systemMode, boolean secureMode, List<RepoFile> repos) {
        if (repos == null)
            throw new IllegalArgumentException('REPO collection is required')

        if (repos.isEmpty()) {
            Logger.warn "REPO collection is empty. Will NOT try to start execution"
            return
        }

        if (!Home.getCurrent().exists()) {
            Logger.warn "INV_HOME does not exists"
            return
        }

        if (isRunning())
            return

        // Create new run folder
        RunsRoller.runsFolder().mkdirs()

        // Erase previous logs
        latestLog().delete()

        // Prepare log writer
        BufferedWriter logWriter

        // Get args
        final File repoListFile = generateRepoListFile(repos)
        final List<String> args = resolveLauncherArgs(debugMode, systemMode, secureMode, repoListFile)

        Logger.system("Using arrgs: ${args}")

        new Thread({

            // Reset latest started on time
            lastExecutionStartedOn = new Date().time

            // Resolve environment variables
            def envs = System.getenv().collect { "${it.key}=${it.value}".toString() } +
                    ["INV_HOME=${Home.getCurrent().absolutePath}".toString()]

            // Do the actual execution
            currentProcess = args.execute(envs, Home.getCurrent())

            // Process output
            currentProcess.waitForProcessOutput(
                    new StringWriter() {
                        @CompileStatic
                        @Override
                        void write(String str) {
                            if (str == "\n")
                                return

                            // Make sure to create from latest folder
                            if (!logWriter)
                                logWriter = latestLog().newWriter()

                            logWriter.writeLine(str)
                            streamMessage(str)

                            System.out.println str
                        }
                    },
                    new StringWriter() {
                        @CompileStatic
                        @Override
                        void write(String str) {
                            if (str == "\n")
                                return

                            // Make sure to create from latest folder
                            if (!logWriter)
                                logWriter = latestLog().newWriter()

                            logWriter.writeLine(str)
                            streamMessage(str)

                            System.out.println str
                        }
                    }
            )
            println "Execution: stopped"

            // Set latest execution time
            lastExecution = new Date().time

            // Flushing writer(s)
            if (logWriter)
                logWriter.flush()

            // Close sessions
            MessageStreamer.sessions.each { it.close() }
            MessageStreamer.sessions.clear()

        } as Runnable).start()
    }

    void stop() {
        if (isRunning()) {
            currentProcess.destroy()
            MessageStreamer.sessions.each {
                it.close()
            }
        }
    }

    protected synchronized void streamMessage(String message) {
        // Stream message
        MessageStreamer.sessions.each {
            it.remote.sendString(message)
        }
    }

    @CompileDynamic
    Map toMap() {
        Map output = [
                lastExecution: [
                        logSize  : latestLog().length(),
                        endedOn  : lastExecution,
                        startedOn: lastExecutionStartedOn,
                        repos    : []
                ],
                executions   : !RunsRoller.runsFolder().exists() ? 0 : RunsRoller.runsFolder().listFiles()
                        .findAll { it.name.isInteger() }
                        .collect { it.lastModified() },
                running      : isRunning(),
                links        : [
                        start: WebServer.API_CONTEXT_ROOT + "/execution/start",
                        stop : WebServer.API_CONTEXT_ROOT + "/execution/stop"
                ]
        ]

        if (isRunning()) {
            output.links["stream"] = WebServer.API_CONTEXT_ROOT + "/execution/log/stream"
        }

        // Add download if latest log exists
        if (latestLog().exists()) {
            output.links["download"] = WebServer.API_CONTEXT_ROOT + "/execution/latest/download"
        }

        // Add latest repo files (if not running)
        if (!isRunning() && latestRepoFiles().exists())
            output.lastExecution["repos"] = latestRepoFilesList()

        return output
    }

    private List<String> resolveLauncherArgs(boolean debugMode, boolean systemMode, boolean secureMode, File repoListFile) {
        def appArgs = []

        if (debugMode)
            appArgs << "-d"

        if (systemMode)
            appArgs << "-x"

        if (secureMode)
            appArgs << "-s"

        appArgs << "repo-run"
        appArgs << "--list"

        appArgs << repoListFile.absolutePath

        return MainHelper.createArgs(appLauncher, appArgs)
    }

    private static File generateRepoListFile(List<RepoFile> repos) {
        def repoListFile = latestRepoFiles()
        repoListFile.delete()

        repoListFile << JsonOutput.toJson(
                repos.collectEntries {
                    [(it.simpleName()): [
                            script               : it.scriptFile.absolutePath,
                            expectedParameterFile: it.expectedParameterFile.absolutePath
                    ]]
                }
        )

        return repoListFile
    }

    static File latestRun() {
        return new File(RunsRoller.latest.folder(), "run.txt")
    }

    static File latestLog() {
        return new File(RunsRoller.latest.folder(), "log.txt")
    }

    static File latestRepoFiles() {
        return new File(RunsRoller.runsFolder(), "repos.json")
    }

    static List<String> latestRepoFilesList() {
        def repoListFile = latestRepoFiles()

        if (!repoListFile.exists())
            return []

        return repoListFile
                .readLines()
                .collect {
                    new File(it).name
                }
    }

    @WebSocket
    static class MessageStreamer {

        protected static final Queue<Session> sessions = new ConcurrentLinkedQueue<>()

        @SuppressWarnings("GrMethodMayBeStatic")
        @OnWebSocketConnect
        void connected(Session session) {
            sessions.add(session)
        }

        @SuppressWarnings("GrMethodMayBeStatic")
        @OnWebSocketClose
        void closed(Session session, int statusCode, String reason) {
            sessions.remove(session)
        }
    }
}