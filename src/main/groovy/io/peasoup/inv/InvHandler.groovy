package io.peasoup.inv

class InvHandler {

    private NetworkValuablePool pool = new NetworkValuablePool()
    private NetworkValuablePool.PoolReport report = new NetworkValuablePool.PoolReport()

    static {
        ExpandoMetaClass.enableGlobally()
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

            pool.totalInv << inv
            pool.remainingsInv << inv

            if (isScript) {
                String scm =  body.binding.variables["scm"]
                String file =  body.binding.variables["\$0"]

                Logger.info "[$scm] [${file}] [${inv.name}]"
            }

        } catch (Exception ex) {
            // Atempt to dump delegate to get path or name
            inv.dumpDelegate()

            report.exceptions.add(new NetworkValuablePool.PoolException(inv: inv, exception: ex))
        }
    }

    NetworkValuablePool.PoolReport call() {

        int count = 0
        def haltingInProgress = false

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

                // Get the next digested invs
                report.eat(pool.digest())

                if (haltingInProgress) {
                    break
                }

                if (report.halted) {
                    haltingInProgress = true
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace()
        }

        Logger.info "---- [DIGEST] completed ----"

        return reset()
    }

    private NetworkValuablePool.PoolReport  reset() {
        def currentReport = report

        report = new NetworkValuablePool.PoolReport()
        pool.runningState() == pool.RUNNING
        pool.shutdown()

        return currentReport
    }

    //@Override
    def propertyMissing(String propertyName) {
        pool.checkAvailability(propertyName)
        return new NetworkValuableDescriptor(propertyName)
    }

    //@Override
    def methodMissing(String methodName, def args) {
        pool.checkAvailability(methodName)

        //noinspection GroovyAssignabilityCheck
        return new NetworkValuableDescriptor(methodName)(*args)
    }
}


