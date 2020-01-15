package io.peasoup.inv

class InvExecutor {

    final NetworkValuablePool pool = new NetworkValuablePool()
    NetworkValuablePool.PoolReport report = new NetworkValuablePool.PoolReport()

    InvExecutor() {

    }

    void read(File scriptFile) {
        InvInvoker.invoke(new InvHandler(this), scriptFile)
    }

    void read(String pwd, File scriptFile, String scm, Map<String, Object> inject = [:]) {
        InvInvoker.invoke(new InvHandler(this), pwd, scriptFile, scm, inject)
    }

    NetworkValuablePool.PoolReport execute() {

        int count = 0
        def haltingInProgress = false

        Logger.info "---- [DIGEST] started ----"

        try {

            // Raising ready event for all invs before the first digest
            for (int i = 0; i < pool.remainingInvs.size(); i++) {
                Inv inv = pool.remainingInvs[i]

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
}
