package io.peasoup.inv.run;

import io.peasoup.inv.Logger;
import lombok.Getter;
import org.apache.commons.lang.StringUtils;

import java.util.*;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingDeque;

public class NetworkValuablePool {
    public static final String HALTING = "HALTED";
    public static final String CLEANING = "CLEANING";
    public static final String RUNNING = "RUNNING";

    private final NetworkValuablePoolIngester ingester;

    @Getter private final NetworkValuablePoolBroadcastMap availableMap = new NetworkValuablePoolBroadcastMap(this);
    @Getter private final NetworkValuablePoolBroadcastMap stagingMap = new NetworkValuablePoolBroadcastMap(this);
    @Getter private final NetworkValuablePoolBroadcastMap cleanedMap = new NetworkValuablePoolBroadcastMap(this);

    @Getter private final Set<Inv> remainingInvs = ConcurrentHashMap.newKeySet();
    @Getter private final Set<Inv> completedInvs = ConcurrentHashMap.newKeySet();
    @Getter private final Set<Inv> totalInvs = ConcurrentHashMap.newKeySet();

    @Getter private final NetworkValuablePoolWatchList watchList;

    /**
     * Gets the current state
     *
     * @return the string value of the current state
     */
    @Getter protected volatile String runningState;

    /**
     * Check if the pool is currently ingesting
     *
     * @return True if ingesting, otherwise false
     */
    @Getter private volatile boolean isIngesting;

    private volatile int latestCompletedCount = 0;

    public NetworkValuablePool() {
        this.ingester = new NetworkValuablePoolIngester(this);
        this.watchList = new NetworkValuablePoolWatchList();

        this.runningState  = RUNNING;
        this.isIngesting = false;
    }

    /**
     * Digest invs and theirs statements.
     * This is the main function to resolve requirements and broadcasts within the current pool.
     * <p>
     * Invs and statements are pushed through different cycles : RUNNING, REMOVED and HALTED.
     * Cycles can be altered based on how statements are matching each other.
     * <p>
     * During the RUNNING cycle, matches are multithreaded.
     * <p>
     * RUNNING cycle ends when there is no more matching being done. The CLEANING cycle is set.
     * During the REMOVING cycle, it removes optional requirements to "unjam" the pool.
     * If enough require statements are removed to meet a broadcast statement, pool is set back to RUNNING and restarts
     * a cycle
     * <p>
     * If nothing is broadcast during the CLEANING cycle, the HALTING cycle is raised.
     * During the HALTED cycle, even a broadcast cannot reset the digestion cycles to RUNNING.
     * The pool is meant to be closed and restarted manually.
     *
     * @return list of inv completed during this digestion cycle
     */
    public PoolReport digest() {
        isIngesting = true;
        try {
            return proceedDigest();
        } catch (Exception ex) {
            Logger.error(ex);
            throw ex;
        } finally {
            isIngesting = false;
        }
    }

    private PoolReport proceedDigest() {

        // If running in halted mode, skip cycle
        if (isHalting())
            return new PoolReport(
                    new LinkedList<>(),
                    true);

        // All digestions
        Inv.Digestion digestion = new Inv.Digestion();

        // Get sorted INV
        List<Inv> sorted = sortRemainings();

        // Eat invs
        Queue<PoolReport.PoolError> errorsCaught = null;

        // Eat invs
        if (isRunning())
            errorsCaught = eatMultithreaded(digestion);

        if (isCleaning())
            errorsCaught = eatSynchronized(sorted, digestion);

        // Check for new broadcasts
        boolean hasStagedSomething = digestion.getBroadcasted() != null && !digestion.getBroadcasted().isEmpty();

        // Check if an INV has been completed.
        boolean hasCompletedSomething = latestCompletedCount > 0;
        latestCompletedCount = 0;

        // Prepare state for next cycle
        evaluateState(
                hasCompletedSomething,
                hasStagedSomething
        );

        return new PoolReport(errorsCaught, runningState.equals(HALTING));
    }

    /**
     * Eat synchronously INV collection
     * @param invs INV collection to eat
     * @param cycleDigestion Cycle digestion
     * @return Pool errors
     */
    private Queue<PoolReport.PoolError> eatSynchronized(List<Inv> invs, Inv.Digestion cycleDigestion) {

        BlockingDeque<PoolReport.PoolError> poolErrors = new LinkedBlockingDeque<>();
        
        for (final Inv inv : invs) {
            String stateBefore = runningState;
            NetworkValuablePoolIngester.IngestedInv ingestedInv = ingester.ingest(inv, poolErrors);

            // If eaten INV has an error, skip and process the next INV
            if (ingestedInv.hasError())
                continue;

            removeIfCompleted(inv);

            cycleDigestion.concat(ingestedInv.getDigestion());

            // If state has changed or the eaten INV interrupted the cycle, quit
            if (ingestedInv.getDigestion().isInterrupted() && !stateBefore.equals(runningState))
                break;
        }

        // Check for broadcasts
        ingester.stageBroadcasts();

        // Print broadcasts
        ingester.printStagedBroadcasts();

        return poolErrors;
    }

