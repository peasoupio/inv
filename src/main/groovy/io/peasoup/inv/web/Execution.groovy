package io.peasoup.inv.web

import groovy.transform.CompileStatic
import io.peasoup.inv.InvHandler
import io.peasoup.inv.InvInvoker
import io.peasoup.inv.Logger
import io.peasoup.inv.scm.ScmDescriptor
import io.peasoup.inv.scm.ScmReader

@CompileStatic
class Execution {

    private final File scmFolder
    private final File externalParametersFolder

    private Thread runningThread =  null
    private List<List<String>> messages = []

    Execution(File scmFolder, File externalParametersFolder) {
        assert scmFolder
        assert scmFolder.exists()

        assert externalParametersFolder
        assert externalParametersFolder.exists()

        this.scmFolder = scmFolder
        this.externalParametersFolder = externalParametersFolder
    }

    boolean isRunning() {
        runningThread != null && runningThread.isAlive()
    }

    void start(List<File> scms) {

        if (isRunning())
            return

        messages.clear()
        List<String> currentChunkOfMessages = [].asSynchronized()
        Logger.capture { String message ->
            currentChunkOfMessages << message
            sendMessages(currentChunkOfMessages)
        }

        runningThread = Thread.start {

            def inv = new InvHandler()

            scms.each { scmFile ->
                def invFiles = new ScmReader(
                        scmFile,
                        new File(externalParametersFolder, scmFile.name.split('\\.')[0] + ".properties")
                ).execute()


                invFiles.each { String name, ScmDescriptor.MainDescriptor repository ->

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
                        InvInvoker.invoke(inv, path.canonicalPath, scriptFile, name)
                    }
                }
            }

            Logger.info("[SCM] done")

            inv()

            messages << currentChunkOfMessages
        }
    }

    void stop() {

    }

    private void sendMessages(List<String> currentMessages) {
        if (currentMessages.size() < 50)
            return

        messages << currentMessages.collect()
        currentMessages.clear()
    }

    Map toMap() {
        return [
            running: isRunning(),
            links: [
                steps: messages.collect { "/execution/logs/${messages.indexOf(it)}" },
                start: "/execution/start",
                stop: "/execution/stop"
            ]
        ]
    }
}