package io.peasoup.inv.scm

import groovy.transform.CompileStatic

@CompileStatic
class ScmHandler {

    private final ScmExecutor executor
    private final File parametersFile

    ScmHandler(ScmExecutor executor, File parametersFile = null) {
        assert executor

        this.executor = executor
        this.parametersFile = parametersFile
    }

    void call(Closure body) {
        def scmConfigDescriptor = new ScmDescriptor(parametersFile)

        body.resolveStrategy = Closure.DELEGATE_ONLY
        body.delegate = scmConfigDescriptor

        try {
            body()
        } catch (MissingMethodException ex) {
            throw new Exception("Scm instruction '${ex.method}' not found for arguments: ${ex.arguments.collect { "${it} (${it.class.name})"}.join(',') }")
        }

        executor.add(scmConfigDescriptor)
    }
}