    /**
     * Eat "multithread-ly" INV collection
     * @param cycleDigestion Cycle digestion
     * @return Pool errors
     */
    private Queue<PoolReport.PoolError> eatMultithreaded(final Inv.Digestion cycleDigestion) {
        BlockingDeque<PoolReport.PoolError> poolErrors = new LinkedBlockingDeque<>();

        // Execute INVs
        new NetworkValuablePoolExecutor(this, ingester, cycleDigestion, poolErrors).start();

        // Print broadcasts
        ingester.printStagedBroadcasts();

        return poolErrors;
    }

    /**
     * Determine the next cycle state
     * @param hasDoneSomething invs were completed
     * @param hasStagedSomething broadcast statements where met
     */
    private void evaluateState(boolean hasDoneSomething, boolean hasStagedSomething) {
        if (hasDoneSomething) {// Has completed Invs
            startRunning();
        } else if (hasStagedSomething) {// Has dumped something
            startRunning();
        } else if (runningState.equals(CLEANING)) {
            startHalting();// Has already start cleaning, but did not do anything ? Halt pool
        } else {
            startCleaning();// Should start cleaning
        }
    }

    /**
     * Add an INV to the pool.
     * @param inv The INV to add
     * @param forceRecalculateWatchlist True to force the watchlist recalculation.
     *                                  Occurs when a statement is added after this owning INV is added.
     * @return True if added, otherwise false.
     */
    public boolean add(Inv inv, boolean forceRecalculateWatchlist) {
        if (inv == null) {
            throw new IllegalArgumentException("Inv is required");
        }

        if (StringUtils.isEmpty(inv.getName()))
            return false;

        boolean dumped = false;

        // Add INV to pool if not already there
        if (!totalInvs.contains(inv)) {
            // Add inv to main collections
            totalInvs.add(inv);
            remainingInvs.add(inv);

            dumped = true;
        }

        if (dumped || forceRecalculateWatchlist) {
            watchList.addWatcher(inv);
        }

        return dumped;
    }


    /**
     * Remove INV from remaining queue if completed.
     * @param inv The INV to remove
     */
    public synchronized void removeIfCompleted(Inv inv) {
        if (inv == null)
            throw new IllegalArgumentException("inv");

        if (!inv.isCompleted())
            return;

        if (!remainingInvs.contains(inv))
            return;

        Logger.system("[POOL] inv: " + inv.getName() + " COMPLETED");

        remainingInvs.remove(inv);
        completedInvs.add(inv);

        latestCompletedCount++;
    }

    /**
     * Determines if pool is doing a cleaning cycle
     * @return true if so, otherwise false
     */
    public boolean isCleaning() { return CLEANING.equals(runningState); }

    /**
     * Determines if pool is doing an halting cycle
     * @return true if so, otherwise false
     */
    public boolean isHalting() { return HALTING.equals(runningState); }

    /**
     * Determines if pool is doing a running cycle
     * @return true if so, otherwise false
     */
    public boolean isRunning() { return RUNNING.equals(runningState); }

    public synchronized void startRunning() {
        runningState = RUNNING;
    }

    public synchronized boolean startCleaning() {

        if (!runningState.equals(RUNNING)) {
            Logger.warn("cannot start cleaning from a non running state. Make sure pool is in running state before starting to clean");
            return false;
        }

        runningState = CLEANING;

        return true;
    }

    public synchronized boolean startHalting() {

        if (runningState.equals(RUNNING)) {
            Logger.warn("cannot start halting from a running state. Start cleaning first");
            return false;
        }


        runningState = HALTING;

        return true;
    }

    /**
     * If we caught a successful broadcast during the cleaning cycle, we need to
     * restart digest since this broadcasts can alter the remaining cycles
     *
     * @param statement statement who could prevent removals
     * @return true if prevented, otherwise, false.
     */
    public synchronized boolean preventCleaning(Statement statement) {
        if (statement == null) {
            throw new IllegalArgumentException("Statement is required");
        }
        if (!isIngesting) {
            throw new IllegalArgumentException("Can't prevent cleaning cycle outside of a digest cycle");
        }

        if (!runningState.equals(CLEANING)) {
            return false;
        }

        if (statement.getState() != StatementStatus.SUCCESSFUL) {
            return false;
        }

        if (!(statement instanceof BroadcastStatement)) return false;

        startRunning();
        return true;
    }

    /**
     * Defines if there is any remaining inv to process
     *
     * @return boolean value indicating if there is any remaining (true) or not (false)
     */
    public boolean isEmpty() {
        return remainingInvs.isEmpty();
    }

    /**
     * Sort the remaining invs based on the amount of cleaned statements and
     * their pop and tail configurations
     *
     * @return A new list with the sorted invs
     */
    List<Inv> sortRemainings() {
        List<Inv> sorted = new ArrayList<>(remainingInvs);
        sorted.sort(Comparator.comparing(a -> a.getDigestionSummary().getCleaned() == null ?
                0 :
                a.getDigestionSummary().getCleaned().size()));
        sorted.sort((a, b) -> {
            if (!a.isPop().equals(b.isPop()))
                return b.isPop().compareTo(a.isPop());

            return a.isTail().compareTo(b.isTail());
        });

        return sorted;
    }
}
