package io.peasoup.inv.scm

import groovy.transform.CompileStatic
import io.peasoup.inv.run.Logger

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

    List<SCMExecutionReport> execute() {

        ExecutorService pool = Executors.newFixedThreadPool(4)
        final List<Future<SCMExecutionReport>> futures = []
        final List<SCMExecutionReport> reports = []

        scms.each { String name, ScmDescriptor repository ->
            futures << pool.submit({
                def report = new SCMExecutionReport(name, repository)

                Logger.debug("[SCM] script: ${repository.name}, parameter: ${repository.parametersFile ? repository.parametersFile.absolutePath : 'not defined'}")

                if (!repository.hooks)
                    return report

                Boolean doesPathExistsAndNotEmpty = repository.path.exists() && repository.path.list().size() > 0

                if (!doesPathExistsAndNotEmpty) {

                    // Make sure path is clean before init
                    repository.path.deleteDir()

                    // Execute hook
                    HookExecutor.init(report)

                } else  {
                    // Execute hook
                    HookExecutor.pull(report)
                }

                return report

            } as Callable<SCMExecutionReport>)
        }

        try {
            futures.each {
                reports.add(it.get())
            }
        }
        catch(Exception ex) {
            Logger.error(ex)
        }
        finally {
            pool.shutdownNow()
            pool = null
        }

        return reports
    }

    static class SCMExecutionReport {
        final String name
        final ScmDescriptor repository

        boolean isOk = true
        String stdout

        SCMExecutionReport(String name, ScmDescriptor repository) {
            this.name = name
            this.repository = repository
        }
    }
}
