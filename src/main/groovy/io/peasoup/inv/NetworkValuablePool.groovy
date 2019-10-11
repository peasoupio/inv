package io.peasoup.inv

import java.util.concurrent.ConcurrentHashMap

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

    List<Inv> digest() {

        List<Inv> invsDone = []
        Collection<NetworkValuable> toResolve = [].asSynchronized()

        // Use fori-loop for speed
        for (int i = 0; i < remainingsInv.size() ; i++) {

            def inv = remainingsInv[i]


            // Loop because dump
            while(true) {

                Collection<NetworkValuable> remainingValuables = inv.remainingValuables.collect()
                boolean hasResolvedSomething = false

                // Use fori-loop for speed
                for (int j = 0; j < remainingValuables.size(); j++) {
                    def networkValuable = remainingValuables[j]

                    def result = networkValuable.match.manage(this, networkValuable)

                    if (result == NetworkValuable.FAILED) {
                        if (inv.sync) {
                            break
                        } else {
                            continue
                        }
                    }

                    inv.remainingValuables.remove(networkValuable)

                    if (result == NetworkValuable.SUCCESSFUL) {

                        hasResolvedSomething = true

                        // Resolved requirements later to make sure broadcasts are available
                        if (networkValuable.match == NetworkValuable.REQUIRE) {
                            toResolve << networkValuable
                        }

                        // If we caught a successful matching while in unbloating state, we need to skip since
                        // and restart digest since this inv could unjammed something else
                        if (runningState == UNBLOATING) {
                            runningState = RUNNING
                            break
                        }
                    }
                }

                // Check for new steps
                if (inv.remainingValuables.isEmpty() && // has no more valuables -and-
                    !hasResolvedSomething &&            // did not just resolved something (waiting for resolve) -and-
                    !inv.steps.isEmpty()) {             // has a remaining step
                    inv.steps.removeAt(0).call()
                }

                // Check for new dumps
                if (!inv.dumpDelegate())
                    break
            }
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
        /*
        boolean hasDumpedSomething = false
        for (int i = 0; i < remainingsInv.size() ; i++) {
            def inv = remainingsInv[i]

            if (inv.dumpDelegate())
                hasDumpedSomething = true

            if (!inv.steps.isEmpty())
                continue

            if (!inv.remainingValuables.isEmpty())
                continue

            invsDone << inv
        }
        */
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
