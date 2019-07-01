package io.peasoup.inv

class InvDelegate {

    final List<NetworkValuable> networkValuables = [].asSynchronized()

    String name

    void name(String name) {
        assert name

        this.name = name
    }

    NetworkValuableDescriptor broadcast(NetworkValuableDescriptor networkValuableDescriptor) {
        assert networkValuableDescriptor

        if (networkValuableDescriptor.id) {
            networkValuables << new NetworkValuable(
                id: networkValuableDescriptor.id,
                name: networkValuableDescriptor.name,
                match: NetworkValuable.BROADCAST
            )

            return networkValuableDescriptor
        }

        networkValuableDescriptor.digestor = { Closure usingBody ->
            BroadcastDelegate delegate = new BroadcastDelegate()

            usingBody.delegate = delegate
            usingBody.call()

            NetworkValuable networkValuable = new NetworkValuable(
                    id: delegate.id,
                    name: networkValuableDescriptor.name,
                    match: NetworkValuable.BROADCAST,
                    ready: delegate.ready,
                    unready: delegate.unready
            )

            networkValuables << networkValuable
        }

        return networkValuableDescriptor
    }

    NetworkValuableDescriptor require(NetworkValuableDescriptor networkValuableDescriptor) {
        assert networkValuableDescriptor

        if (networkValuableDescriptor.id) {
            networkValuables << new NetworkValuable(
                id: networkValuableDescriptor.id,
                name: networkValuableDescriptor.name,
                match: NetworkValuable.REQUIRE
            )

            return networkValuableDescriptor
        }


        networkValuableDescriptor.digestor = { Closure usingBody ->
            RequireDelegate delegate = new RequireDelegate()

            usingBody.delegate = delegate
            usingBody.call()

            NetworkValuable networkValuable = new NetworkValuable(
                    id: delegate.id,
                    name: networkValuableDescriptor.name,
                    match: NetworkValuable.REQUIRE,
                    resolved: delegate.resolved,
                    unresolved: delegate.unresolved
            )

            networkValuables << networkValuable
        }

        return networkValuableDescriptor
    }
}
