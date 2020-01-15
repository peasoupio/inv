package io.peasoup.inv

import groovy.transform.CompileDynamic

@CompileDynamic
class InvHandler {

    private final InvExecutor executor

    InvHandler(InvExecutor executor) {
        assert executor

        this.executor = executor
    }

    static {
        // TODO Not ready to completely remove this.
        //ExpandoMetaClass.enableGlobally()
    }

    void call(Closure body) {
        assert body

        this.call(body, body.owner.class.simpleName)
    }

    void call(Closure body, String defaultName) {
        assert body

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

            inv.dumpDelegate()

            if (defaultName && !inv.name)
                inv.name = defaultName

            if (isScript) {
                String scm =  body.binding.variables["scm"]
                String file =  body.binding.variables["\$0"]

                Logger.info "[$scm] [${file}] [${inv.name}]"
            }

        } catch (Exception ex) {
            // Atempt to dump delegate to get path or name
            inv.dumpDelegate()

            executor.report.exceptions.add(new NetworkValuablePool.PoolException(inv: inv, exception: ex))
        }
    }
}


