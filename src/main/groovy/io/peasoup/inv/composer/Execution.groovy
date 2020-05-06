package io.peasoup.inv.composer

import groovy.json.JsonOutput
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import io.peasoup.inv.Home
import io.peasoup.inv.Main
import io.peasoup.inv.cli.ScmCommand
import io.peasoup.inv.run.Logger
import io.peasoup.inv.run.RunsRoller
import org.eclipse.jetty.websocket.api.Session
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect
import org.eclipse.jetty.websocket.api.annotations.WebSocket

import java.nio.file.Files
import java.util.concurrent.ConcurrentLinkedQueue

@CompileStatic
class Execution {

    private final File scmFolder
    private final File externalParametersFolder

    private Process currentProcess

    private long lastExecution = 0
    private long lastExecutionStartedOn = 0

    Execution(File scmFolder, File externalParametersFolder) {
        assert scmFolder, 'SCM location (folder) is required'
        if (!scmFolder.exists())
            scmFolder.mkdir()
        assert scmFolder.isDirectory(), 'SCM location must be a directory'

        assert externalParametersFolder, 'External parameters location (folder) is required'
        if (!externalParametersFolder.exists())
            externalParametersFolder.mkdir()
        assert externalParametersFolder.isDirectory(), 'External parameters location must be a directory'

        this.scmFolder = scmFolder
        this.externalParametersFolder = externalParametersFolder

        // Set initial last execution times
        if (latestLog().exists()) {
            lastExecution = latestLog().lastModified()
            lastExecutionStartedOn = (Files.getAttribute(latestLog().toPath(), "creationTime") as java.nio.file.attribute.FileTime).toMillis()
        }
    }

    File latestRun() {
        return new File(RunsRoller.latest.folder(), "run.txt")
    }

    File latestLog() {
        return new File(RunsRoller.latest.folder(), "log.txt")
    }

    File latestScmFiles() {
        return new File(RunsRoller.runsFolder(), ScmCommand.LIST_FILE_SUFFIX)
    }

    List<String> latestScmFilesList() {
        def scmListFile = latestScmFiles()

        if (!scmListFile.exists())
            return []

        return scmListFile
                .readLines()
                .collect {
                    new File(it).name
                }
    }

    boolean isRunning() {
        currentProcess && currentProcess.isAlive()
    }

    @CompileDynamic
    void start(boolean debugMode, boolean systemMode, boolean secureMode, List<ScmFile> scms) {
        if (scms == null)
            throw new IllegalArgumentException('SCM collection is required')

        if (scms.isEmpty()) {
            Logger.warn "SCM collection is empty. Will NOT try to start execution"
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
        final File scmListFile = generateScmListFile(scms)
        final List<String> args = resolveArgs(debugMode, systemMode, secureMode, scmListFile)

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
                        startedOn: lastExecutionStartedOn
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

        // Add latest scm files (if not running)
        if (!isRunning() && latestScmFiles().exists())
            output.lastExecution["scms"] = latestScmFilesList()

        return output
    }

    private List<String> resolveArgs(boolean debugMode, boolean systemMode, boolean secureMode, File scmListFile) {

        def myClassPath = System.getProperty("java.class.path")
        def jvmArgs = ["java", "-classpath", myClassPath, Main.class.canonicalName]
        def appArgs = ["scm"]

        if (debugMode)
            appArgs << "-d"

        if (systemMode)
            appArgs << "-x"

        if (secureMode)
            appArgs << "-s"

        appArgs << scmListFile.absolutePath

        return jvmArgs + appArgs
    }

    private File generateScmListFile(List<ScmFile> scms) {
        def scmListFile = latestScmFiles()
        scmListFile.delete()

        scmListFile << JsonOutput.toJson(
                scms.collectEntries {
                    [(it.simpleName()): [
                            script               : it.scriptFile.absolutePath,
                            expectedParameterFile: it.expectedParameterFile.absolutePath
                    ]]
                }
        )

        return scmListFile
    }

    @WebSocket
    static class MessageStreamer {

        protected static final Queue<Session> sessions = new ConcurrentLinkedQueue<>()

        @OnWebSocketConnect
        void connected(Session session) {
            sessions.add(session)
        }

        @OnWebSocketClose
        void closed(Session session, int statusCode, String reason) {
            sessions.remove(session)
        }
    }
}