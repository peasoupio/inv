package io.peasoup.inv

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

    void call(Closure body) {
        assert body, 'Body is required'

        this.call(body, body.owner.class.simpleName)
    }

    void call(Closure body, String defaultName) {
        assert body, 'Body is required'

        Inv inv = new Inv(executor.pool)

        // Is loading from script ?
        Boolean isScript = body.owner.properties["binding"]

        if (isScript) {
            // Set default path
            inv.delegate.path =  body.binding.variables["pwd"]
        }

        body.resolveStrategy = Closure.DELEGATE_FIRST
        body.delegate = inv.delegate

        try {
            body.call()
        } catch (Exception ex) {
            executor.report.exceptions << new PoolReport.PoolException(inv: inv, exception: ex)
        } finally {
            // Atempt to dump delegate to get path or name
            inv.dumpDelegate()
        }

        if (defaultName && !inv.name)
            inv.name = defaultName

        if (!inv.name)
            throw new INVOptionRequiredException("name")

        if (isScript) {
            String scm =  body.binding.variables["scm"]
            String file =  body.binding.variables["\$0"]

            Logger.info "[$scm] [${file}] [${inv.name}]"
        }
    }

    static class INVOptionRequiredException extends Exception {

        private static final String HELP_LINK = "https://github.com/peasoupio/inv/wiki/Syntax-INV"

        INVOptionRequiredException(String option) {
            super("Option ${option} is not valid. Please visit ${HELP_LINK} for more information")
        }
    }
}


