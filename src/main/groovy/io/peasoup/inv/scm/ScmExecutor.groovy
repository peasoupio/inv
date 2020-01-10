package io.peasoup.inv.scm

import io.peasoup.inv.Logger
import org.apache.commons.lang.RandomStringUtils

import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

class ScmExecutor {

    final Map<String, ScmDescriptor> scms = [:]

    ScmExecutor() {
    }

    void read(File scriptFile, File parametersFile = null) {
        ScmInvoker.invoke(new ScmHandler(this, parametersFile), scriptFile)
    }

    void add(ScmDescriptor descriptor) {
        assert descriptor

        if (!descriptor.name) {
            Logger.warn("No scm name provided. Skipped")
            return
        }

        scms.put(descriptor.name, descriptor)
    }

    Map<String, ScmDescriptor> execute() {

        ExecutorService pool = Executors.newFixedThreadPool(4)
        List<Future> futures = []

        scms.collectEntries { String name, ScmDescriptor repository ->
            futures << pool.submit({
                if (!repository.path)
                    Logger.warn "path not define for scm ${name}"

                if (!repository.hooks)
                    return

                Boolean doesPathExistsAndNotEmpty = repository.path.exists() && repository.path.list().size() > 0

                if (repository.hooks.init && !doesPathExistsAndNotEmpty) {

                    // Make sure path is clean before init
                    repository.path.deleteDir()

                    Logger.info("[SCM] ${name} [INIT] start")
                    executeCommands repository, repository.hooks.init
                    Logger.info("[SCM] ${name} [INIT] done")
                } else if (repository.hooks.update) {

                    Logger.info("[SCM] ${name} [UPDATE] start")
                    executeCommands repository, repository.hooks.update
                    Logger.info("[SCM] ${name} [UPDATE] done")
                }
            } as Callable)
        }

        futures.each {
            it.get()
        }

        pool.shutdown()

        return scms
    }

    private void executeCommands(ScmDescriptor repository, String commands) {

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
        def shFile = new File(repository.path, randomSuffix() + '.scm-sh')
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

        shFile.delete()

        if (process.exitValue() != 0)
            repository.path.deleteDir()
    }

    private String randomSuffix() {
        return RandomStringUtils.random(9, true, true)
    }
}
