package io.peasoup.inv.run;

import org.apache.commons.lang.StringUtils;

import java.util.*;
import java.util.concurrent.*;

public class NetworkValuablePool {
    private static final String HALTING = "HALTED";
    private static final String UNBLOATING = "UNBLOATING";
    private static final String RUNNING = "RUNNING";
    private final Queue<String> names = new ConcurrentLinkedQueue<>();
    private final Map<String, Map<Object, BroadcastResponse>> availableStatements = new ConcurrentHashMap<>(24, 0.9f, 1);
    private final Map<String, Map<Object, BroadcastResponse>> stagingStatements = new ConcurrentHashMap<>(24, 0.9f, 1);
    private final Map<String, Queue<Object>> unbloatedStatements = new ConcurrentHashMap<>(24, 0.9f, 1);
    private final Set<Inv> remainingInvs = new HashSet<>();
    private final Set<Inv> totalInvs = new HashSet<>();
    protected volatile String runningState = RUNNING;
    private volatile boolean isDigesting = false;
    private ExecutorService invExecutor;

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
        List<Inv> sorted = sortRemainingInvs();

        // Eat invs
        Queue<PoolReport.PoolError> errorsCaught;

        // Eat invs
        if (isRunning())
            errorsCaught = eatSynchronized(sorted, digestion);
        else
            errorsCaught = eatMultithreaded(sorted, digestion);

        // Batch all require resolve at once
        boolean hasResolvedSomething = digestion.getRequires() != 0;

        // Check for new broadcasts
        boolean hasStagedSomething = stageBroadcasts();

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

    private List<Inv> sortRemainingInvs() {
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
     * Eat synchronously INV collection
     * @param invs INV collection to eat
     * @param currentDigestion Current digestion
     * @return Pool errors
     */
    private Queue<PoolReport.PoolError> eatSynchronized(List<Inv> invs, Inv.Digestion currentDigestion) {

        final BlockingDeque<PoolReport.PoolError> poolErrors = new LinkedBlockingDeque<>();

        // Use fori-loop for speed
        for (int i = 0; i < invs.size(); i++) {
            final Inv inv = invs.get(i);

            String stateBefore = runningState;
            currentDigestion.concat(eatInv(inv, poolErrors));

            // If changed, quit
            if (!stateBefore.equals(runningState)) break;
        }

        return poolErrors;
    }

    /**
     * Eat "multithread-ly" INV collection
     * @param invs INV collection to eat
     * @param currentDigestion Current digestion
     * @return Pool errors
     */
    private Queue<PoolReport.PoolError> eatMultithreaded(List<Inv> invs, Inv.Digestion currentDigestion) {
        List<Future<Inv.Digestion>> futures = new ArrayList<>();
        final BlockingDeque<PoolReport.PoolError> poolErrors = new LinkedBlockingDeque<>();

        // Multithreading is allowed only in a RUNNING cycle
        if (invExecutor == null) invExecutor = Executors.newFixedThreadPool(4);

        // Use fori-loop for speed
        for (int i = 0; i < invs.size(); i++) {
            final Inv inv = invs.get(i);
            futures.add(invExecutor.submit(() -> eatInv(inv, poolErrors) ));
        }

        // Wait for invs to be digested in parallel.
        for (Future<Inv.Digestion> future : futures) {
            try {
                currentDigestion.concat(future.get());
            } catch (Exception e) {
                Logger.error(e);
            }
        }

        return poolErrors;
    }

    /**
     * Do the actual "eating" for a single INV
     * @param inv The INV to eat
     * @param poolErrors Pool errors collection
     * @return A new digestion for this specific INV
     */
    private Inv.Digestion eatInv(final Inv inv, final Queue<PoolReport.PoolError> poolErrors) {
        Inv.Digestion currentDigest = null;

        try {
            currentDigest = inv.digest();
        } catch (Throwable t) {
            PoolReport.PoolError exception = new PoolReport.PoolError();
            exception.setInv(inv);
            exception.setThrowable(t);

            poolErrors.add(exception);

            // issues:8
            remainingInvs.remove(inv);
        }

        return currentDigest;
    }

    /**
     * Batch and add staging broadcasts once to prevent double-broadcasts on the same digest.
     * @return true if a broadcast was digested, or false if none.
     */
    private boolean stageBroadcasts() {
        boolean hasStagedSomething = false;

        for (Map.Entry<String, Map<Object, BroadcastResponse>> statements : stagingStatements.entrySet()) {
            availableStatements.get(statements.getKey()).putAll(statements.getValue());

            if (!statements.getValue().isEmpty()) hasStagedSomething = true;

            statements.getValue().clear();
        }

        return hasStagedSomething;
    }

    /**
     * Determine and remove completed invs from the remaining ones
     * @return a list with the removed invs
     */
    private List<Inv> cleanCompletedInvs() {
        List<Inv> invsDone = new ArrayList<>();
        for (Inv inv : remainingInvs) {

            // Has more steps or statements, it is not done yet
            if (!inv.getSteps().isEmpty() ||
                !inv.getRemainingStatements().isEmpty()) continue;

            invsDone.add(inv);
        }

        remainingInvs.removeAll(invsDone);

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
     * Shutting down any remaining tasks in the pool
     *
     * @return true if shutdown, otherwise false.
     */
    public boolean shutdown() {
        if (invExecutor == null) return false;

        Logger.debug("executor is shutting down");
        invExecutor.shutdownNow();
        invExecutor = null;

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
