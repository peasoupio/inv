package io.peasoup.inv.repo;

import groovy.lang.GroovyRuntimeException;
import io.peasoup.inv.Logger;
import io.peasoup.inv.io.FileUtils;
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

    private static final String MESSAGE_REPO_START = "[REPO] name: %s, path: %s [%s] start";
    private static final String MESSAGE_REPO_DONE = "[REPO] name: %s, path: %s [%s] done";

    private static final boolean IS_WINDOWS = System.getProperty("os.name").startsWith("Windows");

    private HookExecutor() {

    }

    public static void init(final RepoExecutor.RepoHookExecutionReport report) {
        if (report == null)
            throw new IllegalArgumentException("Report");

        if (StringUtils.isEmpty(report.getDescriptor().getHooks().getInit())) {
            Logger.warn("hook 'init' not defined for " + report.getName());
            return;
        }

        Logger.info(MESSAGE_REPO_START, report.getName(), FileUtils.convertUnixPath(report.getDescriptor().getRepoPath().getAbsolutePath()), "INIT");
        executeCommands(report, report.getDescriptor().getHooks().getInit());
        Logger.info(MESSAGE_REPO_DONE, report.getName(), FileUtils.convertUnixPath(report.getDescriptor().getRepoPath().getAbsolutePath()), "INIT");
    }

    public static void pull(final RepoExecutor.RepoHookExecutionReport report) {
        if (report == null)
            throw new IllegalArgumentException("Report");

        if (StringUtils.isEmpty(report.getDescriptor().getHooks().getPull())) {
            Logger.warn("hook 'pull' not defined for " + report.getName());
            return;
        }

        Logger.info(MESSAGE_REPO_START, report.getName(), FileUtils.convertUnixPath(report.getDescriptor().getRepoPath().getAbsolutePath()), "PULL");
        executeCommands(report, report.getDescriptor().getHooks().getPull());
        Logger.info(MESSAGE_REPO_DONE, report.getName(), FileUtils.convertUnixPath(report.getDescriptor().getRepoPath().getAbsolutePath()), "PULL");
    }

    public static void push(final RepoExecutor.RepoHookExecutionReport report) {
        if (report == null)
            throw new IllegalArgumentException("Report");

        if (StringUtils.isEmpty(report.getDescriptor().getHooks().getPush())) {
            Logger.warn("hook 'push' not defined for " + report.getName());
            return;
        }

        Logger.info(MESSAGE_REPO_START, report.getName(), FileUtils.convertUnixPath(report.getDescriptor().getRepoPath().getAbsolutePath()), "PUSH");
        executeCommands(report, report.getDescriptor().getHooks().getPush());
        Logger.info(MESSAGE_REPO_DONE, report.getName(), FileUtils.convertUnixPath(report.getDescriptor().getRepoPath().getAbsolutePath()), "PUSH");
    }

    public static void version(final RepoExecutor.RepoHookExecutionReport report) {
        if (report == null)
            throw new IllegalArgumentException("Report");

        if (StringUtils.isEmpty(report.getDescriptor().getHooks().getVersion())) {
            Logger.warn("hook 'version' not defined for " + report.getName());
            return;
        }

        Logger.info(MESSAGE_REPO_START, report.getName(), FileUtils.convertUnixPath(report.getDescriptor().getRepoPath().getAbsolutePath()), "VERSION");
        executeCommands(report, report.getDescriptor().getHooks().getVersion(), true);
        Logger.info(MESSAGE_REPO_DONE, report.getName(), FileUtils.convertUnixPath(report.getDescriptor().getRepoPath().getAbsolutePath()), "VERSION");
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static void executeCommands(RepoExecutor.RepoHookExecutionReport report, String commands, boolean returnStdout) {
        RepoDescriptor repository = report.getDescriptor();
        boolean shouldDeleteUponFailure = false;

        // Make sure cache is available with minimal accesses
        if (!repository.getRepoPath().exists()) {
            repository.getRepoPath().mkdirs();
            if (!repository.getRepoPath().setExecutable(true) ||
                // https://bugs.java.com/bugdatabase/view_bug.do?bug_id=6728842
                !IS_WINDOWS && !repository.getRepoPath().setWritable(true) ||
                !repository.getRepoPath().setReadable(true))
                Logger.warn("Failed to set path permissions");

            shouldDeleteUponFailure = true;
        }

        // Check for shebang
        // #!/bin/bash
        String currentCommands = commands;
        String program = DEFAULT_SHEBANG_UNIX;
        String extension = DEFAULT_UNIX_EXTENSION;
        List<String> envs = RepoDescriptor.getCurrentOSSet();

        if (IS_WINDOWS) {
            currentCommands = "@ECHO OFF" + System.lineSeparator() + currentCommands;
            program = DEFAULT_SHEBANG_WINDOW;
            extension = DEFAULT_WINDOWS_EXTENSION;
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
        File shFile = new File(repository.getRepoPath().getParentFile(), randomSuffix() + extension);

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
        String cmd = program + " " + FileUtils.convertUnixPath(shFile.getAbsolutePath());
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

        report.setExitCode(process.exitValue());

        // Delete folder ONLY if this hook brought it
        if (report.getExitCode() != 0 &&shouldDeleteUponFailure) {
            Logger.warn("Repository for location '" + FileUtils.convertUnixPath(repository.getRepoPath().toString()) + "' was deleted since hook returned " + process.exitValue());
            ResourceGroovyMethods.deleteDir(repository.getRepoPath());
        }
    }

    private static void executeCommands(RepoExecutor.RepoHookExecutionReport report, String commands) {
        HookExecutor.executeCommands(report, commands, false);
    }

    private static String randomSuffix() {
        return RandomStringUtils.random(9, true, true);
    }

    private static class ExecOutput implements Appendable {
        private final StringBuilder output = new StringBuilder();

        @Override
        public Appendable append(CharSequence csq) {
            Logger.trace(String.valueOf(csq));
            return output.append(csq);
        }

        @Override
        public Appendable append(CharSequence csq, int start, int end) {
            return output.append(csq, start, end);
        }

        @Override
        public Appendable append(char c) {
            return output.append(c);
        }

        public final StringBuilder getOutput() {
            return output;
        }
    }
}
