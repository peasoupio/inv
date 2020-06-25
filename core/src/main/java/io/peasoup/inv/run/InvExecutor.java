package io.peasoup.inv.run;

import java.io.File;

/**
 * InvExecutor is the main object for parsing, invoking and executing INV files.
 */
public class InvExecutor {
    private final NetworkValuablePool pool;
    private final PoolReport report;

    /**
     * Creates a new InvExecutor object.
     */
    public InvExecutor() {
        pool = new NetworkValuablePool();
        report = new PoolReport();

        Logger.info("---- [DIGEST] opened ----");
    }

    /**
     * Parse and invoke INV Groovy script file
     * @param scriptFile INV Groovy script file
     */
    public void parse(File scriptFile) {
        InvInvoker.invoke(this, scriptFile);
    }

    /**
     * Parse and invoke INV Groovy script file with specific path (pwd).
     * Also, it allows to define the SCM associated to this INV
     * @param scriptFile INV Groovy script file
     * @param pwd Pwd "Print working directory", the working directory
     * @param scm The associated SCM name
     */
    public void parse(File scriptFile, String pwd, String scm) {
        InvInvoker.invoke(this, scriptFile, pwd, scm);
    }

    /**
     * Execute the parsed INV groovy files
     * @return Execution report
     */
    public PoolReport execute() {
        Logger.info("---- [DIGEST] started ----");

        // Do the actual digestion
        executeAndReport();

        Logger.info("---- [DIGEST] completed ----");

        new PoolReportTrace(pool, report).printPoolTrace();
        new PoolReportMarkdown(pool).printPoolMarkdown();

        return report;
    }

    private void executeAndReport() {
        // If something happened during read, skip execute
        if (!report.isOk()) return;
        report.reset();

        int count = 0;

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

        report.setCycleCount(count);
    }

    /**
     * Gets the NetworkValuablePool used for execution.
     * It is a singleton and it is accessible while running.
     * @return NetworkValuablePool object reference
     */
    public final NetworkValuablePool getPool() {
        return pool;
    }

    /**
     * Gets the (ongoind) PoolReport used for execution.
     * It is a singleton and it is accessible while running.
     * @return PoolReport object reference
     */
    public final PoolReport getReport() {
        return report;
    }
}
