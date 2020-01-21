package io.peasoup.inv.web

import groovy.transform.CompileStatic
import io.peasoup.inv.Logger
import io.peasoup.inv.Main

import java.nio.file.Files
import java.util.concurrent.ConcurrentLinkedQueue

@CompileStatic
class Execution {

    static volatile Integer MESSAGES_STATIC_CLUSTER_SIZE = 1000
    static volatile Integer MESSAGES_RUNNING_CLUSTER_SIZE = 50

    private final File executionsLocation
    private final File scmFolder
    private final File externalParametersFolder

    private Process currentProcess
    private List<List<String>> messages = []
    private File executionLog

    Execution(File executionsLocation, File scmFolder, File externalParametersFolder) {
        assert executionsLocation, 'Executions location (folder) is required'
        if (!executionsLocation.exists())
            executionsLocation.mkdir()
        assert executionsLocation.isDirectory(), 'Executions location must be a directory'

        assert scmFolder, 'SCM location (folder) is required'
        if (!scmFolder.exists())
            scmFolder.mkdir()
        assert executionsLocation.isDirectory(), 'SCM location must be a directory'

        assert externalParametersFolder, 'External parameters location (folder) is required'
        if (!externalParametersFolder.exists())
            externalParametersFolder.mkdir()
        assert executionsLocation.isDirectory(), 'External parameters location must be a directory'

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
        currentProcess && currentProcess.isAlive()
    }

    void start(List<File> scms) {
        assert scms, 'SCM collection is required'

        if (scms.isEmpty()) {
            Logger.warn "SCM collection is empty. Will NOT try to start execution"

            return
        }

        if (isRunning())
            return

        if (executionLog.exists())
            Files.move(executionLog.toPath(), new File(executionsLocation, "execution.log." + executionLog.lastModified()).toPath())

        messages.clear()
        ConcurrentLinkedQueue<String> currentChunkOfMessages = new ConcurrentLinkedQueue<>()

        final def scmListFile = new File(executionsLocation, "scm-list.txt")
        scmListFile.delete()

        final def myClassPath = System.getProperty("java.class.path")
        final def args = ["java", "-classpath", myClassPath, Main.class.canonicalName, "scm", scmListFile.absolutePath]

        scms.each {
            scmListFile << it.absolutePath + System.lineSeparator()
        }

        new Thread({
            currentProcess = args.execute()
            currentProcess.waitForProcessOutput(
                    new StringWriter() {
                        @Override
                        void write(String str) {
                            if (str == "\n")
                                return

                            currentChunkOfMessages << str
                            sendMessages(currentChunkOfMessages)

                            System.out.println str
                        }
                    },
                    new StringWriter() {
                        @Override
                        void write(String str) {
                            if (str == "\n")
                                return

                            currentChunkOfMessages << str
                            sendMessages(currentChunkOfMessages)

                            System.out.println str
                        }
                    }
            )

            println "Execution: stopped"

            sendMessages(currentChunkOfMessages, false)

        } as Runnable).start()
    }

    void stop() {
        if (isRunning()) {
            currentProcess.destroy()
        }
    }

    protected synchronized void sendMessages(ConcurrentLinkedQueue<String> currentMessages, boolean validateSize = true) {
        if (validateSize && currentMessages.size() < MESSAGES_RUNNING_CLUSTER_SIZE)
            return

        // Double lock-checking
        if (validateSize && currentMessages.size() < MESSAGES_RUNNING_CLUSTER_SIZE)
            return

        List<String> tmpList = []
        for(def i = 0; i< MESSAGES_RUNNING_CLUSTER_SIZE; i++) {
            tmpList << currentMessages.poll()
        }

        tmpList.each {
            executionLog << it + System.lineSeparator()
        }

        messages << tmpList
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