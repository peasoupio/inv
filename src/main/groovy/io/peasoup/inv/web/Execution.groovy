package io.peasoup.inv.web

import groovy.transform.CompileStatic
import io.peasoup.inv.InvExecutor
import io.peasoup.inv.Logger
import io.peasoup.inv.scm.ScmDescriptor
import io.peasoup.inv.scm.ScmExecutor

import java.nio.file.Files

@CompileStatic
class Execution {

    private static Integer MESSAGES_STATIC_CLUSTER_SIZE = 1000
    private static Integer MESSAGES_RUNNING_CLUSTER_SIZE = 50

    private final File executionsLocation
    private final File scmFolder
    private final File externalParametersFolder

    private Thread runningThread =  null
    private List<List<String>> messages = []
    private File executionLog

    Execution(File executionsLocation, File scmFolder, File externalParametersFolder) {
        assert executionsLocation
        if (!executionsLocation.exists())
            executionsLocation.mkdir()

        assert scmFolder
        if (!scmFolder.exists())
            scmFolder.mkdir()

        assert externalParametersFolder
        if (!externalParametersFolder.exists())
            externalParametersFolder.mkdir()

        this.scmFolder = scmFolder
        this.externalParametersFolder = externalParametersFolder
        this.executionsLocation = executionsLocation

        executionLog = new File(executionsLocation, "execution.log")

        if (executionLog.exists()) {
            def currentBatch = []

            executionLog.eachLine {
                if (currentBatch.size() >= MESSAGES_STATIC_CLUSTER_SIZE) {
                    messages << currentBatch.collect()
                    currentBatch.clear()
                }

                currentBatch << it
            }
            messages << currentBatch.collect()
        }
    }

    File latestLog() {
        return executionLog
    }

    boolean isRunning() {
        runningThread != null && runningThread.isAlive()
    }

    void start(List<File> scms) {

        if (isRunning())
            return

        if (executionLog.exists())
            Files.move(executionLog.toPath(), new File(executionsLocation, "execution.log." + executionLog.lastModified()).toPath())

        messages.clear()
        List<String> currentChunkOfMessages = [].asSynchronized()
        Logger.capture { String message ->
            currentChunkOfMessages << message
            sendMessages(currentChunkOfMessages)
        }

        def invExecutor = new InvExecutor()
        def scmExecutor = new ScmExecutor()

        runningThread = Thread.start {

            scms.each { scmFile ->
                scmExecutor.read(scmFile, new File(externalParametersFolder, scmFile.name.split('\\.')[0] + ".json"))
            }

            scmExecutor.execute().each { String name, ScmDescriptor repository ->

                // Manage entry points for SCM
                repository.entry.split().each {

                    def scriptFile = new File(it)
                    def path = repository.path

                    if (!scriptFile.exists()) {
                        scriptFile = new File(path, it)
                        path = scriptFile.parentFile
                    }

                    if (!scriptFile.exists()) {
                        Logger.warn "${scriptFile.canonicalPath} does not exist. Won't run."
                        return
                    }

                    Logger.info("file: ${scriptFile.canonicalPath}")
                    invExecutor.read(path.canonicalPath, scriptFile, name)
                }
            }

            Logger.info("[SCM] done")

            // Do the actual sequencing work
            invExecutor.execute()

            sendMessages(currentChunkOfMessages, false)

            List<List<String>> reorganizedMessages = []
            reorganizedMessages << []

            messages.each {
                def last = reorganizedMessages.last()

                if (last.size() >= MESSAGES_STATIC_CLUSTER_SIZE) {
                    reorganizedMessages << []
                    last = reorganizedMessages.last()
                }

                last.addAll(it)
            }

            messages = reorganizedMessages
        }
    }

    void stop() {
        if (isRunning())
            runningThread.interrupt()
    }

    private void sendMessages(List<String> currentMessages, boolean validateSize = true) {
        if (validateSize && currentMessages.size() < MESSAGES_RUNNING_CLUSTER_SIZE)
            return

        synchronized (messages) {

            // Double lock-checking
            if (validateSize && currentMessages.size() < MESSAGES_RUNNING_CLUSTER_SIZE)
                return


            currentMessages.each {
                executionLog << it + System.lineSeparator()
            }

            messages << currentMessages.collect()
            currentMessages.clear()
        }
    }

    Map toMap() {
        return [
            lastExecution: executionLog.lastModified(),
            executions: executionsLocation.listFiles().collect { it.lastModified() },
            running: isRunning(),
            links: [
                steps: messages.collect { "/execution/logs/${messages.indexOf(it)}" },
                start: "/execution/start",
                stop: "/execution/stop"
            ]
        ]
    }
}