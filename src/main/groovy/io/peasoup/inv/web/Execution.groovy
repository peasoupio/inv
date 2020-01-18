package io.peasoup.inv.web

import groovy.transform.CompileStatic
import io.peasoup.inv.Logger
import io.peasoup.inv.Main

import java.nio.file.Files

@CompileStatic
class Execution {

    static Integer MESSAGES_STATIC_CLUSTER_SIZE = 1000
    static Integer MESSAGES_RUNNING_CLUSTER_SIZE = 50

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
        List<String> currentChunkOfMessages = [].asSynchronized() as List<String>

        new Thread({

            def myClassPath = System.getProperty("java.class.path")
            def args = ["java", "-classpath", myClassPath, Main.class.canonicalName, "scm"]

            scms.each {
                args.add(it.absolutePath)
            }

            currentProcess = args.execute()
            currentProcess.consumeProcessOutput(
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
            currentProcess.waitFor()

            println "Execution: stopped"

            sendMessages(currentChunkOfMessages, false)

        } as Runnable).start()
    }

    void stop() {
        if (isRunning()) {
            currentProcess.destroy()
        }
    }

    protected void sendMessages(List<String> currentMessages, boolean validateSize = true) {
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