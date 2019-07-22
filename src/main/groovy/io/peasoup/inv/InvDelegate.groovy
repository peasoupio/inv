package io.peasoup.inv

class InvDelegate {


    final List<NetworkValuable> networkValuables = [].asSynchronized()

    String name
    Closure ready

    void name(String name) {
        assert name

        this.name = name
    }

    Map impersonate(Closure body) {
        body.delegate = this

        [
            now: {
                body()
            },
            with: { params ->
                body(params)
            }
        ]
    }

    NetworkValuableDescriptor broadcast(NetworkValuableDescriptor networkValuableDescriptor) {

        assert networkValuableDescriptor

        NetworkValuable networkValuable = new NetworkValuable()

        networkValuable.id = networkValuableDescriptor.id ?: "undefined"
        networkValuable.name = networkValuableDescriptor.name
        networkValuable.match = NetworkValuable.BROADCAST

        networkValuableDescriptor.usingDigestor = { Closure usingBody ->
            BroadcastDelegate delegate = new BroadcastDelegate()

            usingBody.delegate = delegate
            usingBody.call()

            if (delegate.id)
                networkValuable.id = delegate.id

            networkValuable.ready = delegate.ready
            networkValuable.unready = delegate.unready
        }

        networkValuables << networkValuable

        return networkValuableDescriptor
    }

    NetworkValuableDescriptor require(NetworkValuableDescriptor networkValuableDescriptor) {
        assert networkValuableDescriptor

        NetworkValuable networkValuable = new NetworkValuable()

        networkValuable.id = networkValuableDescriptor.id ?: "undefined"
        networkValuable.name = networkValuableDescriptor.name
        networkValuable.match = NetworkValuable.REQUIRE

        networkValuableDescriptor.usingDigestor = { Closure usingBody ->
            RequireDelegate delegate = new RequireDelegate()

            usingBody.delegate = delegate
            usingBody.call()

            if (delegate.id)
                networkValuable.id = delegate.id

            networkValuable.resolved = delegate.resolved
            networkValuable.unresolved = delegate.unresolved
        }
        networkValuableDescriptor.intoDigestor = { String into ->
            networkValuable.into = into
        }

        networkValuables << networkValuable

        return networkValuableDescriptor
    }

    void ready(Closure ready) {
        assert ready

        this.ready = ready
    }
}
