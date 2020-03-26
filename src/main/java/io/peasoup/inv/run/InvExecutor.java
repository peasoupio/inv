package io.peasoup.inv.run;

import java.io.File;
import java.io.IOException;

public class InvExecutor {
    private final NetworkValuablePool pool;
    private final PoolReport report;

    public InvExecutor() {
        pool = new NetworkValuablePool();
        report = new PoolReport();
    }

    public void read(File scriptFile) throws IOException {
        InvInvoker.invoke(new InvHandler(this), scriptFile);
    }

    public void read(String pwd, File scriptFile, String scm) throws IOException {
        InvInvoker.invoke(new InvHandler(this), pwd, scriptFile, scm);
    }

    public PoolReport execute() {
        executeAndReport();

        report.printPoolTrace(pool);

        return report;
    }

    private void executeAndReport() {
        // If something happened during read, skip execute
        if (!report.isOk()) return;
        report.reset();

        int count = 0;

        Logger.info("---- [DIGEST] started ----");

        try {

            // Raising ready event for all invs before the first digest
            for (Inv inv : pool.getRemainingInvs()) {
                if (inv.getReady() == null) continue;

                Logger.info(inv + " event ready raised");
                inv.getReady().call();
            }


            // Run for eternity
            while (!report.isHalted()) {

                // has no more work to do
                if (pool.isEmpty()) break;

                ++count;
                Logger.info("---- [DIGEST] #" + count + " (state=" + getPool().runningState() + ") ----");

                // Get the next digested invs
                report.eat(pool.digest());
            }

        } catch (Exception ex) {
            Logger.error(ex);
        }

        Logger.info("---- [DIGEST] completed ----");

        report.setCycleCount(count);
    }

    public final NetworkValuablePool getPool() {
        return pool;
    }

    public final PoolReport getReport() {
        return report;
    }
}
