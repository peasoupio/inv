package io.peasoup.inv

class Inv {

    String name
    Closure ready

    boolean sync = true

    final InvDelegate delegate = new InvDelegate()

    final List<NetworkValuable> remainingValuables = [].asSynchronized()
    final List<NetworkValuable> totalValuables = [].asSynchronized()

    boolean dumpDelegate() {
        if (!name)
            name = delegate.name

        if (!ready)
            ready = delegate.ready

        if (delegate.networkValuables.isEmpty())
            return false

        // use for-loop to keep order
        for(NetworkValuable networkValuable : delegate.networkValuables) {
            networkValuable.inv = this

            this.totalValuables << networkValuable
            this.remainingValuables << networkValuable
        }

        delegate.networkValuables.clear()

        return true
    }

}
