package io.peasoup.inv.run;

import org.apache.commons.lang.StringUtils;
import org.codehaus.groovy.runtime.StackTraceUtils;

import java.util.*;
import java.util.concurrent.*;

public class NetworkValuablePool {
    private static final String HALTING = "HALTED";
    private static final String UNBLOATING = "UNBLOATING";
    private static final String RUNNING = "RUNNING";
    private final Queue<String> names = new ConcurrentLinkedQueue<String>();
    private final Map<String, Map<Object, BroadcastResponse>> availableStatements = new ConcurrentHashMap<>(24, 0.9f, 1);
    private final Map<String, Map<Object, BroadcastResponse>> stagingStatements = new ConcurrentHashMap<>(24, 0.9f, 1);
    private final Map<String, Queue<Object>> unbloatedStatements = new ConcurrentHashMap<>(24, 0.9f, 1);
    private final Set<Inv> remainingInvs = new HashSet<>();
    private final Set<Inv> totalInvs = new HashSet<>();
    protected volatile String runningState = RUNNING;
    private volatile boolean isDigesting = false;
    private ExecutorService invExecutor;

    public static String getHALTING() {
        return HALTING;
    }

    public static String getUNBLOATING() {
        return UNBLOATING;
    }

    public static String getRUNNING() {
        return RUNNING;
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
            throw ex;
        } finally {
            isDigesting = false;
        }

    }

    private PoolReport proceedDigest() {

        // If running in halted mode, skip cycle
        if (runningState.equals(HALTING))
            return new PoolReport(
                    new ArrayList<>(),
                    new LinkedList<>(),
                    true);

        // Multithreading is allowed only in a RUNNING cycle
        if (invExecutor == null) invExecutor = Executors.newFixedThreadPool(4);

        // All digestions
        Inv.Digestion digestion = new Inv.Digestion();

        List<Inv> sorted = new ArrayList<>(remainingInvs);
        if (runningState.equals(UNBLOATING))
            sorted.sort(Comparator.comparing(o -> o.getDigestionSummary().getUnbloats()));

        List<Future<Inv.Digestion>> futures = new ArrayList<>();
        final BlockingDeque<PoolReport.PoolException> exceptions = new LinkedBlockingDeque<>();

        // Use fori-loop for speed
        for (int i = 0; i < sorted.size(); i++) {

            final Inv inv = sorted.get(i);

            // If is in RUNNING state, we are allowed to do parallel stuff.
            // Otherwise, we may change the sequence.
            Callable<Inv.Digestion> eat = () -> {
                Inv.Digestion currentDigest = null;

                try {
                    currentDigest = inv.digest();
                } catch (Exception ex) {
                    PoolReport.PoolException exception = new PoolReport.PoolException();
                    exception.setInv(inv);
                    exception.setException((Exception) StackTraceUtils.sanitize(ex));

                    exceptions.add(exception);

                    // issues:8
                    remainingInvs.remove(inv);
                } finally {
                    return currentDigest;
                }
            };

            if (runningState.equals(RUNNING)) {
                futures.add(invExecutor.submit(eat));
            } else {
                String stateBefore = runningState;

                try {
                    Inv.Digestion digestResult = eat.call();

                    if (digestResult == null)
                        continue;

                    digestion.concat(digestResult);
                } catch (Exception e) {
                    Logger.error(e);
                } finally {
                    // If changed, quit
                    if (!stateBefore.equals(runningState)) break;
                }
            }
        }

        // Wait for invs to be digested in parallel.
        if (!futures.isEmpty()) {
            for (Future<Inv.Digestion> future : futures) {
                try {
                    Inv.Digestion digestResult = future.get();

                    if (digestResult == null)
                        continue;

                    digestion.concat(digestResult);
                } catch (Exception e) {
                    Logger.error(e);
                }
            }
        }


        // Batch all require resolve at once
        boolean hasResolvedSomething = digestion.getRequires() != 0;

        // Batch and add staging broadcasts once to prevent double-broadcasts on the same digest
        boolean hasStagedSomething = false;
        for (Map.Entry<String, Map<Object, BroadcastResponse>> statements : stagingStatements.entrySet()) {
            availableStatements.get(statements.getKey()).putAll(statements.getValue());

            if (!statements.getValue().isEmpty()) hasStagedSomething = true;

            statements.getValue().clear();
        }

        // Check for new dumps
        List<Inv> invsDone = new ArrayList<>();
        for (Inv inv : remainingInvs) {

            if (!inv.getSteps().isEmpty()) continue;

            if (!inv.getRemainingStatements().isEmpty()) continue;

            invsDone.add(inv);
        }

        remainingInvs.removeAll(invsDone);

        if (!invsDone.isEmpty()) {// Has completed Invs
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

        return new PoolReport(invsDone, exceptions, runningState.equals(HALTING));
    }

    public boolean process(Inv inv) {
        assert inv != null : "Inv is required";

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
        assert StringUtils.isNotEmpty(name) : "Name is required";

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
     * @param statement
     * @return
     */
    public synchronized boolean preventUnbloating(Statement statement) {
        assert statement != null : "Statement is required";
        assert isDigesting : "Can't prevent unbloating outside a digest cycle";

        if (!runningState.equals(UNBLOATING)) {
            return false;
        }


        if (statement.getState() != Statement.SUCCESSFUL) {
            return false;
        }


        if (!(statement instanceof BroadcastStatement)) return false;

        startRunning();
        return true;
    }

    /**
     * Shutting down any remaining tasks in the pool
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
