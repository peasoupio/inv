package io.peasoup.inv.run

import groovy.transform.CompileStatic

@CompileStatic
class InvDescriptor {

    String name
    String path
    Closure ready
    final InvNames inv = InvNames.Instance

    final List<Statement> statements = [].asSynchronized() as List<Statement>
    final List<Closure> steps = [].asSynchronized() as List<Closure>

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

            BroadcastDescriptor broadcastDescriptor = new BroadcastDescriptor()

            usingBody.resolveStrategy = Closure.DELEGATE_FIRST
            usingBody.delegate = broadcastDescriptor
            usingBody.call()

            if (broadcastDescriptor.id)
                broadcastValuable.id = broadcastDescriptor.id

            broadcastValuable.ready = broadcastDescriptor.ready
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

            RequireDescriptor requireDescriptor = new RequireDescriptor()

            usingBody.resolveStrategy = Closure.DELEGATE_FIRST
            usingBody.delegate = requireDescriptor
            usingBody.call()

            if (requireDescriptor.id)
                requireValuable.id = requireDescriptor.id

            if (requireDescriptor.defaults != null)
                requireValuable.defaults = requireDescriptor.defaults

            if (requireDescriptor.unbloatable != null)
                requireValuable.unbloatable = requireDescriptor.unbloatable

            requireValuable.resolved = requireDescriptor.resolved
            requireValuable.unresolved = requireDescriptor.unresolved
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
