package io.peasoup.inv

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

class NetworkValuablePool {


    final static String HALTED = "HALTED",
                     UNBLOATING = "UNBLOATING",
                     RUNNING = "RUNNING"

    String runningState = RUNNING

    Set<String> names = []

    Map<String, Map<Object, Object>> availableValuables = [:]
    Map<String, Map<Object, Object>> stagingValuables = [:]

    List<Inv> remainingsInv = [].asSynchronized()
    List<Inv> totalInv = [].asSynchronized()

    ExecutorService invExecutor

    List<Inv> digest() {

        List<Inv> invsDone = []
        Collection<NetworkValuable> toResolve = [].asSynchronized()

        invExecutor = Executors.newFixedThreadPool(4)
        List<Future> futures = []

        // Use fori-loop for speed
        for (int i = 0; i < remainingsInv.size() ; i++) {

            def inv = remainingsInv[i]

            // If is in RUNNING state, we are allowed to do parallel stuff.
            // Otherwise, we may change the sequence.
            if (runningState == RUNNING) {
                futures << invExecutor.submit({->
                    toResolve += inv.digest(this)
                })
            } else {
                // If so, execute right now
                toResolve += inv.digest(this)
            }
        }

        // Wait for invs to be digested in parallel.
        if (!futures.isEmpty()) {
            futures.each { it.get() }
        }

        // If running in halted mode, no need to broadcasts
        // It is only required to unresolved requirements.
        if (runningState == HALTED) {
            return null
        }

        // Batch all require resolve at once
        boolean hasResolvedSomething = false
        for (int i = 0; i < toResolve.size(); i++) {
            NetworkValuable networkValuable = toResolve[i]

            def broadcast = availableValuables[networkValuable.name][networkValuable.id]

            // Sends message to resolved (if defined)
            if (networkValuable.resolved) {
                networkValuable.resolved.delegate = broadcast
                networkValuable.resolved()

                // Check if NV would have dumped something
                networkValuable.inv.dumpDelegate()
            }

            hasResolvedSomething = true
        }

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

        if (hasDoneSomething) { // Has completed Invs
            runningState = RUNNING
        } else if (hasResolvedSomething) { // Has completed requirements
            runningState = RUNNING
        } else if (hasStagedSomething) { // Has dumped something
            runningState = RUNNING
        } else if (runningState == UNBLOATING) {
            Logger.info "nothing unbloated"
            runningState = HALTED // Has already start unbloating, but did not do anything ? Halt pool
        } else {
            Logger.info "nothing done"
            runningState = UNBLOATING // Should start unbloating
        }

        // Clear executor
        invExecutor.shutdown()

        return invsDone
    }

    void checkAvailability(String name) {
        if (names.contains(name))
            return

        names << name
        availableValuables.put(name, new ConcurrentHashMap<Object, Object>())
        stagingValuables.put(name, new ConcurrentHashMap<Object, Object>())
    }


    boolean isEmpty() {
        return remainingsInv.isEmpty()
    }

}
