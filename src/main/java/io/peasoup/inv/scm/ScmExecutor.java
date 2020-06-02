package io.peasoup.inv.scm;

import io.peasoup.inv.run.Logger;
import org.codehaus.plexus.util.FileUtils;

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

    public void read(File scriptFile, File parametersFile) {
        if (scriptFile == null)
            throw new IllegalArgumentException("scriptFile");

        if (!scriptFile.exists())
            throw new IllegalStateException("Script file must exist on filesystem");

        ScmInvoker.invoke(new ScmHandler(this, parametersFile), scriptFile);
    }

    public void read(File scriptFile) {
        read(scriptFile, null);
    }

    public void add(ScmDescriptor descriptor) {
        if (descriptor == null)
            throw new IllegalArgumentException("descriptor");

        scms.put(descriptor.getName(), descriptor);
    }

    public List<SCMExecutionReport> execute() {

        final ExecutorService pool = Executors.newFixedThreadPool(4);
        final List<Future<SCMExecutionReport>> futures = new ArrayList<>();
        final List<SCMExecutionReport> reports = new ArrayList<>();

        for(Map.Entry<String, ScmDescriptor> kpv : scms.entrySet()) {
            futures.add(pool.submit(() -> {
                String name = kpv.getKey();
                ScmDescriptor repository = kpv.getValue();

                SCMExecutionReport report = new SCMExecutionReport(name, repository);
                Logger.debug("[SCM] script: " + repository.getName() + ", parameter: " +
                        (repository.getParametersFile() != null ? repository.getParametersFile().getAbsolutePath() : "not defined"));

                String[] pathFiles = repository.getPath().list();
                boolean doesPathExistsAndNotEmpty = repository.getPath().exists() && pathFiles != null && pathFiles.length > 0;

                if (!doesPathExistsAndNotEmpty) {

                    // Make sure path is clean before init
                    FileUtils.deleteDirectory(repository.getPath());

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

    public final Map<String, ScmDescriptor> getScms() {
        return scms;
    }

    public static class SCMExecutionReport {
        private final String name;
        private final ScmDescriptor repository;

        private boolean isOk = true;
        private String stdout;

        public SCMExecutionReport(String name, ScmDescriptor repository) {
            this.name = name;
            this.repository = repository;
        }

        public final String getName() {
            return name;
        }

        public final ScmDescriptor getRepository() {
            return repository;
        }

        public boolean isOk() {
            return isOk;
        }

        public void setIsOk(boolean isOk) {
            this.isOk = isOk;
        }

        public String getStdout() {
            return stdout;
        }

        public void setStdout(String stdout) {
            this.stdout = stdout;
        }


    }
}
