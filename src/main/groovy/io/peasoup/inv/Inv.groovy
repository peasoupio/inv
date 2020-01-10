package io.peasoup.inv

import groovy.transform.CompileStatic

@CompileStatic
class Inv {

    String name
    String path
    Closure ready

    final Digestion digestionSummary = new Digestion()

    final InvDescriptor delegate = new InvDescriptor()

    final List<Statement> remainingStatements = [].asSynchronized() as List<Statement>
    final List<Statement> totalStatements = [].asSynchronized() as List<Statement>
    final List<Closure> steps = [].asSynchronized() as List<Closure>

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
        for(Statement networkValuable : delegate.statements) {

            dumpedSomething = true

            networkValuable.inv = this

            this.totalStatements << networkValuable
            this.remainingStatements << networkValuable
        }

        delegate.ready = null
        delegate.steps.clear()
        delegate.statements.clear()

        return dumpedSomething
    }

    /**
     * Digest this inv.
     * This method is meant to be called during a digest cycle of the pool.
     * This method is also meant to be called synchronously.
     *
     * It allows to match the remaining statements.
     * Steps are also handled here.
     *
     * @param pool the pool currently in digestion
     * @return
     */
    synchronized Digestion digest(NetworkValuablePool pool ) {

        assert pool
        assert pool.isDigesting()

        boolean stopUnbloating = false

        def digestion = new Digestion()

        // Loop because dump
        while (true) {

            boolean hasResolvedSomething = false
            List toRemove = []

            // Use fori-loop for speed
            for (int j = 0; j < this.remainingStatements.size(); j++) {
                Statement networkValuable = this.remainingStatements[j] as Statement
                networkValuable.match.manage(pool, networkValuable)

                // Process results for digestion
                digestion.addResults(networkValuable)

                if (networkValuable.state == Statement.FAILED)
                    break

                toRemove.add(networkValuable)

                if (pool.preventUnbloating(networkValuable)) {
                    stopUnbloating = true
                    break
                }
            }

            // Remove all NV meant to be deleted
            this.remainingStatements.removeAll(toRemove)

            if (stopUnbloating)
                break

            boolean hasDumpedSomething = false

            // Check for new steps if :
            // 1. has a remaining step
            // 2. has not (previously dumped something)
            // 3. has no more statements
            // 4. Is not halting
            while ( !steps.isEmpty() &&
                    !hasDumpedSomething &&
                    this.remainingStatements.isEmpty() &&
                    pool.runningState != NetworkValuablePool.HALTING) {

                // Call next step
                Closure step = steps.pop()
                step.resolveStrategy = Closure.DELEGATE_FIRST
                step.call()

                // If the step dumped something, remainingStatements won't be empty and exit loop
                hasDumpedSomething = dumpDelegate()
            }


            // Check for new dumps
            if (!hasDumpedSomething)
                break
        }

        digestionSummary.concat(digestion)
        return digestion
    }

    static class Digestion {
        Integer requires = 0
        Integer broadcasts = 0
        Integer unbloats = 0

        void addResults(Statement networkValuable) {
            assert networkValuable

            if (networkValuable.state >= Statement.SUCCESSFUL) {
                if (networkValuable.match == RequireStatement.REQUIRE) {
                    requires++
                }

                if (networkValuable.match == BroadcastStatement.BROADCAST) {
                    broadcasts++
                }
            }

            if (networkValuable.state == Statement.UNBLOADTING) {
                unbloats++
            }
        }

        void concat(Digestion digestion) {
            assert digestion

            this.requires += digestion.requires
            this.broadcasts += digestion.broadcasts
            this.unbloats += digestion.unbloats
        }
    }

}
