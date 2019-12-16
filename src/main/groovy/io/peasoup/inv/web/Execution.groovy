package io.peasoup.inv.web

import io.peasoup.inv.InvHandler
import io.peasoup.inv.InvInvoker
import io.peasoup.inv.Logger
import io.peasoup.inv.scm.ScmReader

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

        runningThread = Thread.start {

            def inv = new InvHandler()

            scms.each { scmFile ->
                def invFiles = new ScmReader(
                        scmFile,
                        new File(externalParametersFolder, scmFile.name.split('\\.')[0] + ".properties")
                ).execute()

                invFiles.each { String name, List<File> scripts ->

                    scripts.each { File script ->
                        if (!script.exists()) {
                            Logger.warn "${script.absolutePath} does not exist. Won't run."
                            return
                        }

                        Logger.info("file: ${script.absolutePath}")
                        InvInvoker.invoke(inv, script, name)
                    }
                }
            }

            Logger.info("[SCM] done")

            def currentMessages = []

            Logger.capture { String message ->
                if (currentMessages.size() > 1000) {
                    messages << currentMessages
                    currentMessages = []
                }

                currentMessages << message
            }

            inv()

            messages << currentMessages
        }
    }

    void stop() {

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