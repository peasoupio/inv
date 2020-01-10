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

        Inv inv = new Inv()

        // Set default name
        inv.delegate.name = body.owner.class.simpleName

        // Is loading from script ?
        Boolean isScript = body.owner.properties["binding"]

        if (isScript) {
            // Set default path
            inv.delegate.path =  body.binding.variables["pwd"]
        }

        body.delegate = inv.delegate

        try {
            body.call()

            inv.dumpDelegate()

            executor.add(inv)

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

    //@Override
    Object propertyMissing(String propertyName) {
        executor.pool.checkAvailability(propertyName)
        return new StatementDescriptor(propertyName)
    }

    //@Override
    Object methodMissing(String methodName, def args) {
        executor.pool.checkAvailability(methodName)

        //noinspection GroovyAssignabilityCheck
        return new StatementDescriptor(methodName)(*args)
    }
}


