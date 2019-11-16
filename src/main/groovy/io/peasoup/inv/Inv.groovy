package io.peasoup.inv

class Inv {

    String name
    String path
    Closure ready

    boolean sync = true

    final InvDescriptor delegate = new InvDescriptor()

    final List<NetworkValuable> remainingValuables = [].asSynchronized()
    final List<NetworkValuable> totalValuables = [].asSynchronized()

    final List<Closure> steps = [].asSynchronized()

    synchronized boolean dumpDelegate() {
        if (!name)
            name = delegate.name

        if (!path)
            path = delegate.path

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

    /**
     * Digest this inv.
     * This method is meant to be called during a digest cycle of the pool.
     * This method is also meant to be called synchronously.
     *
     * It allows to match the remaining network valuables.
     * Steps are also handled here.
     *
     *
     * @param pool the pool currently in digestion
     * @return
     */
    synchronized List<RequireValuable> digest(NetworkValuablePool pool ) {

        assert pool
        assert pool.isDigesting()

        List<RequireValuable> toResolve = []

        // Loop because dump
        while (true) {

            boolean hasResolvedSomething = false
            List toRemove = []

            // Use fori-loop for speed
            for (int j = 0; j < this.remainingValuables.size(); j++) {
                def networkValuable = this.remainingValuables[j]

                networkValuable.match.manage(pool, networkValuable)
                int result = networkValuable.match_state

                if (result == NetworkValuable.FAILED) {
                    if (sync) {
                        break
                    } else {
                        continue
                    }
                }

                toRemove << networkValuable

                if (result >= NetworkValuable.SUCCESSFUL) {

                    // Resolved requirements later to make sure broadcasts are available
                    if (networkValuable.match == NetworkValuable.REQUIRE) {
                        toResolve << networkValuable
                    }

                    // If we caught a successful broadcast during the unbloating cycle, we need to
                    // restart digest since this broadcasts can altered the remaining cycles
                    if (pool.stopUnbloating(networkValuable))
                        break
                }
            }

            // Remove all NV meant to be deleted
            this.remainingValuables.removeAll(toRemove)

            boolean hasDumpedSomething = false

            // Check for new steps if :
            // 1. has a remaining step
            // 2. has nothing to resolve (might glitch because of $into)
            // 3. has not (previously dumped something)
            // 4. has no more valuables
            while (!steps.isEmpty() &&
                   toResolve.isEmpty() &&
                   !hasDumpedSomething &&
                   this.remainingValuables.isEmpty()) {
                // Call next step
                steps.pop().call()

                // If the step dumped something, remainingValuables won't be empty and exit loop
                hasDumpedSomething = dumpDelegate()
            }


            // Check for new dumps
            if (!hasDumpedSomething)
                break
        }

        return toResolve
    }

}
