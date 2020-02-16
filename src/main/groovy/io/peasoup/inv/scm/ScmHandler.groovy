package io.peasoup.inv.scm

import groovy.transform.CompileStatic

@CompileStatic
class ScmHandler {

    private final ScmExecutor executor
    private final File parametersFile

    ScmHandler(ScmExecutor executor, File parametersFile = null) {
        assert executor, 'SCM executor is required to handle SCM scripts'

        this.executor = executor
        this.parametersFile = parametersFile
    }

    void call(Closure body) {
        assert body, 'Body is required'

        def scmConfigDescriptor = new ScmDescriptor(parametersFile)

        body.resolveStrategy = Closure.DELEGATE_ONLY
        body.delegate = scmConfigDescriptor

        try {
            body()
        } catch (MissingMethodException ex) {
            throw new Exception("Scm instruction '${ex.method}' not found for arguments: ${ex.arguments.collect { "${it} (${it.class.name})"}.join(',') }")
        }

        if (!scmConfigDescriptor.name)
            throw new SCMOptionRequiredException("path")


        if (!scmConfigDescriptor.path)
            throw new SCMOptionRequiredException("path")

        executor.add(scmConfigDescriptor)
    }

    static class SCMOptionRequiredException extends Exception {

        private static final String HELP_LINK = "https://github.com/peasoupio/inv/wiki/SCM-Syntax"

        SCMOptionRequiredException(String option) {
            super("Option ${option} is not valid. Please visit ${HELP_LINK} for more information")
        }
    }
}
