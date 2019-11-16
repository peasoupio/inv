package io.peasoup.inv

class InvDescriptor {

    String name
    String path
    Closure ready

    final List<NetworkValuable> networkValuables = [].asSynchronized()
    final List<Closure> steps = [].asSynchronized()

    void name(String name) {
        assert name

        this.name = name
    }

    void path(String path) {
        assert path

        this.path = path
    }

    void ready(Closure ready) {
        assert ready

        this.ready = ready
    }

    NetworkValuableDescriptor broadcast(NetworkValuableDescriptor networkValuableDescriptor) {

        assert networkValuableDescriptor

        BroadcastValuable networkValuable = new BroadcastValuable()

        networkValuable.id = networkValuableDescriptor.id ?: NetworkValuable.DEFAULT_ID
        networkValuable.name = networkValuableDescriptor.name

        networkValuableDescriptor.usingDigestor = { Closure usingBody ->
            BroadcastDescriptor delegate = new BroadcastDescriptor()

            usingBody.delegate = delegate
            usingBody.call()

            if (delegate.id)
                networkValuable.id = delegate.id

            networkValuable.ready = delegate.ready
        }

        networkValuables << networkValuable

        return networkValuableDescriptor
    }

    NetworkValuableDescriptor require(NetworkValuableDescriptor networkValuableDescriptor) {
        assert networkValuableDescriptor

        RequireValuable networkValuable = new RequireValuable()

        networkValuable.id = networkValuableDescriptor.id ?: NetworkValuable.DEFAULT_ID
        networkValuable.name = networkValuableDescriptor.name

        networkValuableDescriptor.usingDigestor = { Closure usingBody ->
            RequireDescriptor delegate = new RequireDescriptor()

            usingBody.delegate = delegate
            usingBody.call()

            if (delegate.id)
                networkValuable.id = delegate.id

            if (delegate.defaults != null)
                networkValuable.defaults = delegate.defaults

            if (delegate.unbloatable != null)
                networkValuable.unbloatable = delegate.unbloatable

            networkValuable.resolved = delegate.resolved
            networkValuable.unresolved = delegate.unresolved


        }
        networkValuableDescriptor.intoDigestor = { String into ->
            networkValuable.into = into
        }

        networkValuables << networkValuable

        return networkValuableDescriptor
    }

    void step(Closure step) {
        steps << step
    }



}
