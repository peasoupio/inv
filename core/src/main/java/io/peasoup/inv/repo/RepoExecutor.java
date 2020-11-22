package io.peasoup.inv.repo;

import io.peasoup.inv.Logger;
import io.peasoup.inv.io.FileUtils;
import org.codehaus.groovy.runtime.ResourceGroovyMethods;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class RepoExecutor {

    private final Map<String, RepoDescriptor> repos;

    public RepoExecutor() {
        this.repos = new ConcurrentHashMap<>();
    }

    /**
     * Parse and invoke an REPO Groovy script file
     * @param scriptFile REPO Groovy script file
     */
    public void parse(File scriptFile) {
        parse(scriptFile, null);
    }

    /**
     * Parse and invoke an REPO Groovy script file with a matching parameters file
     * @param scriptFile REPO Groovy script file
     * @param parametersFile Matching parameters file
     */
    public void parse(File scriptFile, File parametersFile) {
        if (scriptFile == null)
            throw new IllegalArgumentException("scriptFile");

        if (!scriptFile.exists())
            throw new IllegalStateException("Script file must exist on filesystem");

        RepoInvoker.invoke(this, scriptFile, parametersFile);
    }

    /**
     * Adds a RepoDescriptor instance to the executor
     * @param descriptor RepoDescriptor instance
     */
    public void add(RepoDescriptor descriptor) {
        if (descriptor == null)
            throw new IllegalArgumentException("descriptor");

        repos.put(descriptor.getName(), descriptor);
    }

    /**
     * Execute all the parsed Repo
     * Descriptors.
     * It raises automatically, if available, the "init" and "pull" hooks.
     * "init" is raised when the computed or user-defined path is not existing. Upon failure, it is automatically deleted.
     * "pull" is raised when the computed or user-defined path is existing. Upoen failure, nothing is deleted.
     * @return List of RepoHookExecutionReport
     */
    public List<RepoHookExecutionReport> execute() {

        final ExecutorService pool = Executors.newFixedThreadPool(4);
        final List<Future<RepoHookExecutionReport>> futures = new ArrayList<>();
        final List<RepoHookExecutionReport> reports = new ArrayList<>();

        // Quit right away if nothing has been parsed
        if (repos.isEmpty())
            return Collections.emptyList();

        for(Map.Entry<String, RepoDescriptor> kpv : repos.entrySet()) {
            futures.add(pool.submit(() -> {
                String name = kpv.getKey();
                RepoDescriptor descriptor = kpv.getValue();

                RepoHookExecutionReport report = new RepoHookExecutionReport(name, descriptor);
                Logger.debug("[REPO] script: " + descriptor.getName() + ", parameter: " +
                        (descriptor.getParametersFile() != null ?
                                FileUtils.convertUnixPath(descriptor.getParametersFile().getAbsolutePath()) :
                                "not defined"));

                String[] pathFiles = descriptor.getRepoPath().list();
                boolean doesPathExistsAndNotEmpty = descriptor.getRepoPath().exists() && pathFiles != null && pathFiles.length > 0;

                if (!doesPathExistsAndNotEmpty) {

                    // Make sure path is clean before init (if exists)
                    // NOTE: Do not delete, might be mounted on docker.
                    ResourceGroovyMethods.deleteDir(descriptor.getRepoPath());

                    // Execute hook
                    HookExecutor.init(report);

                } else {
                    // Execute hook
                    HookExecutor.pull(report);
                }

                return report;
            }));
        }

        // Wait for all REPO to process.
        try {
            for(Future<RepoHookExecutionReport> future : futures) {
                reports.add(future.get());
            }
        } catch(Exception ex) {
            Logger.error(ex);
        } finally {
            pool.shutdown();
        }

        return reports;
    }

    /**
     * Gets the map of RepoDescriptor with their respective name
     * @return Map of <String, RepoDescriptor>
     */
    public final Map<String, RepoDescriptor> getRepos() {
        return repos;
    }

    public static class RepoHookExecutionReport {
        private final String name;
        private final RepoDescriptor descriptor;

        private int exitCode = -1;
        private String stdout;

        /**
         * Creates a new RepoHookExecutionReport.
         * It is usally available upon executing an RepoDescriptor through RepoExecutor.executor() or HookExecutor's methods.
         *
         * @param name String representation of the name of the RepoDescriptor
         * @param descriptor RepoDescriptor instance
         */
        public RepoHookExecutionReport(String name, RepoDescriptor descriptor) {
            this.name = name;
            this.descriptor = descriptor;
        }

        /**
         * Gets the user-defined name (specified with the "name" instruction in the Groovy script file) for this descriptor.
         * It is not related to the Groovy script file name.
         * @return String representation of the name.
         */
        public final String getName() {
            return name;
        }

        /**
         * Gets the RepoDescriptor instance.
         * @return RepoDescriptor instance.
         */
        public final RepoDescriptor getDescriptor() {
            return descriptor;
        }

        /**
         * Gets the exit code of the hook's script
         * @return Int value of the exit code
         */
        public int getExitCode() {
            return exitCode;
        }

        /**
         * Sets the exit code for a hook's script execution
         * @param exitCode New int exit code
         */
        public void setExitCode(int exitCode) {
            this.exitCode = exitCode;
        }

        /**
         * Indicates whether or not the execution was completed successively.
         * @return True if success, otherwise false.
         */
        public boolean isOk() {
            return exitCode == 0;
        }

        /**
         * Gets the stdout representation of the execution.
         * @return String representation.
         */
        public String getStdout() {
            return stdout;
        }

        /**
         * Sets the stdout representation of the execution.
         * @param stdout String representation.
         */
        protected void setStdout(String stdout) {
            this.stdout = stdout;
        }



    }
}
