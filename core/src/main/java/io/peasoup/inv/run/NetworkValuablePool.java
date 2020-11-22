package io.peasoup.inv.run;

import io.peasoup.inv.Logger;
import org.apache.commons.lang.StringUtils;

import java.util.*;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingDeque;

public class NetworkValuablePool {
    public static final String HALTING = "HALTED";
    public static final String UNBLOATING = "UNBLOATING";
    public static final String RUNNING = "RUNNING";

    private final Queue<String> names = new ConcurrentLinkedQueue<>();
    private final Map<String, Map<Object, BroadcastResponse>> availableStatements = new ConcurrentHashMap<>(24, 0.9f, 1);
    private final Map<String, Map<Object, BroadcastResponse>> stagingStatements = new ConcurrentHashMap<>(24, 0.9f, 1);
    private final Map<String, Queue<Object>> unbloatedStatements = new ConcurrentHashMap<>(24, 0.9f, 1);
    private final Set<Inv> remainingInvs = ConcurrentHashMap.newKeySet();
    private final Set<Inv> completedInvs = ConcurrentHashMap.newKeySet();
    private final Set<Inv> totalInvs = ConcurrentHashMap.newKeySet();

    private final NetworkValuablePoolEater eater;

    protected volatile String runningState;
    private volatile boolean isDigesting;

    public NetworkValuablePool() {
        this.eater = new NetworkValuablePoolEater(this);
        this.runningState  = RUNNING;
        this.isDigesting = false;
    }

    /**
     * Digest invs and theirs statements.
     * This is the main function to resolve requirements and broadcasts within the current pool.
     * <p>
     * Invs and statements are pushed through different cycles : RUNNING, UNBLOATING and HALTED.
     * Cycles can be altered based on how well the digestion goes.
     * <p>
     * Invs resolution process is multithreaded during the RUNNING cycle only.
     * Within the resolution, statements are processed.
     * During this step, the statements (require and broadcast) are matched.
     * NOTE : If running in an UNBLOATING cycle, requires "unresolved" could be raised if marked unbloatable.
     * <p>
     * During the UNBLOATING cycle, we removed/do not resolve unbloatable requirements to unbloat the pool.
     * We wait until we catch a broadcast since a broadcast could alter the remaining statements within the pool.
     * In this case, when reached, the current cycle is close and a new one is meant to called as RUNNING.
     * <p>
     * If nothing is broadcast during a UNBLOATING cycle, next one will be called as HALTED.
     * During the HALTED cycle, even a broadcast cannot reset the digestion cycles to running.
     * The pool is meant to be closed and restarted manually.
     *
     * @return list of inv completed during this digestion cycle
     */
    public PoolReport digest() {
        isDigesting = true;
        try {
            return proceedDigest();
        } catch (Exception ex) {
            Logger.error(ex);
            throw ex;
        } finally {
            isDigesting = false;
        }

    }

    private PoolReport proceedDigest() {

        // If running in halted mode, skip cycle
        if (isHalting())
            return new PoolReport(
                    new ArrayList<>(),
                    new LinkedList<>(),
                    true);

        // All digestions
        Inv.Digestion digestion = new Inv.Digestion();

        // Get sorted INV
        List<Inv> sorted = sort();

        // Eat invs
        Queue<PoolReport.PoolError> errorsCaught = null;

        // Eat invs
        if (isRunning())
            errorsCaught = eatMultithreaded(sorted, digestion);

        if (isUnbloating())
            errorsCaught = eatSynchronized(sorted, digestion);

        // Batch all require resolve at once
        boolean hasResolvedSomething = digestion.getRequires() > 0;

        // Check for new broadcasts
        boolean hasStagedSomething = digestion.getBroadcasts() > 0;

        // Check for new dumps
        List<Inv> invsCompleted = cleanCompletedInvs();

        // Prepare state for next cycle
        evaluateState(
                !invsCompleted.isEmpty(),
                hasResolvedSomething,
                hasStagedSomething
        );

        return new PoolReport(invsCompleted, errorsCaught, runningState.equals(HALTING));
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
            Inv.Digestion invDigest = eater.eatInv(inv, poolErrors).getDigestion();

            cycleDigestion.concat(invDigest);

            // If changed, quit
            if (invDigest.isInterrupted() && !stateBefore.equals(runningState))
                break;
        }

        // Check for broadcasts
        eater.stageBroadcasts();

        // Print broadcasts
        eater.printStagedBroadcasts();

