package io.peasoup.inv.repo;

import groovy.lang.GroovyRuntimeException;
import io.peasoup.inv.run.Logger;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.groovy.runtime.ProcessGroovyMethods;
import org.codehaus.groovy.runtime.ResourceGroovyMethods;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HookExecutor {

    private static final String SHEBANG_REGEX = "^#(.*)";
    private static final Pattern SHEBANG_PATTERN = Pattern.compile(SHEBANG_REGEX);
    private static final String DEFAULT_SHEBANG_UNIX = "/bin/bash";
    private static final String DEFAULT_UNIX_EXTENSION = ".script";
    private static final String DEFAULT_SHEBANG_WINDOW = "cmd /c ";
    private static final String DEFAULT_WINDOWS_EXTENSION = ".cmd";

    private HookExecutor() {

    }

    public static void init(final RepoExecutor.RepoExecutionReport report) {
        if (report == null)
            throw new IllegalArgumentException("Report");

        if (StringUtils.isEmpty(report.getDescriptor().getHooks().getInit())) {
            Logger.warn("hook 'init' not defined for " + report.getName());
            report.setIsOk(false);
            return;
        }

        Logger.info("[REPO] name: " + report.getName() + ", path: " + report.getDescriptor().getRepoPath().getAbsolutePath() + " [INIT] start");
        executeCommands(report, report.getDescriptor().getHooks().getInit());
        Logger.info("[REPO] name: " + report.getName() + ", path: " + report.getDescriptor().getRepoPath().getAbsolutePath() + " [INIT] done");
    }

    public static void pull(final RepoExecutor.RepoExecutionReport report) {
        if (report == null)
            throw new IllegalArgumentException("Report");

        if (StringUtils.isEmpty(report.getDescriptor().getHooks().getPull())) {
            Logger.warn("hook 'pull' not defined for " + report.getName());
            report.setIsOk(false);
            return;
        }

        Logger.info("[REPO] name: " + report.getName() + ", path: " + report.getDescriptor().getRepoPath().getAbsolutePath() + " [PULL] start");
        executeCommands(report, report.getDescriptor().getHooks().getPull());
        Logger.info("[REPO] name: " + report.getName() + ", path: " + report.getDescriptor().getRepoPath().getAbsolutePath() + " [PULL] done");
    }

    public static void push(final RepoExecutor.RepoExecutionReport report) {
        if (report == null)
            throw new IllegalArgumentException("Report");

        if (StringUtils.isEmpty(report.getDescriptor().getHooks().getPush())) {
            Logger.warn("hook 'push' not defined for " + report.getName());
            report.setIsOk(false);
            return;
        }

        Logger.info("[REPO] name: " + report.getName() + ", path: " + report.getDescriptor().getRepoPath().getAbsolutePath() + " [PUSH] start");
        executeCommands(report, report.getDescriptor().getHooks().getPush());
        Logger.info("[REPO] name: " + report.getName() + ", path: " + report.getDescriptor().getRepoPath().getAbsolutePath() + " [PUSH] done");
    }

    public static void version(final RepoExecutor.RepoExecutionReport report) {
        if (report == null)
            throw new IllegalArgumentException("Report");

        if (StringUtils.isEmpty(report.getDescriptor().getHooks().getVersion())) {
            Logger.warn("hook 'version' not defined for " + report.getName());
            report.setIsOk(false);
            return;
        }

        Logger.info("[REPO] name: " + report.getName() + ", path: " + report.getDescriptor().getRepoPath().getAbsolutePath() + " [VERSION] start");
        executeCommands(report, report.getDescriptor().getHooks().getVersion(), true);
        Logger.info("[REPO] name: " + report.getName() + ", path: " + report.getDescriptor().getRepoPath().getAbsolutePath() + " [VERSIOn] done");
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static void executeCommands(RepoExecutor.RepoExecutionReport report, String commands, boolean returnStdout) {
        RepoDescriptor repository = report.getDescriptor();
        boolean shouldDeleteUponFailure = false;

        // Make sure cache is available with minimal accesses
        if (!repository.getRepoPath().exists()) {
            repository.getRepoPath().mkdirs();
            repository.getRepoPath().setExecutable(true);
            repository.getRepoPath().setWritable(true);
            repository.getRepoPath().setReadable(true);

            shouldDeleteUponFailure = true;
        }

        // Check for shebang
        // #!/bin/bash
        String currentCommands = commands;
        String program = DEFAULT_SHEBANG_UNIX;
        String extention = DEFAULT_UNIX_EXTENSION;
        List<String> envs = RepoDescriptor.getCurrentOSSet();

        if (System.getProperty("os.name").startsWith("Windows")) {
            currentCommands = "@ECHO OFF" + System.lineSeparator() + currentCommands;
            program = DEFAULT_SHEBANG_WINDOW;
            extention = DEFAULT_WINDOWS_EXTENSION;
        } else {
            int indexOfFirstNewline = commands.indexOf('\n');
            if (indexOfFirstNewline > -1) {
                String firstLine = currentCommands.substring(0, indexOfFirstNewline);
                Matcher shebangMatcher = SHEBANG_PATTERN.matcher(firstLine);
                if (shebangMatcher.matches()) {
                    currentCommands = currentCommands.substring(commands.indexOf('\n')); // remove first line
                    program = shebangMatcher.group(1);
                }
            }
        }

        // Create file and dirs for the SH file
        File shFile = new File(repository.getRepoPath().getParentFile(), randomSuffix() + extention);

        // Write the commands into the script file
        try {
            // Almost no way it could happen, but make sure Shfile is not existing before
            if (shFile.exists())
                Files.delete(shFile.toPath());

            // Writing commands to shFile
            ResourceGroovyMethods.write(shFile, currentCommands);
        } catch (IOException e) {
            Logger.error(e);
        }

        // Calling the SH file with the commands in it
        // We can't let the runtime decide of the executing folder, so we're using the parent folder of the SH File
        String cmd = program + " " + shFile.getAbsolutePath();
        Logger.system("[REPO] " + cmd);


        Integer timeout = repository.getTimeout();

        // Execute the actual process
        Process process = null;
        try {
            process = ProcessGroovyMethods.execute(cmd, envs, repository.getRepoPath());
            ProcessGroovyMethods.waitForOrKill(process, timeout != null ? timeout : 60000);
        } catch (IOException e) {
            Logger.error(e);
        }
        if (process == null)
            return;

        // Consume output and wait until done.
        try {
            if (returnStdout) {
                ExecOutput execOutput = new ExecOutput();
                ProcessGroovyMethods.waitForProcessOutput(process, execOutput, System.err);
                report.setStdout(execOutput.getOutput().toString());
            } else {
                ProcessGroovyMethods.waitForProcessOutput(process, System.out, (Appendable) System.err);
            }
        } catch(GroovyRuntimeException exception) {
            // IOException could happen if process is closed before any reading
            if (!(exception.getCause() instanceof IOException))
                Logger.error(exception);
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
                Logger.warn("REPO path " + repository.getRepoPath().toString() + " was deleted since hook returned " + process.exitValue());
                ResourceGroovyMethods.deleteDir(repository.getRepoPath());
            }

            report.setIsOk(false);
            return;
        }

        report.setIsOk(true);
    }

    private static void executeCommands(RepoExecutor.RepoExecutionReport report, String commands) {
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
