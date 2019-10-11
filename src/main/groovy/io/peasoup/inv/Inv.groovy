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

}
