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

        boolean haltInProgress = false

        try {
            // TODO Might make a fori loop for performance
            pool.remainingsInv.each {
                if (it.ready)
                    it.ready()
            }

            while (true) {

                // has no more work to do
                if (pool.isEmpty())
                    break


                Logger.info "---- [DIGEST] #${++count} (state=${pool.runningState}) ----"
                // TODO Might make a fori loop for performance
                for (Inv digest : pool.digest()) {
                    digested.add(digest)
                }

                if (haltInProgress) {
                    // Reset state and break loop
                    pool.runningState == pool.RUNNING
                    break
                }

                // Has finish unbloating and halted
                if (pool.runningState == pool.HALTED) {
                    haltInProgress = true
                }

            }
        }
        catch (Exception ex) {
            ex.printStackTrace()
        }

        Logger.info "--- completed ----"

        // Kill pool executor if still running
        if (pool.invExecutor)
            pool.invExecutor.shutdown()

        return digested
    }
}


