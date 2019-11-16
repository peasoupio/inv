package io.peasoup.inv

import java.util.concurrent.*

class NetworkValuablePool {


    final static String HALTING = "HALTED",
                        UNBLOATING = "UNBLOATING",
                        RUNNING = "RUNNING"



    final Set<String> names = []

    final Map<String, Map<Object, BroadcastValuable.Response>> availableValuables = [:]
    final Map<String, Map<Object, BroadcastValuable.Response>> stagingValuables = [:]

    final List<Inv> remainingsInv = [].asSynchronized()
    final List<Inv> totalInv = [].asSynchronized()

    private String runningState = RUNNING
    private boolean isDigesting = false

    private ExecutorService invExecutor



    /**
     * Digest invs and their network valuables.
     * This is the main function to resolve requirements and broadcasts within the current pool.
     *
     * Invs and network valuables are pushed through different cycles : RUNNING, UNBLOATING and HALTED.
     * Cycles can be altered based on how well the digestion goes.
     *
     * Invs resolution process is multithreaded during the RUNNING cycle only.
     * Within the resolution, network valuables are processed.
     * During this step, the network valuables (require and broadcast) are matched.
     * NOTE : If running in an UNBLOATING cycle, requires "unresolved" could be raised if marked unbloatable.
     *
     * During the UNBLOATING cycle, we removed/do not resolve unbloatable requirements to unbloat the pool.
     * We wait until we catch a broadcast since a broadcast could alter the remaining network valuables within the pool.
     * In this case, when reached, the current cycle is close and a new one is meant to called as RUNNING.
     *
     * If nothing is broadcast during a UNBLOATING cycle, next one will be called as HALTED.
     * During the HALTED cycle, even a broadcast cannot reset the digestion cycles to running.
     * The pool is meant to be closed and restarted manually.
     *
     * @return list of inv completed during this digestion cycle
     */
    List<Inv> digest() {

        // Multithreading is allowed only in a RUNNING cycle
        if (!invExecutor)
            invExecutor = Executors.newFixedThreadPool(4)

        isDigesting = true

        List<Inv> invsDone = []
        List<RequireValuable> toResolve = []

        List<Future> futures = []

        // Use fori-loop for speed
        for (int i = 0; i < remainingsInv.size() ; i++) {

            def inv = remainingsInv[i]

            // If is in RUNNING state, we are allowed to do parallel stuff.
            // Otherwise, we may change the sequence.
            if (runningState == RUNNING) {

                def pool = this

                futures << invExecutor.submit( {
                    return inv.digest(pool)
                } as Callable )
            } else {
                // If so, execute right now
                toResolve += inv.digest(this)
            }
        }

        // Wait for invs to be digested in parallel.
        if (!futures.isEmpty()) {
            futures.each {
                toResolve += it.get()
            }
        }

        // If running in halted mode, no need to broadcasts
        // It is only required to unresolved requirements.
        if (runningState == HALTING) {
            return []
        }

        // Batch all require resolve at once
        boolean hasResolvedSomething = !toResolve.isEmpty()

        // Batch and add staging broadcasts once to prevent double-broadcasts on the same digest
        boolean hasStagedSomething = false
        def stagingSet = stagingValuables.entrySet()
        for (int i = 0; i < stagingValuables.size(); i++) {
            def networkValuables = stagingSet[i]
            availableValuables[networkValuables.key].putAll(networkValuables.value)

            if (networkValuables.value.size())
                hasStagedSomething = true

            networkValuables.value.clear()
        }

        // Check for new dumps
        boolean hasDoneSomething = false
        for (int i = 0; i < remainingsInv.size() ; i++) {
            def inv = remainingsInv[i]

            if (!inv.steps.isEmpty())
                continue

            if (!inv.remainingValuables.isEmpty())
                continue

            invsDone << inv

            hasDoneSomething = true
        }

        remainingsInv.removeAll(invsDone)

        Logger.debug "Has done something: ${hasDoneSomething}"
        Logger.debug "Has resolved something: ${hasResolvedSomething}"
        Logger.debug "Has staged something: ${hasStagedSomething}"

        if (hasDoneSomething) { // Has completed Invs
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

        return invsDone
    }

    /**
     * Check if a name is already available within the pool.
     * If not, make it available.
     * @param name string value of the name to check
     */
    void checkAvailability(String name) {

        assert name

        if (names.contains(name))
            return

        names << name
        availableValuables.put(name, new ConcurrentHashMap<Object, Object>())
        stagingValuables.put(name, new ConcurrentHashMap<Object, Object>())
    }

    synchronized void startRunning() {
        runningState = RUNNING
    }

    synchronized void startUnbloating() {

        if (runningState != RUNNING) {
            Logger.warn "cannot start unbloating from a non running state. Make sure pool is in running state before starting to unbloat"
            return
        }

        runningState = UNBLOATING
    }

    synchronized void startHalting() {

        if (runningState == RUNNING) {
            Logger.warn "cannot start halting from a running state. Start unbloating first"
            return
        }

        runningState = HALTING
    }

    synchronized boolean stopUnbloating(NetworkValuable networkValuable) {

        assert networkValuable
        assert isDigesting

        if (runningState != UNBLOATING ||
            networkValuable.match != NetworkValuable.BROADCAST) {
            return false
        }

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
        return remainingsInv.isEmpty()
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
