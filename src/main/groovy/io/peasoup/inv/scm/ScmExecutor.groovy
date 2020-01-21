package io.peasoup.inv.scm

import groovy.transform.CompileStatic
import io.peasoup.inv.Logger
import org.apache.commons.lang.RandomStringUtils

import java.util.concurrent.*

@CompileStatic
class ScmExecutor {

    final Map<String, ScmDescriptor> scms = new ConcurrentHashMap<>()

    ScmExecutor() {
    }

    void read(File scriptFile, File parametersFile = null) {
        assert scriptFile, 'Script file is required'
        assert scriptFile.exists(), 'Script file must exist on filesystem'

        ScmInvoker.invoke(new ScmHandler(this, parametersFile), scriptFile)
    }

    void add(ScmDescriptor descriptor) {
        assert descriptor, 'SCM descriptor (for a repository) is required'

        scms.put(descriptor.name, descriptor)
    }

    List<SCMReport> execute() {

        ExecutorService pool = Executors.newFixedThreadPool(4)
        final List<Future<SCMReport>> futures = []
        final List<SCMReport> reports = []

        scms.each { String name, ScmDescriptor repository ->
            futures << pool.submit({
                def report = new SCMReport(name: name, repository: repository)

                if (!repository.hooks)
                    return report

                Boolean doesPathExistsAndNotEmpty = repository.path.exists() && repository.path.list().size() > 0

                if (repository.hooks.init && !doesPathExistsAndNotEmpty) {

                    // Make sure path is clean before init
                    repository.path.deleteDir()

                    Logger.info("[SCM] ${name} [INIT] start")
                    report.isOk = executeCommands(repository, repository.hooks.init)
                    Logger.info("[SCM] ${name} [INIT] done")
                } else if (repository.hooks.update) {

                    Logger.info("[SCM] ${name} [UPDATE] start")
                    report.isOk = executeCommands(repository, repository.hooks.update)
                    Logger.info("[SCM] ${name} [UPDATE] done")
                }

                return report

            } as Callable<SCMReport>)
        }

        try {
            futures.each {
                reports.add(it.get())
            }
        }
        finally {
            pool.shutdownNow()
            pool = null
        }

        return reports
    }

    private boolean executeCommands(ScmDescriptor repository, String commands) {

        // If parent undefined, can do nothing
        if (!repository.path)
            return false

        // Make sure cache is available with minimal accesses
        if (!repository.path.exists()) {
            repository.path.mkdirs()
            repository.path.setExecutable(true)
            repository.path.setWritable(true)
            repository.path.setReadable(true)
        }

        // Create file and dirs for the SH file
        def shFile = new File(repository.path.parentFile, randomSuffix() + '.scm-sh')
        shFile.delete()

        // Write the commands into the script file
        shFile << commands

        // Calling the SH file with the commands in it
        // We can't let the runtime decide of the executing folder, so we're using the parent folder of the SH File
        def cmd = "bash ${shFile.canonicalPath}"
        def envs = repository.env.collect { "${it.key}=${it.value}"}
        def process = cmd.execute(envs, repository.path)

        Logger.debug cmd

        // Consome output and wait until done.
        process.consumeProcessOutput(System.out, System.err)
        process.waitForOrKill(repository.timeout ?: 60000)

        shFile.delete()

        if (process.exitValue() != 0) {
            Logger.warn "SCM path ${repository.path.absolutePath} was deleted since hook returned ${process.exitValue()}"

            repository.path.deleteDir()

            return false
        }

        return true
    }

    private static String randomSuffix() {
        return RandomStringUtils.random(9, true, true)
    }

    static class SCMReport {

        String name
        ScmDescriptor repository
        boolean isOk
    }
}
