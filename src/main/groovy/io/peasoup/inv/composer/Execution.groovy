package io.peasoup.inv.composer

import groovy.transform.CompileStatic
import io.peasoup.inv.Main
import io.peasoup.inv.run.Logger
import io.peasoup.inv.run.RunsRoller
import org.eclipse.jetty.websocket.api.Session
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage
import org.eclipse.jetty.websocket.api.annotations.WebSocket

import java.util.concurrent.ConcurrentLinkedQueue

@CompileStatic
class Execution {

    static volatile Integer MESSAGES_STATIC_CLUSTER_SIZE = 10000

    private final File scmFolder
    private final File externalParametersFolder

    private Process currentProcess
    private List<List<String>> messages = []

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

        resizeMessagesChunks()
    }

    File latestLog() {
        return new File(RunsRoller.latest.folder(), "run.txt")
    }

    boolean isRunning() {
        currentProcess && currentProcess.isAlive()
    }

    void start(List<File> scms) {
        assert scms, 'SCM collection is required'

        if (scms.isEmpty()) {
            Logger.warn "SCM collection is empty. Will NOT try to start execution"
            return
        }

        if (!Main.currentHome.exists()) {
            Logger.warn "INV_HOME does not exists"
            return
        }

        if (isRunning())
            return

        messages.clear()

        RunsRoller.runsFolder().mkdirs()
        final def scmListFile = new File(RunsRoller.runsFolder(), "scm-list.txt")
        final def myClassPath = System.getProperty("java.class.path")
        final def args = ["java", "-classpath", myClassPath, Main.class.canonicalName, "scm", scmListFile.absolutePath]

        scmListFile.delete()
        scms.each {
            scmListFile << it.absolutePath + System.lineSeparator()
        }

        new Thread({

            def envs = System.getenv().collect { "${it.key}=${it.value}".toString() } + ["INV_HOME=${Main.currentHome.absolutePath}".toString()]

            currentProcess = args.execute(envs, Main.currentHome)
            currentProcess.waitForProcessOutput(
                    new StringWriter() {
                        @Override
                        void write(String str) {
                            if (str == "\n")
                                return

                            sendMessage(str)

                            System.out.println str
                        }
                    },
                    new StringWriter() {
                        @Override
                        void write(String str) {
                            if (str == "\n")
                                return

                            sendMessage(str)

                            System.out.println str
                        }
                    }
            )
            println "Execution: stopped"

            // Closing stuffs
            scmListFile.delete()
            resizeMessagesChunks()
            MessageStreamer.sessions.each {
                it.close()
            }

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

    private void resizeMessagesChunks() {
        if (!latestLog().exists())
            return

        messages.clear()

        def currentBatch = []

        latestLog().eachLine {
            if (currentBatch.size() >= MESSAGES_STATIC_CLUSTER_SIZE) {
                messages << currentBatch.collect()
                currentBatch.clear()
            }

            currentBatch << it
        }
        messages << currentBatch.collect()
    }

    protected synchronized void sendMessage(String message) {
        MessageStreamer.sessions.each {
            it.remote.sendString(message)
        }
    }

    Map toMap() {
        return [
            lastExecution: latestLog().lastModified(),
            executions: !RunsRoller.runsFolder().exists() ? 0 : RunsRoller.runsFolder().listFiles()
                    .findAll { it.name.isInteger() }
                    .collect { it.lastModified() },
            running: isRunning(),
            links: [
                steps: messages.collect { "/execution/logs/${messages.indexOf(it)}" },
                start: "/execution/start",
                stop: "/execution/stop"
            ]
        ]
    }

    @WebSocket
    static class MessageStreamer {

        private static final Queue<Session> sessions = new ConcurrentLinkedQueue<>()

        @OnWebSocketConnect
        void connected(Session session) {
            sessions.add(session)
        }

        @OnWebSocketClose
        void closed(Session session, int statusCode, String reason) {
            sessions.remove(session)
        }

        @OnWebSocketMessage
        void message(Session session, String message) throws IOException {
            System.out.println("Got: " + message);   // Print message
            session.getRemote().sendString(message); // and send it back
        }

    }
}