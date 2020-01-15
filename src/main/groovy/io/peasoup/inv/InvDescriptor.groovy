package io.peasoup.inv

class InvDescriptor {

    String name
    String path
    Closure ready
    final InvNames inv = InvNames.Instance

    final List<Statement> statements = [].asSynchronized()
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

    StatementDescriptor broadcast(StatementDescriptor networkValuableDescriptor) {

        assert networkValuableDescriptor

        BroadcastStatement broadcastValuable = new BroadcastStatement()

        broadcastValuable.id = networkValuableDescriptor.id ?: Statement.DEFAULT_ID
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

        statements << broadcastValuable

        return networkValuableDescriptor
    }

    StatementDescriptor require(StatementDescriptor networkValuableDescriptor) {
        assert networkValuableDescriptor

        RequireStatement requireValuable = new RequireStatement()

        requireValuable.id = networkValuableDescriptor.id ?: Statement.DEFAULT_ID
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

        statements << requireValuable

        return networkValuableDescriptor
    }

    void step(Closure step) {
        steps << step
    }
}
