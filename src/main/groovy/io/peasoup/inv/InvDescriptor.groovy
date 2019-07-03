package io.peasoup.inv

class InvDescriptor {

    private NetworkValuablePool pool = new NetworkValuablePool()

    InvDescriptor() {

        // When trying to invoke Name as property (w/o parameters)
        InvDescriptor.metaClass.propertyMissing = { String propertyName ->
            pool.checkAvailability(propertyName)
            return new NetworkValuableDescriptor(name: propertyName)
        }

        InvDescriptor.metaClass.methodMissing = { String methodName, args ->
            pool.checkAvailability(methodName)
            //noinspection GroovyAssignabilityCheck
            return new NetworkValuableDescriptor(name: methodName)(*args)
        }
    }

    void call(Closure body) {

        assert body


        Inv inv = new Inv()

        body.delegate = inv.delegate
        body.call()

        inv.dumpDelegate()

        pool.totalInv << inv
        pool.remainingsInv << inv
    }

    List<Inv> call() {

        int count = 0
        List<Inv> digested = []

        while(true) {

            // has no more work to do
            if (pool.isEmpty())
                break

            // flagged as done, but has more work to do.
            // --- It allows the remaining NV to raise uncompleted events
            if (!pool.stillRunning) {
                pool.digest()
                pool.stillRunning = true
                break
            }

            Logger.info "---- [DIGEST] #${++count} ----"
            digested += pool.digest()
        }

        Logger.info "--- completed ----"

        return digested
    }
}
