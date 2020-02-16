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

    BroadcastDescriptor broadcast(StatementDescriptor statementDescriptor) {
        assert statementDescriptor, 'Statement descriptor is required'

        BroadcastStatement broadcastStatement = new BroadcastStatement()

        broadcastStatement.id = statementDescriptor.id ?: Statement.DEFAULT_ID
        broadcastStatement.name = statementDescriptor.name

        statements << broadcastStatement

        return new BroadcastDescriptor(statementDescriptor, broadcastStatement)
    }

    RequireDescriptor require(StatementDescriptor statementDescriptor) {
        assert statementDescriptor, 'Statement descriptor is required'

        RequireStatement requireStatement = new RequireStatement()

        requireStatement.id = statementDescriptor.id ?: Statement.DEFAULT_ID
        requireStatement.name = statementDescriptor.name

        statements << requireStatement

        return new RequireDescriptor(statementDescriptor, requireStatement)
    }

    void step(Closure stepBody) {
        assert stepBody, 'Step body is required'

        steps.add(stepBody)
    }
}
