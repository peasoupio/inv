package io.peasoup.inv.scm

import io.peasoup.inv.Logger

class ScmReader {

    final Map<String, ScmDescriptor.MainDescriptor> scms

    ScmReader(File scmFile, File externalProperties = null) {

        def descriptor = new ScmDescriptor(scmFile.newReader())

        if (externalProperties)
            scms = descriptor.scms(externalProperties)
        else
            scms = descriptor.scms()
    }

    Map<String, List<File>> execute() {

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

            // Manage entry points for SCM
            List<File> scripts = repository.entry.split().collect {
                def scriptFile = new File(it)

                if (scriptFile.exists())
                    return scriptFile

                return new File(repository.path, it)
            }

            return [(name): scripts]
        }
    }

    private void executeCommands(ScmDescriptor.MainDescriptor repository, String commands) {

        // If parent undefined, can do nothing
        if (!repository.path)
            return

        // Make sure cache is available with minimal accesses
        if (!repository.path.exists()) {
            repository.path.mkdirs()
            repository.path.setExecutable(true)
            repository.path.setWritable(true)
            repository.path.setReadable(true)
        }

        // Create file and dirs for the SH file
        def shFile = new File(repository.path, "${repository.name}.scm-sh")
        shFile.delete()

        // Write the commands into the script file
        shFile << commands

        // Calling the SH file with the commands in it
        // We can't let the runtime decide of the executing folder, so we're using the parent folder of the SH File
        def cmd = "sh ${shFile.canonicalPath}"
        def envs = repository.env.collect { "${it.key}=${it.value}"}
        def process = cmd.execute(envs, repository.path)

        Logger.debug cmd

        // Consome output and wait until done.
        process.consumeProcessOutput(System.out, System.err)
        process.waitForOrKill(repository.timeout ?: 60000)
    }
}