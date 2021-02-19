package io.peasoup.inv.run;

import io.peasoup.inv.Logger;
import io.peasoup.inv.repo.RepoExecutor;
import io.peasoup.inv.repo.RepoFolderCollection;
import io.peasoup.inv.repo.RepoURLFetcher;
import lombok.Getter;

import java.io.File;
import java.util.Queue;

/**
 * InvExecutor is the main object for parsing, invoking and executing INV files.
 */
public class InvExecutor {

    private final InvInvoker invInvoker;

    /**
     * Gets the NetworkValuablePool used for execution.
     * It is a singleton and it is accessible while running.
     * @return NetworkValuablePool object reference
     */
    @Getter
    private final NetworkValuablePool pool;

    /**
     * Gets the (ongoind) PoolReport used for execution.
     * It is a singleton and it is accessible while running.
     * @return PoolReport object reference
     */
    @Getter
    private final PoolReport report;

    @Getter
    private final RepoFolderCollection repoFolderCollection;

    /**
     * Creates a new InvExecutor object.
     */
    public InvExecutor() {
        invInvoker = new InvInvoker(this);

        pool = new NetworkValuablePool();
        report = new PoolReport();

        repoFolderCollection = new RepoFolderCollection(this);

        Logger.info("---- [DIGEST] opened ----");
    }

    public void addClass(File classFile, String packageName) {
        invInvoker.addClass(classFile, packageName);
    }

    /**
     * Will compile all added classes since latest compile.
     * It does manage deltas.
     */
    public void compileClasses() {
        invInvoker.compileClasses();
    }

    /**
     * Parse and invoke INV Groovy script file
     * @param scriptFile INV Groovy script file
     */
    public void addScript(File scriptFile) {
        invInvoker.invokeScript(scriptFile);
    }

    /**
     * Parse and invoke INV Groovy script file within a predefined package
     * @param scriptFile INV Groovy script file
     * @param packageName package name
     */
    public void addScript(File scriptFile, String packageName) {
        invInvoker.invokeScript(scriptFile, packageName);
    }

    /**
     * Parse and invoke INV Groovy script file with specific path (pwd).
     * Also, it allows to define the REPO associated to this INV
     * @param scriptFile INV Groovy script file
     * @param packageName New package assigned to the script file
     * @param pwd Pwd "Print working directory", the working directory
     * @param repo The associated REPO name
     */
    public void addScript(File scriptFile, String packageName, String pwd, String repo) {
        invInvoker.invokeScript(scriptFile, packageName, pwd, repo);
    }

    /**
     * Execute the parsed INV groovy files
     * @return Execution report
     */
    public PoolReport execute() {

        compileClasses();
        fetchGetRepoSources();

        Logger.info("---- [DIGEST] started ----");

        // Do the actual digestion
        executePoolAndReport();

        Logger.info("---- [DIGEST] completed ----");

        // TODO Horrible fix. PoolStacktraces print before pool has done printing even with flushing.
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Logger.error(e);
            Thread.currentThread().interrupt();
        }

        new PoolReportTrace(pool, report).printPoolTrace();
        new PoolReportMarkdown(pool).printPoolMarkdown();

        return report;
    }



    /**
     * Fetch files added into the RepogGetHandler instance
     */
    private void fetchGetRepoSources() {

        // Has any INV declared a load statement
        while(repoFolderCollection.getRepoExecutor().hasUnexecutedRepos()) {

            // Read current batch
            if (!repoFolderCollection.bulkRead())
                Logger.warn("Could not read fetched file(s)");
        }

    }

    private void executePoolAndReport() {
        // If something happened during read, skip execute
        if (!report.isOk()) return;
        report.reset();

        int count = 0;

        try {

            // Raising ready event for all invs before the first digest
            for (Inv inv : pool.getRemainingInvs()) {
                if (inv.getReady() == null) continue;

                Logger.info(inv + " event ready raised");
                inv.getReady().run();
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
}
