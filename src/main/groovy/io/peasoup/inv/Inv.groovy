package io.peasoup.inv

class Inv {

    String name
    Closure ready

    boolean sync = true

    final InvDelegate delegate = new InvDelegate()

    final List<NetworkValuable> remainingValuables = [].asSynchronized()
    final List<NetworkValuable> totalValuables = [].asSynchronized()

    final List<Closure> steps = [].asSynchronized()

    boolean dumpDelegate() {
        if (!name)
            name = delegate.name

        if (!ready)
            ready = delegate.ready

        def dumpedSomething = false

        if (!delegate.steps.isEmpty()) {
            steps.addAll(delegate.steps)
            dumpedSomething = true
        }

        // use for-loop to keep order
        for(NetworkValuable networkValuable : delegate.networkValuables) {

            dumpedSomething = true

            networkValuable.inv = this

            this.totalValuables << networkValuable
            this.remainingValuables << networkValuable
        }

        delegate.ready = null
        delegate.steps.clear()
        delegate.networkValuables.clear()

        return dumpedSomething
    }

    List<NetworkValuable> digest(NetworkValuablePool pool ) {

        List<NetworkValuable> toResolve = []

        // Loop because dump
        while (true) {

            Collection<NetworkValuable> remainingValuablesRightNow = remainingValuables.collect()
            boolean hasResolvedSomething = false

            // Use fori-loop for speed
            for (int j = 0; j < remainingValuablesRightNow.size(); j++) {
                def networkValuable = remainingValuablesRightNow[j]

                networkValuable.match.manage(pool, networkValuable)
                int result = networkValuable.match_state

                if (result == NetworkValuable.FAILED) {
                    if (sync) {
                        break
                    } else {
                        continue
                    }
                }

                this.remainingValuables.remove(networkValuable)

                if (result >= NetworkValuable.SUCCESSFUL) {

                    hasResolvedSomething = true

                    // Resolved requirements later to make sure broadcasts are available
                    if (networkValuable.match == NetworkValuable.REQUIRE) {
                        toResolve << networkValuable
                    }

                    // If we caught a successful matching while in unbloating state, we need to skip since
                    // and restart digest since this inv could unjammed something else
                    if (pool.runningState == pool.UNBLOATING) {
                        pool.runningState = pool.RUNNING
                        break
                    }
                }
            }

            // Check for new steps
            if (remainingValuablesRightNow.isEmpty() && // has no more valuables -and-
                    !hasResolvedSomething &&            // did not just resolved something (waiting for resolve) -and-
                    !steps.isEmpty()) {             // has a remaining step
                steps.removeAt(0).call()
            }

            // Check for new dumps
            if (!dumpDelegate())
                break
        }

        return toResolve
    }

}
