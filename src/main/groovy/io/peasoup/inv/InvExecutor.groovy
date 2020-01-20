package io.peasoup.inv

class InvExecutor {

    final NetworkValuablePool pool
    final PoolReport report

    InvExecutor() {
        pool = new NetworkValuablePool()
        report = new PoolReport()
    }

    void read(File scriptFile) {
        InvInvoker.invoke(new InvHandler(this), scriptFile)
    }

    void read(String pwd, File scriptFile, String scm, Map<String, Object> inject = [:]) {
        InvInvoker.invoke(new InvHandler(this), pwd, scriptFile, scm, inject)
    }

    PoolReport execute() {

        // If something happened during read, skip execute
        if (!report.isOk())
            return report

        report.reset()

        int count = 0

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
            while (!report.halted) {

                // has no more work to do
                if (pool.isEmpty())
                    break

                Logger.info "---- [DIGEST] #${++count} (state=${pool.runningState()}) ----"

                // Get the next digested invs
                report.eat(pool.digest())
            }
        }
        catch (Exception ex) {
            ex.printStackTrace()
        }
        finally {
            pool.shutdown()
            report.printPoolTrace(pool)
        }

        Logger.info "---- [DIGEST] completed ----"

        return report
    }
}
