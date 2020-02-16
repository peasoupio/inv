package io.peasoup.inv.run

import groovy.transform.CompileDynamic

@CompileDynamic
class InvHandler {

    private final InvExecutor executor

    InvHandler(InvExecutor executor) {
        assert executor, 'Executor is required to handle INV script(s)'

        this.executor = executor
    }

    static {
        // TODO Not ready to completely remove this.
        //ExpandoMetaClass.enableGlobally()
    }

    void call(@DelegatesTo(InvDescriptor) Closure body) {
        assert body, 'Body is required'

        this.call(body, body.owner.getClass().simpleName)
    }

    void call(@DelegatesTo(InvDescriptor) Closure body, String defaultName) {
        assert body, 'Body is required'

        Inv inv = new Inv(executor.pool)

        // Is loading from script ?
        Script script =  (body.owner instanceof Script)? body.owner as Script : null

        if (script) {
            // Set default path
            inv.delegate.path =  script.binding.variables["pwd"]
        }

        if (defaultName)
            inv.delegate.name = defaultName

        body.resolveStrategy = Closure.DELEGATE_FIRST
        body.delegate = inv.delegate

        try {
            body.call()
        } catch (Exception ex) {
            executor.report.exceptions << new PoolReport.PoolException(inv: inv, exception: ex)
        } finally {

            // Make sure, at any cost, delegate.name is not empty before dumping for the first time
            if (!inv.delegate.name)
                throw new INVOptionRequiredException("name")

            // Attempt to dump delegate to insert it into pool
            inv.dumpDelegate()
        }

        if (script) {
            String scm =  script.binding.variables["scm"]
            String file =  script.binding.variables["\$0"]

            Logger.info "[$scm] [${file}] [${inv.name}]"
        }
    }

    static class INVOptionRequiredException extends Exception {

        private static final String HELP_LINK = "https://github.com/peasoupio/inv/wiki/INV-Syntax"

        INVOptionRequiredException(String option) {
            super("Option ${option} is not valid. Please visit ${HELP_LINK} for more information")
        }
    }
}


