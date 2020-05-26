package io.peasoup.inv.scm

import io.peasoup.inv.run.Logger
import io.peasoup.inv.scm.ScmExecutor.SCMExecutionReport
import org.apache.commons.lang.RandomStringUtils

class HookExecutor {

    static void init(SCMExecutionReport report) {
        if (!report) {
            throw new IllegalArgumentException("Report")
        }

        if (!report.repository.hooks.init) {
            Logger.warn("hook 'init' not defined for ${report.name}")
            report.isOk = false
            return
        }

        Logger.info("[SCM] name: ${report.name}, path: ${report.repository.path.canonicalPath} [INIT] start")
        executeCommands(report, report.repository.hooks.init)
        Logger.info("[SCM] name: ${report.name}, path: ${report.repository.path.canonicalPath} [INIT] done")
    }

    static void pull(SCMExecutionReport report) {
        if (!report) {
            throw new IllegalArgumentException("Report")
        }

        if (!report.repository.hooks.pull) {
            Logger.warn("hook 'pull' not defined for ${report.name}")
            report.isOk = false
            return
        }

        Logger.info("[SCM] name: ${report.name}, path: ${report.repository.path.canonicalPath} [PULL] start")
        executeCommands(report, report.repository.hooks.pull)
        Logger.info("[SCM] name: ${report.name}, path: ${report.repository.path.canonicalPath} [PULL] done")
    }

    static void push(SCMExecutionReport report) {
        if (!report) {
            throw new IllegalArgumentException("Report")
        }

        if (!report.repository.hooks.push) {
            Logger.warn("hook 'push' not defined for ${report.name}")
            report.isOk = false
            return
        }

        Logger.info("[SCM] name: ${report.name}, path: ${report.repository.path.canonicalPath} [PUSH] start")
        executeCommands(report, report.repository.hooks.push)
        Logger.info("[SCM] name: ${report.name}, path: ${report.repository.path.canonicalPath} [PUSH] done")
    }

    static void version(SCMExecutionReport report) {
        if (!report) {
            throw new IllegalArgumentException("Report")
        }

        if (!report.repository.hooks.version) {
            Logger.warn("hook 'version' not defined for ${report.name}")
            report.isOk = false
            return
        }

        Logger.info("[SCM] name: ${report.name}, path: ${report.repository.path.canonicalPath} [VERSION] start")
        executeCommands(report, report.repository.hooks.version, true)
        Logger.info("[SCM] name: ${report.name}, path: ${report.repository.path.canonicalPath} [VERSIOn] done")
    }

    private static void executeCommands(SCMExecutionReport report, String commands, boolean returnStdout = false) {
        ScmDescriptor repository = report.repository
        boolean shouldDeleteUponFailure = false

        // Make sure cache is available with minimal accesses
        if (!repository.path.exists()) {
            repository.path.mkdirs()
            repository.path.setExecutable(true)
            repository.path.setWritable(true)
            repository.path.setReadable(true)

            shouldDeleteUponFailure = true
        }

        // Create file and dirs for the SH file
        def shFile = new File(repository.path.parentFile, randomSuffix() + '.scm-sh')
        shFile.delete()

        // Write the commands into the script file
        shFile << commands

        // Calling the SH file with the commands in it
        // We can't let the runtime decide of the executing folder, so we're using the parent folder of the SH File
        def cmd = "bash ${shFile.canonicalPath}"
        Logger.system "[SCM] ${cmd}"

        def envs = repository.env.collect { "${it.key}=${it.value}"}
        def process = cmd.execute(envs, repository.path.canonicalFile)
        process.waitForOrKill(repository.timeout ?: 60000)

        // Consume output and wait until done.
        if (returnStdout) {
            ExecOutput execOutput = new ExecOutput()
            process.waitForProcessOutput(execOutput, System.err)

            report.stdout = execOutput.output.toString()
        } else {
            process.waitForProcessOutput(System.out, System.err)
        }

        shFile.delete()

        if (process.exitValue() != 0) {

            // Delete folder ONLY if this hook brought it
            if (shouldDeleteUponFailure) {
                Logger.warn "SCM path ${repository.path.absolutePath} was deleted since hook returned ${process.exitValue()}"
                repository.path.deleteDir()
            }

            report.isOk = false
            return
        }

        report.isOk = true
    }

    private static String randomSuffix() {
        return RandomStringUtils.random(9, true, true)
    }

    private static class ExecOutput implements Appendable {
        final StringBuilder output = new StringBuilder()

        @Override
        Appendable append(CharSequence csq) throws IOException {
            output.append(csq)
            System.out.append(csq)
        }

        @Override
        Appendable append(CharSequence csq, int start, int end) throws IOException {
            output.append(csq, start, end)
            System.out.append(csq, start, end)
        }

        @Override
        Appendable append(char c) throws IOException {
            output.append(c)
            System.out.append(c)
        }
    }
}
