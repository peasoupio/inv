package io.peasoup.inv

class InvDescriptor {

    String name
    String path
    Closure ready
    final InvNames inv = InvNames.Instance

    final List<Statement> statements = [].asSynchronized()
    final List<Closure> steps = [].asSynchronized()

    void name(String name) {
        assert name, 'Name is required'

        this.name = name
    }

    void path(String path) {
        assert path, 'Path is required'

        this.path = path
    }

    void ready(Closure readyBody) {
        assert readyBody, 'Ready body is required'

        this.ready = readyBody
    }

    StatementDescriptor broadcast(StatementDescriptor statementDescriptor) {
        assert statementDescriptor, 'Statement descriptor is required'

        BroadcastStatement broadcastValuable = new BroadcastStatement()

        broadcastValuable.id = statementDescriptor.id ?: Statement.DEFAULT_ID
        broadcastValuable.name = statementDescriptor.name

        statementDescriptor.usingDigestor = { Closure usingBody ->
            assert usingBody, 'Using body is required'

            BroadcastDescriptor delegate = new BroadcastDescriptor()

            usingBody.resolveStrategy = Closure.DELEGATE_FIRST
            usingBody.delegate = delegate
            usingBody.call()

            if (delegate.id)
                broadcastValuable.id = delegate.id

            broadcastValuable.ready = delegate.ready
        }

        statements << broadcastValuable

        return statementDescriptor
    }

    StatementDescriptor require(StatementDescriptor statementDescriptor) {
        assert statementDescriptor, 'Statement descriptor is required'

        RequireStatement requireValuable = new RequireStatement()

        requireValuable.id = statementDescriptor.id ?: Statement.DEFAULT_ID
        requireValuable.name = statementDescriptor.name

        statementDescriptor.usingDigestor = { Closure usingBody ->
            assert usingBody, 'Using body is required'

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
        statementDescriptor.intoDigestor = { String into ->
            requireValuable.into = into
        }

        statements << requireValuable

        return statementDescriptor
    }

    void step(Closure stepBody) {
        assert stepBody, 'Step body is required'

        steps.add(stepBody)
    }
}
