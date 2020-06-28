package io.peasoup.inv.scm;

import io.peasoup.inv.run.Logger;
import org.codehaus.groovy.runtime.ResourceGroovyMethods;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ScmExecutor {

    private final Map<String, ScmDescriptor> scms;

    public ScmExecutor() {
        this.scms = new ConcurrentHashMap<>();
    }

    /**
     * Parse and invoke an SCM Groovy script file
     * @param scriptFile SCM Groovy script file
     */
    public void parse(File scriptFile) {
        parse(scriptFile, null);
    }

    /**
     * Parse and invoke an SCM Groovy script file with a matching parameters file
     * @param scriptFile SCM Groovy script file
     * @param parametersFile Matching parameters file
     */
    public void parse(File scriptFile, File parametersFile) {
        if (scriptFile == null)
            throw new IllegalArgumentException("scriptFile");

        if (!scriptFile.exists())
            throw new IllegalStateException("Script file must exist on filesystem");

        ScmInvoker.invoke(this, scriptFile, parametersFile);
    }

    /**
     * Adds a ScmDescriptor instance to the executor
     * @param descriptor ScmDescriptor instance
     */
    public void add(ScmDescriptor descriptor) {
        if (descriptor == null)
            throw new IllegalArgumentException("descriptor");

        scms.put(descriptor.getName(), descriptor);
    }

    /**
     * Execute all the parsed ScmDescriptors.
     * It raises automatically, if available, the "init" and "pull" hooks.
     * "init" is raised when the computed or user-defined path is not existing. Upon failure, it is automatically deleted.
     * "pull" is raised when the computed or user-defined path is existing. Upoen failure, nothing is deleted.
     * @return List of ScmExecutionReport
     */
    public List<SCMExecutionReport> execute() {

        final ExecutorService pool = Executors.newFixedThreadPool(4);
        final List<Future<SCMExecutionReport>> futures = new ArrayList<>();
        final List<SCMExecutionReport> reports = new ArrayList<>();

        for(Map.Entry<String, ScmDescriptor> kpv : scms.entrySet()) {
            futures.add(pool.submit(() -> {
                String name = kpv.getKey();
                ScmDescriptor descriptor = kpv.getValue();

                SCMExecutionReport report = new SCMExecutionReport(name, descriptor);
                Logger.debug("[SCM] script: " + descriptor.getName() + ", parameter: " +
                        (descriptor.getParametersFile() != null ? descriptor.getParametersFile().getAbsolutePath() : "not defined"));

                String[] pathFiles = descriptor.getPath().list();
                boolean doesPathExistsAndNotEmpty = descriptor.getPath().exists() && pathFiles != null && pathFiles.length > 0;

                if (!doesPathExistsAndNotEmpty) {

                    // Make sure path is clean before init (if exists)
                    // NOTE: Do not delete, might be mounted on docker.
                    ResourceGroovyMethods.deleteDir(descriptor.getPath());

                    // Execute hook
                    HookExecutor.init(report);

                } else {
                    // Execute hook
                    HookExecutor.pull(report);
                }

                return report;
            }));
        }

        // Wait for all SCM to process.
        try {
            for(Future<SCMExecutionReport> future : futures) {
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
     * Gets the map of ScmDescriptor with their respective name
     * @return Map of <String, ScmDescriptor>
     */
    public final Map<String, ScmDescriptor> getScms() {
        return scms;
    }

    public static class SCMExecutionReport {
        private final String name;
        private final ScmDescriptor descriptor;

        private boolean isOk = true;
        private String stdout;

        /**
         * Creates a new ScmExecutionReport.
         * It is usally available upon executing an ScmDescriptor through ScmExecutor.executor() or HookExecutor's methods.
         *
         * @param name String representation of the name of the ScmDescriptor
         * @param descriptor ScmDescriptor instance
         */
        public SCMExecutionReport(String name, ScmDescriptor descriptor) {
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
         * Gets the ScmDescriptor instance.
         * @return ScmDescriptor instance.
         */
        public final ScmDescriptor getDescriptor() {
            return descriptor;
        }

        /**
         * Indicates whether or not the execution was completed successively.
         * @return TRue if success, otherwise false.
         */
        public boolean isOk() {
            return isOk;
        }

        /**
         * Sets whether or not the execution was completed successively.
         * @param isOk True if success, otherwise false.
         */
        protected void setIsOk(boolean isOk) {
            this.isOk = isOk;
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
