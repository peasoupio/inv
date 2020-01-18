package io.peasoup.inv

import groovy.transform.CompileStatic

import java.util.concurrent.*

@CompileStatic
class NetworkValuablePool {

    final static String HALTING = "HALTED",
                        UNBLOATING = "UNBLOATING",
                        RUNNING = "RUNNING"

    final Queue<String> names = new ConcurrentLinkedQueue<>()
    final Map<String, Map<Object, BroadcastStatement.Response>> availableStatements = new ConcurrentHashMap<>(24, 0.9f, 1)
    final Map<String, Map<Object, BroadcastStatement.Response>> stagingStatements = new ConcurrentHashMap<>(24, 0.9f, 1)
    final Map<String, Queue<Object>> unbloatedStatements = new ConcurrentHashMap<>(24, 0.9f, 1)

    final Set<Inv> remainingInvs = new HashSet<>()
    final Set<Inv> totalInvs = new HashSet<>()

    volatile protected String runningState = RUNNING
    volatile boolean isDigesting = false

    private ExecutorService invExecutor

    /**
     * Digest invs and theirs statements.
     * This is the main function to resolve requirements and broadcasts within the current pool.
     *
     * Invs and statements are pushed through different cycles : RUNNING, UNBLOATING and HALTED.
     * Cycles can be altered based on how well the digestion goes.
     *
     * Invs resolution process is multithreaded during the RUNNING cycle only.
     * Within the resolution, statements are processed.
     * During this step, the statements (require and broadcast) are matched.
     * NOTE : If running in an UNBLOATING cycle, requires "unresolved" could be raised if marked unbloatable.
     *
     * During the UNBLOATING cycle, we removed/do not resolve unbloatable requirements to unbloat the pool.
     * We wait until we catch a broadcast since a broadcast could alter the remaining statements within the pool.
     * In this case, when reached, the current cycle is close and a new one is meant to called as RUNNING.
     *
     * If nothing is broadcast during a UNBLOATING cycle, next one will be called as HALTED.
     * During the HALTED cycle, even a broadcast cannot reset the digestion cycles to running.
     * The pool is meant to be closed and restarted manually.
     *
     * @return list of inv completed during this digestion cycle
     */
    PoolReport digest() {

        // If running in halted mode, skip cycle
        if (runningState == HALTING) {
            return new PoolReport()
        }

        // Multithreading is allowed only in a RUNNING cycle
        if (!invExecutor)
            invExecutor = Executors.newFixedThreadPool(4)

        isDigesting = true

        // All digestions
        def digestion = new Inv.Digestion()

        def sorted = remainingInvs
        if (runningState == UNBLOATING)
            sorted = remainingInvs.sort { a, b ->
                a.digestionSummary.unbloats.compareTo(b.digestionSummary.unbloats)
            }

        List<Future<Inv.Digestion>> futures = []
        BlockingDeque<PoolReport.PoolException> exceptions = new LinkedBlockingDeque<>()

        // Use fori-loop for speed
        for (int i = 0; i < sorted.size() ; i++) {

            def inv = sorted[i]

            // If is in RUNNING state, we are allowed to do parallel stuff.
            // Otherwise, we may change the sequence.
            def eat = ({
                try {
                    return inv.digest()
                } catch (Exception ex) {
                    exceptions.add(new PoolReport.PoolException(inv: inv, exception: ex))

                    // issues:8
                    remainingInvs.remove(inv)
                }

                return []
            } as Callable<Inv.Digestion>)


            if (runningState == RUNNING) {
                futures.add(invExecutor.submit(eat))
            } else {
                def currentState = runningState
                digestion.concat(eat())

                // If changed, quit
                if (currentState != runningState)
                    break
            }
        }

        // Wait for invs to be digested in parallel.
        if (!futures.isEmpty()) {
            futures.each {
                digestion.concat(it.get())
            }
        }



        // Batch all require resolve at once
        boolean hasResolvedSomething = digestion.requires != 0

        // Batch and add staging broadcasts once to prevent double-broadcasts on the same digest
        boolean hasStagedSomething = false
        def stagingSet = stagingStatements.entrySet()

        for (int i = 0; i < stagingStatements.size(); i++) {
            def statements = stagingSet[i]
            availableStatements[statements.key].putAll(statements.value)

            if (statements.value.size())
                hasStagedSomething = true

            statements.value.clear()
        }

        // Check for new dumps
        List<Inv> invsDone = []
        for (int i = 0; i < remainingInvs.size() ; i++) {
            def inv = remainingInvs[i]

            if (!inv.steps.isEmpty())
                continue

            if (!inv.remainingStatements.isEmpty())
                continue

            invsDone << inv
        }
        remainingInvs.removeAll(invsDone)

        if (!invsDone.isEmpty()) { // Has completed Invs
            startRunning()
        } else if (hasResolvedSomething) { // Has completed requirements
            startRunning()
        } else if (hasStagedSomething) { // Has dumped something
            startRunning()
        } else if (runningState == UNBLOATING) {
            Logger.info "nothing unbloated"
            startHalting() // Has already start unbloating, but did not do anything ? Halt pool
        } else {
            Logger.info "nothing done"
            startUnbloating() // Should start unbloating
        }

        // Update validation flag for ins
        isDigesting = false

        return new PoolReport(
                invsDone,
                exceptions,
                runningState == HALTING
        )
    }

