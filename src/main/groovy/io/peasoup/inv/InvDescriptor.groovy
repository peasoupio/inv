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

        BroadcastValuable broadcastValuable = new BroadcastValuable()

        broadcastValuable.id = networkValuableDescriptor.id ?: NetworkValuable.DEFAULT_ID
        broadcastValuable.name = networkValuableDescriptor.name

        networkValuableDescriptor.usingDigestor = { Closure usingBody ->
            BroadcastDescriptor delegate = new BroadcastDescriptor()

            usingBody.resolveStrategy = Closure.DELEGATE_FIRST
            usingBody.delegate = delegate
            usingBody.call()

            if (delegate.id)
                broadcastValuable.id = delegate.id

            broadcastValuable.ready = delegate.ready
        }

        networkValuables << broadcastValuable

        return networkValuableDescriptor
    }

    NetworkValuableDescriptor require(NetworkValuableDescriptor networkValuableDescriptor) {
        assert networkValuableDescriptor

        RequireValuable requireValuable = new RequireValuable()

        requireValuable.id = networkValuableDescriptor.id ?: NetworkValuable.DEFAULT_ID
        requireValuable.name = networkValuableDescriptor.name

        networkValuableDescriptor.usingDigestor = { Closure usingBody ->
            RequireDescriptor delegate = new RequireDescriptor()

            usingBody.resolveStrategy = Closure.DELEGATE_FIRST
            usingBody.delegate = delegate
            usingBody.call()

            if (delegate.id)
                requireValuable.id = delegate.id

            if (delegate.defaults != null)
                requireValuable.defaults = delegate.defaults

            if (delegate.unbloatable != null)
                requireValuable.unbloatable = delegate.unbloatable

            requireValuable.resolved = delegate.resolved
            requireValuable.unresolved = delegate.unresolved
        }
        networkValuableDescriptor.intoDigestor = { String into ->
            requireValuable.into = into
        }

        networkValuables << requireValuable

        return networkValuableDescriptor
    }

    void step(Closure step) {
        steps << step
    }



}
