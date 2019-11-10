package io.peasoup.inv.scm

import io.peasoup.inv.Logger

class ScmReader {

    final Map<String, ScmDescriptor.MainDescriptor> scms

    ScmReader(File scmFile) {
        this(scmFile.newReader())
    }

    ScmReader(Reader reader) {
        // Resolve SCMS from groovy file
        scms = new ScmDescriptor(reader).scms()
    }

    Map<String, File> execute() {

        return scms.collectEntries { String name, ScmDescriptor.MainDescriptor repository ->

            if (!repository.path)
                Logger.warn "path not define for scm ${name}"

            if (!repository.hooks)
                return

            if (repository.hooks.init && !repository.path.exists()) {

                Logger.info("[SCM] ${name} [INIT] start")
                executeCommands repository, repository.hooks.init
                Logger.info("[SCM] ${name} [INIT] done")
            } else if (repository.hooks.update) {

                Logger.info("[SCM] ${name} [UPDATE] start")
                executeCommands repository, repository.hooks.update
                Logger.info("[SCM] ${name} [UPDATE] done")
            }

            return [(name): new File(repository.path, repository.entry)]
        }
    }

    private void executeCommands(ScmDescriptor.MainDescriptor repository, String commands) {

        // If parent undefined, can do nothing
        if (!repository.path.parent)
            return

        // Create file and dirs for the SH file
        def shFile = new File(repository.path.parent, ".scm-sh")
        shFile.mkdirs()

        // Make sure we're using the latest version of the commands
        if (shFile.exists())
            shFile.delete()

        // Write the commands into the script file
        shFile << commands

        // Calling the SH file with the commands in it
        // We can't let the runtime decide of the executing folder, so we're using the parent folder of the SH File
        def cmd = "sh ${shFile.canonicalPath}"
        def process = cmd.execute(null, new File(repository.path.parent))

        Logger.debug cmd

        // Consome output and wait until done.
        process.consumeProcessOutput(System.out, System.err)
        process.waitForOrKill(repository.timeout ?: 60000)
    }
}