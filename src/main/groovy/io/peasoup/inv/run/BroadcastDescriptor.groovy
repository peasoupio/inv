package io.peasoup.inv.run

import groovy.transform.CompileStatic

@CompileStatic
class BroadcastDescriptor {

    private final StatementDescriptor statementDescriptor
    private final BroadcastStatement broadcastStatement

    BroadcastDescriptor(StatementDescriptor statementDescriptor, BroadcastStatement broadcastStatement) {
        assert statementDescriptor, 'StatementDescriptor is required'
        assert broadcastStatement, 'BroadcastStatement is required'

        this.statementDescriptor = statementDescriptor
        this.broadcastStatement = broadcastStatement
    }

    BroadcastDescriptor using(@DelegatesTo(BroadcastUsingDescriptor) Closure usingBody) {
        assert usingBody, 'Using body is required'

        BroadcastUsingDescriptor broadcastUsingDescriptor = new BroadcastUsingDescriptor()

        usingBody.resolveStrategy = Closure.DELEGATE_FIRST
        usingBody.delegate = broadcastUsingDescriptor
        usingBody.call()

        if (broadcastUsingDescriptor.id)
            broadcastStatement.id = broadcastUsingDescriptor.id

        broadcastStatement.ready = broadcastUsingDescriptor.ready

        return this
    }
}