        return poolErrors;
    }

    /**
     * Eat "multithread-ly" INV collection
     * @param invs INV collection to eat
     * @param cycleDigestion Cycle digestion
     * @return Pool errors
     */
    private Queue<PoolReport.PoolError> eatMultithreaded(List<Inv> invs, final Inv.Digestion cycleDigestion) {
        BlockingDeque<PoolReport.PoolError> poolErrors = new LinkedBlockingDeque<>();

        // Execute INVs
        new NetworkValuablePoolExecutor(eater, invs, cycleDigestion, poolErrors).start();

        // Print broadcasts
        eater.printStagedBroadcasts();

        return poolErrors;
    }

    /**
     * Determine and remove completed invs from the remaining ones
     * @return a list with the removed invs
     */
    private List<Inv> cleanCompletedInvs() {
        List<Inv> invsDone = new ArrayList<>();
        for (Inv inv : remainingInvs) {

            // Has more steps, more whens or statements, it is not done yet
            if (!inv.isCompleted()) {
                continue;
            }

            invsDone.add(inv);

            Logger.system("[POOL] inv: " + inv.getName() + " COMPLETED");
        }

        remainingInvs.removeAll(invsDone);
        completedInvs.addAll(invsDone);

        return invsDone;
    }

    /**
     * Determine the next cycle state
     * @param hasDoneSomething invs were completed
     * @param hasResolvedSomething requires statements where met
     * @param hasStagedSomething broadcast statements where met
     */
    private void evaluateState(boolean hasDoneSomething, boolean hasResolvedSomething, boolean hasStagedSomething) {
        if (hasDoneSomething) {// Has completed Invs
            startRunning();
        } else if (hasResolvedSomething) {// Has completed requirements
            startRunning();
        } else if (hasStagedSomething) {// Has dumped something
            startRunning();
        } else if (runningState.equals(UNBLOATING)) {
            Logger.info("nothing unbloated");
            startHalting();// Has already start unbloating, but did not do anything ? Halt pool
        } else {
            Logger.info("nothing done");
            startUnbloating();// Should start unbloating
        }
    }

    public boolean include(Inv inv) {
        if (inv == null) {
            throw new IllegalArgumentException("Inv is required");
        }

        if (StringUtils.isEmpty(inv.getName()))
            return false;

        if (totalInvs.contains(inv))
            return false;

        totalInvs.add(inv);
        remainingInvs.add(inv);

        return true;
    }

    /**
     * Check if a name is already available within the pool.
     * If not, make it available.
     *
     * @param name string value of the name to check
     */
    public void checkAvailability(String name) {
        if (StringUtils.isEmpty(name)) {
            throw new IllegalArgumentException("Name is required");
        }

        if (names.contains(name)) return;

        synchronized (names) {
            // double lock checking
            if (names.contains(name)) return;

            names.add(name);
            availableStatements.put(name, new ConcurrentHashMap<>(24, 0.9f, 1));
            stagingStatements.put(name, new ConcurrentHashMap<>(24, 0.9f, 1));
            unbloatedStatements.put(name, new ConcurrentLinkedQueue<>());
        }
    }

    /**
     * Sort the remaining invs based on their pop and tail configuration
     * @return A new list with the sorted invs
     */
    List<Inv> sort() {
        List<Inv> sorted = new ArrayList<>(remainingInvs);
        sorted.sort(Comparator.comparing(a -> a.getDigestionSummary().getUnbloats()));
        sorted.sort((a, b) -> {

            if (a.isPop() == b.isPop() && a.isTail() == b.isTail())
                return 0;

            if (a.isPop())
                return -1;

            if (a.isTail())
                return 1;

            return 0;
        });

        return sorted;
    }

    /**
     * Determines if pool is doing an unbloating cycle
     * @return true if so, otherwise false
     */
    public boolean isUnbloating() { return UNBLOATING.equals(runningState); }

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

    public synchronized boolean startUnbloating() {

        if (!runningState.equals(RUNNING)) {
            Logger.warn("cannot start unbloating from a non running state. Make sure pool is in running state before starting to unbloat");
            return false;
        }


        runningState = UNBLOATING;

        return true;
    }

    public synchronized boolean startHalting() {

        if (runningState.equals(RUNNING)) {
            Logger.warn("cannot start halting from a running state. Start unbloating first");
            return false;
        }


        runningState = HALTING;

        return true;
    }

    /**
     * If we caught a successful broadcast during the unbloating cycle, we need to
     * restart digest since this broadcasts can altered the remaining cycles
     *
     * @param statement statement who could prevent unbloating
     * @return true if prevented, otherwise, false.
     */
    public synchronized boolean preventUnbloating(Statement statement) {
        if (statement == null) {
            throw new IllegalArgumentException("Statement is required");
        }
        if (!isDigesting) {
            throw new IllegalArgumentException("Can't prevent unbloating outside a digest cycle");
        }

        if (!runningState.equals(UNBLOATING)) {
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
     * Gets the current state
     *
     * @return the string value of the current state
     */
    public String runningState() {
        return this.runningState;
    }

    /**
     * Check if the pool is currently digesting
     *
     * @return the boolean value indicating if the pool is digesting (true) or not (false).
     */
    public boolean isDigesting() {
        return this.isDigesting;
    }

    public final Map<String, Map<Object, BroadcastResponse>> getAvailableStatements() {
        return availableStatements;
    }

    public final Map<String, Map<Object, BroadcastResponse>> getStagingStatements() {
        return stagingStatements;
    }

    public final Map<String, Queue<Object>> getUnbloatedStatements() {
        return unbloatedStatements;
    }

    public final Set<Inv> getRemainingInvs() {
        return remainingInvs;
    }

    public final Set<Inv> getCompletedInvs() {
        return completedInvs;
    }

    public final Set<Inv> getTotalInvs() {
        return totalInvs;
    }

    public boolean getIsDigesting() {
        return isDigesting;
    }

    public void setIsDigesting(boolean isDigesting) {
        this.isDigesting = isDigesting;
    }


}
