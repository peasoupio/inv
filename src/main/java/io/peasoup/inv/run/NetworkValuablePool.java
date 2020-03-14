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
    private final Set<Inv> remainingInvs = ConcurrentHashMap.newKeySet();
    private final Set<Inv> totalInvs = ConcurrentHashMap.newKeySet();
    protected volatile String runningState = RUNNING;
    private volatile boolean isDigesting = false;

    private ExecutorService invExecutor;
    private CompletionService<EatenInv> invCompletionService;

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
     * @param cycleDigestion Cycle digestion
     * @return Pool errors
     */
    private Queue<PoolReport.PoolError> eatSynchronized(List<Inv> invs, Inv.Digestion cycleDigestion) {

        final BlockingDeque<PoolReport.PoolError> poolErrors = new LinkedBlockingDeque<>();

        // Use fori-loop for speed
        for (int i = 0; i < invs.size(); i++) {
            final Inv inv = invs.get(i);

            String stateBefore = runningState;
            Inv.Digestion invDigest = eatInv(inv, poolErrors).getDigestion();

            cycleDigestion.concat(invDigest);

            // If changed, quit
            if (!stateBefore.equals(runningState)) break;
        }

        // Check for broadcasts
        stageBroadcasts();

        return poolErrors;
    }

    /**
     * Eat "multithread-ly" INV collection
     * @param invs INV collection to eat
     * @param cycleDigestion Cycle digestion
     * @return Pool errors
     */
    private Queue<PoolReport.PoolError> eatMultithreaded(List<Inv> invs, Inv.Digestion cycleDigestion) {
        final BlockingDeque<PoolReport.PoolError> poolErrors = new LinkedBlockingDeque<>();

        // Create executor and completion service
        if (invExecutor == null) {
            invExecutor = Executors.newFixedThreadPool(4);
            invCompletionService = new ExecutorCompletionService<>(invExecutor);
        }

        // Initial queueing
        for (int i = 0; i < invs.size(); i++) {
            final Inv inv = invs.get(i);
            invCompletionService.submit(() -> eatInv(inv, poolErrors) );
        }

        // Define a new  dynamic stack (for reallocation)
        int taskRemaining = invs.size();

        // Until stack if not empty, keep processing
        while(taskRemaining > 0) {
            taskRemaining--;

            try {
                // Get next eaten INV
                EatenInv eatenInv = invCompletionService.take().get();

                // If error was caught, check next Inv
                if (eatenInv.hasError())
                    continue;

                // If INV could be resumed, do it right now
                if (eatenInv.couldResumeEatNow()) {
                    // Submit INV again
                    taskRemaining++;
                    invCompletionService.submit(() -> eatInv(eatenInv.getInv(), poolErrors) );

                    Logger.debug(eatenInv.getInv() + " eaten back now");
                }

                // Stage broadcasts
                if (eatenInv.getDigestion().getBroadcasts() > 0)
                    stageBroadcasts();

                // Concat digestion metrics
                cycleDigestion.concat(eatenInv.getDigestion());

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
    @SuppressWarnings("squid:S1181")
    private EatenInv eatInv(final Inv inv, final Queue<PoolReport.PoolError> poolErrors) {
        Inv.Digestion currentDigest = new Inv.Digestion();
        boolean hasError = false;

        try {
            currentDigest.concat(inv.digest());
        } catch (Throwable t) {
            poolErrors.add(new PoolReport.PoolError(inv, t));

            // Remove upon errors
            remainingInvs.remove(inv);

            // Set error tracker to true
            hasError = true;

            Logger.fail(inv + " caught an error. Report will be displayed on pool termination.");
        }

        return new EatenInv(inv, currentDigest, hasError);
    }

    /**
     * Batch and add staging broadcasts once to prevent double-broadcasts on the same digest.
     * @return true if a broadcast was digested, or false if none.
     */
    private synchronized boolean stageBroadcasts() {
        boolean hasStagedSomething = false;

        for (Map.Entry<String, Map<Object, BroadcastResponse>> statements : stagingStatements.entrySet()) {
            Map<Object, BroadcastResponse> inChannel = availableStatements.get(statements.getKey());
            Iterator<Map.Entry<Object, BroadcastResponse>> outChannel = statements.getValue().entrySet().iterator();

            Logger.debug("[POOL] available:" + availableStatements.get(statements.getKey()).size() + " " + ", staged:" + statements.getValue().size());

            while(outChannel.hasNext()) {
                hasStagedSomething = true;

                Map.Entry<Object,BroadcastResponse> response = outChannel.next();
                inChannel.putIfAbsent(response.getKey(), response.getValue());

                outChannel.remove();
            }
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

            // Has more steps, more whens or statements, it is not done yet
            if (!inv.isCompleted()) {
                continue;
            }

            invsDone.add(inv);

            Logger.debug("[POOL] inv: " + inv.getName() + " COMPLETED");
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

        Logger.debug("[POOL] closed: true");
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

    private class EatenInv {

        private final Inv inv;
        private final Inv.Digestion digestion;
        private final boolean hasError;

        private EatenInv(Inv inv, Inv.Digestion digestion, boolean hasError) {
            this.inv = inv;
            this.digestion = digestion;
            this.hasError = hasError;
        }

        /**
         * Determines if an INV could be eaten again right away in a same cycle
         * @return true if eatable again, otherwise false
         */
        public boolean couldResumeEatNow() {
            return !inv.isCompleted() && digestion.hasDoneSomething();
        }

        public Inv getInv() {
            return inv;
        }

        public Inv.Digestion getDigestion() {
            return digestion;
        }

        public boolean hasError() {
            return hasError;
        }
    }
}
