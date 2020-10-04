package io.peasoup.inv.composer

import groovy.json.JsonOutput
import io.peasoup.inv.fs.Pattern
import io.peasoup.inv.repo.RepoInvoker
import io.peasoup.inv.run.Logger
import io.peasoup.inv.utils.Progressbar
import org.eclipse.jetty.websocket.api.Session
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect
import org.eclipse.jetty.websocket.api.annotations.WebSocket

import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger

class Boot {

    private final WebServer webServer

    private boolean isRunning = false
    private boolean isDone = false
    private AtomicInteger thingsToDo = new AtomicInteger(0)
    private AtomicInteger thingsDone = new AtomicInteger(0)

    Boot(WebServer webServer) {
        this.webServer = webServer
    }

    boolean isDone() {
        return isDone
    }

    synchronized void run() {
        if (isRunning)
            return

        isRunning = true


        // Notifier
        new Thread({
            while (!isDone) {
                notifyListeners()

                sleep(250)
            }

            // Close sessions
            MessageStreamer.sessions.each { it.close() }
            MessageStreamer.sessions.clear()

        }).start()

        // Processor
        new Thread({
            // Process RUNFILE
            def runFile = webServer.baseFile()
            if (runFile.exists())
                webServer.run = new RunFile(runFile)

            readReposFiles()
            stageSettings()
            readRunFile()

            isDone = true
        }).start()
    }

    protected synchronized void notifyListeners() {
        // Build message
        String message = JsonOutput.toJson([
                isDone    : isDone,
                thingsToDo: thingsToDo.get(),
                thingsDone: thingsDone.get()
        ])

        // Stream message
        MessageStreamer.sessions.each {
            it.remote.sendString(message)
        }
    }

    protected void readReposFiles() {
        File repoFolder = webServer.repos.repoFolder
        def files = Pattern.get(["*"], RepoInvoker.DEFAULT_EXCLUDED, repoFolder)

        if (!files) {
            Logger.warn("No files to be found in'${repoFolder.absolutePath}'")
            return
        }

        thingsToDo.addAndGet(files.size())

        def progress = new Progressbar("Reading from '${repoFolder.absolutePath}'".toString(), files.size(), false)
        progress.start {
            files.each {
                webServer.repos.load(it)

                thingsDone.incrementAndGet()
                progress.step()
            }
        }
    }

    protected void stageSettings() {
        def stagedIds = webServer.settings.stagedIds()
        thingsToDo.addAndGet(stagedIds.size())

        def stagedRepos = webServer.settings.stagedREPOs()
        thingsToDo.addAndGet(stagedRepos.size())

        new Progressbar("Staging from 'settings.xml'", stagedIds.size() + stagedRepos.size(), false).start {
            stagedIds.each {
                if (webServer.run)
                    webServer.run.stageWithoutPropagate(it)

                thingsDone.incrementAndGet()
                step()
            }

            stagedRepos.each {
                webServer.repos.stage(it)

                thingsDone.incrementAndGet()
                step()
            }
        }
    }

    protected void readRunFile() {
        println "Checking for 'run.txt'..."

        if (webServer.run) {
            webServer.run.propagate()

            println "Found ${webServer.run.owners.size()} INV(s)"
            println "Found ${webServer.run.names.size()} unique name(s)"
            println "Found ${webServer.run.nodes.size()} broadcast(s)"
        } else {
            println "Not present right now."
        }

        println "Ready and listening on http://localhost:${webServer.webServerConfigs.port}"
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