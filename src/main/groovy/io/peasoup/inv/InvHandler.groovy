package io.peasoup.inv

class InvHandler {

    private NetworkValuablePool pool = new NetworkValuablePool()

    InvHandler() {

        // When trying to invoke Name as property (w/o parameters)
        InvHandler.metaClass.propertyMissing = { String propertyName ->
            pool.checkAvailability(propertyName)
            return new NetworkValuableDescriptor(propertyName)
        }

        InvHandler.metaClass.methodMissing = { String methodName, args ->
            pool.checkAvailability(methodName)
            //noinspection GroovyAssignabilityCheck
            return new NetworkValuableDescriptor(methodName)(*args)
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

    def call() {

        int count = 0
        List<Inv> digested = []
        boolean haltInProgress = false

        Logger.info "---- [DIGEST] started ----"


        try {

            // Raising ready event for all invs before the first digest
            for (int i = 0; i < pool.remainingsInv.size(); i++) {
                Inv inv = pool.remainingsInv[i]

                if (!inv.ready) {
                    continue
                }

                Logger.info "[${inv.name}] event ready raised"
                inv.ready()
            }

            // Run for eternity
            while (true) {

                // has no more work to do
                if (pool.isEmpty())
                    break

                Logger.info "---- [DIGEST] #${++count} (state=${pool.runningState()}) ----"

                // Get the next digested invs and stack them in the list
                digested.addAll(pool.digest())

                // If we were running a HALTING cycle, break the eternity loop
                if (haltInProgress) {
                    // Reset state and break loop
                    pool.runningState() == pool.RUNNING
                    break
                }

                // Has finish unbloating and halted
                if (pool.runningState() == pool.HALTING) {
                    haltInProgress = true
                }

            }
        }
        catch (Exception ex) {
            ex.printStackTrace()
        }

        // Shutdown pool executor if still running
        pool.shutdown()

        Logger.info "---- [DIGEST] completed ----"

        return [digested, !haltInProgress]
    }
}


