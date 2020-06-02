package io.peasoup.inv.scm;

import io.peasoup.inv.run.Logger;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.groovy.runtime.ProcessGroovyMethods;
import org.codehaus.groovy.runtime.ResourceGroovyMethods;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

public class HookExecutor {

    private HookExecutor() {

    }

    public static void init(final ScmExecutor.SCMExecutionReport report) {
        if (report == null)
            throw new IllegalArgumentException("Report");

        if (StringUtils.isEmpty(report.getRepository().getHooks().getInit())) {
            Logger.warn("hook 'init' not defined for " + report.getName());
            report.setIsOk(false);
            return;
        }

        Logger.info("[SCM] name: " + report.getName() + ", path: " + report.getRepository().getPath().getAbsolutePath() + " [INIT] start");
        executeCommands(report, report.getRepository().getHooks().getInit());
        Logger.info("[SCM] name: " + report.getName() + ", path: " + report.getRepository().getPath().getAbsolutePath() + " [INIT] done");
    }

    public static void pull(final ScmExecutor.SCMExecutionReport report) {
        if (report == null)
            throw new IllegalArgumentException("Report");

        if (StringUtils.isEmpty(report.getRepository().getHooks().getPull())) {
            Logger.warn("hook 'pull' not defined for " + report.getName());
            report.setIsOk(false);
            return;
        }

        Logger.info("[SCM] name: " + report.getName() + ", path: " + report.getRepository().getPath().getAbsolutePath() + " [PULL] start");
        executeCommands(report, report.getRepository().getHooks().getPull());
        Logger.info("[SCM] name: " + report.getName() + ", path: " + report.getRepository().getPath().getAbsolutePath() + " [PULL] done");
    }

    public static void push(final ScmExecutor.SCMExecutionReport report) {
        if (report == null)
            throw new IllegalArgumentException("Report");

        if (StringUtils.isEmpty(report.getRepository().getHooks().getPush())) {
            Logger.warn("hook 'push' not defined for " + report.getName());
            report.setIsOk(false);
            return;
        }

        Logger.info("[SCM] name: " + report.getName() + ", path: " + report.getRepository().getPath().getAbsolutePath() + " [PUSH] start");
        executeCommands(report, report.getRepository().getHooks().getPush());
        Logger.info("[SCM] name: " + report.getName() + ", path: " + report.getRepository().getPath().getAbsolutePath() + " [PUSH] done");
    }

    public static void version(final ScmExecutor.SCMExecutionReport report) {
        if (report == null)
            throw new IllegalArgumentException("Report");

        if (StringUtils.isEmpty(report.getRepository().getHooks().getVersion())) {
            Logger.warn("hook 'version' not defined for " + report.getName());
            report.setIsOk(false);
            return;
        }

        Logger.info("[SCM] name: " + report.getName() + ", path: " + report.getRepository().getPath().getAbsolutePath() + " [VERSION] start");
        executeCommands(report, report.getRepository().getHooks().getVersion(), true);
        Logger.info("[SCM] name: " + report.getName() + ", path: " + report.getRepository().getPath().getAbsolutePath() + " [VERSIOn] done");
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static void executeCommands(ScmExecutor.SCMExecutionReport report, String commands, boolean returnStdout) {
        ScmDescriptor repository = report.getRepository();
        boolean shouldDeleteUponFailure = false;

        // Make sure cache is available with minimal accesses
        if (!repository.getPath().exists()) {
            repository.getPath().mkdirs();
            repository.getPath().setExecutable(true);
            repository.getPath().setWritable(true);
            repository.getPath().setReadable(true);

            shouldDeleteUponFailure = true;
        }

        // Create file and dirs for the SH file
        File shFile = new File(repository.getPath().getParentFile(), randomSuffix() + ".scm-sh");

        // Write the commands into the script file
        try {
            // Almost no way it could happen, but make sure Shfile is not existing before
            if (shFile.exists())
                Files.delete(shFile.toPath());

            // Writing commands to shFile
            ResourceGroovyMethods.write(shFile, commands);
        } catch (IOException e) {
            Logger.error(e);
        }

        // Calling the SH file with the commands in it
        // We can't let the runtime decide of the executing folder, so we're using the parent folder of the SH File
        String cmd = "bash " + shFile.getAbsolutePath();
        Logger.system("[SCM] " + cmd);

        List<String> envs = ScmDescriptor.getSet();
        Integer timeout = repository.getTimeout();

        // Execute the actual process
        Process process = null;
        try {
            process = ProcessGroovyMethods.execute(cmd, envs, repository.getPath());
            ProcessGroovyMethods.waitForOrKill(process, timeout != null ? timeout : 60000);
        } catch (IOException e) {
            Logger.error(e);
        }
        if (process == null)
            return;

        // Consume output and wait until done.
        if (returnStdout) {
            ExecOutput execOutput = new ExecOutput();
            ProcessGroovyMethods.waitForProcessOutput(process, execOutput, System.err);
            report.setStdout(execOutput.getOutput().toString());
        } else {
            ProcessGroovyMethods.waitForProcessOutput(process, System.out, (Appendable)System.err);
        }

        // Delete SH file when done (successful or not)
        try {
            Files.delete(shFile.toPath());
        } catch (IOException e) {
            Logger.error(e);
        }

        if (process.exitValue() != 0) {

            // Delete folder ONLY if this hook brought it
            if (shouldDeleteUponFailure) {
                Logger.warn("SCM path " + repository.getPath().getAbsolutePath() + " was deleted since hook returned " + process.exitValue());
                ResourceGroovyMethods.deleteDir(repository.getPath());
            }

            report.setIsOk(false);
            return;
        }

        report.setIsOk(true);
    }

    private static void executeCommands(ScmExecutor.SCMExecutionReport report, String commands) {
        HookExecutor.executeCommands(report, commands, false);
    }

    private static String randomSuffix() {
        return RandomStringUtils.random(9, true, true);
    }

    private static class ExecOutput implements Appendable {
        private final StringBuilder output = new StringBuilder();

        @Override
        public Appendable append(CharSequence csq) {
            output.append(csq);
            return System.out.append(csq);
        }

        @Override
        public Appendable append(CharSequence csq, int start, int end) {
            output.append(csq, start, end);
            return System.out.append(csq, start, end);
        }

        @Override
        public Appendable append(char c) {
            output.append(c);
            return System.out.append(c);
        }

        public final StringBuilder getOutput() {
            return output;
        }
    }
}