    /**
     * Check if a name is already available within the pool.
     * If not, make it available.
     * @param name string value of the name to check
     */
    void checkAvailability(String name) {
        assert name, 'Name is required'

        if (names.contains(name))
            return

        synchronized (names) {
            // double lock checking
            if (names.contains(name))
                return

            names << name
            availableStatements.put(name, new ConcurrentHashMap<Object, BroadcastStatement.Response>(24, 0.9f, 1))
            stagingStatements.put(name, new ConcurrentHashMap<Object, BroadcastStatement.Response>(24, 0.9f, 1))
            unbloatedStatements.put(name, new ConcurrentLinkedQueue<Object>())
        }
    }

    synchronized void startRunning() {
        runningState = RUNNING
    }

    synchronized boolean startUnbloating() {

        if (runningState != RUNNING) {
            Logger.warn "cannot start unbloating from a non running state. Make sure pool is in running state before starting to unbloat"
            return false
        }

        runningState = UNBLOATING

        return true
    }

    synchronized boolean startHalting() {

        if (runningState == RUNNING) {
            Logger.warn "cannot start halting from a running state. Start unbloating first"
            return false
        }

        runningState = HALTING

        return true
    }

    // TODO Should this code belon to BroadcastStatement? Seems it would make it safer
    /**
     * If we caught a successful broadcast during the unbloating cycle, we need to
     * restart digest since this broadcasts can altered the remaining cycles
     * @param statement
     * @return
     */
    synchronized boolean preventUnbloating(Statement statement) {
        assert statement, 'Statement is required'
        assert isDigesting, "Can't prevent unbloating outside a digest cycle"

        if (runningState != UNBLOATING) {
            return false
        }

        if (statement.state != Statement.SUCCESSFUL) {
            return false
        }

        if (!(statement instanceof BroadcastStatement))
            return false

        startRunning()
        return true
    }

    /**
     * Shutting down any remaining tasks in the pool
     */
    boolean shutdown() {
        if (invExecutor) {
            Logger.debug "executor is shutting down"
            invExecutor.shutdownNow()
            invExecutor = null

            return true
        }

        return false
    }

    /**
     * Defines if there is any remaining inv to process
     * @return boolean value indicating if there is any remaining (true) or not (false)
     */
    boolean isEmpty() {
        return remainingInvs.isEmpty()
    }

    /**
     * Gets the current state
     * @return the string value of the current state
     */
    String runningState() {
        return this.runningState
    }

    /**
     * Check if the pool is currently digesting
     * @return the boolean value indicating if the pool is digesting (true) or not (false).
     */
    boolean isDigesting() {
        return this.isDigesting
    }
}
