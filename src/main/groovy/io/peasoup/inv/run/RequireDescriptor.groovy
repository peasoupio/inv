package io.peasoup.inv.run

import groovy.transform.CompileStatic

@CompileStatic
class RequireDescriptor {

    private final StatementDescriptor statementDescriptor
    private final RequireStatement requireStatement

    RequireDescriptor(StatementDescriptor statementDescriptor, RequireStatement requireStatement) {
        assert statementDescriptor, 'StatementDescriptor is required'
        assert requireStatement, 'RequireStatement is required'

        this.statementDescriptor = statementDescriptor
        this.requireStatement = requireStatement
    }

    RequireDescriptor using(@DelegatesTo(RequireUsingDescriptor) Closure usingBody) {
        assert usingBody, 'Using body is required'

        RequireUsingDescriptor requireUsingDescriptor = new RequireUsingDescriptor()

        usingBody.resolveStrategy = Closure.DELEGATE_FIRST
        usingBody.delegate = requireUsingDescriptor
        usingBody.call()

        if (requireUsingDescriptor.id)
            requireStatement.id = requireUsingDescriptor.id

        if (requireUsingDescriptor.defaults != null)
            requireStatement.defaults = requireUsingDescriptor.defaults

        if (requireUsingDescriptor.unbloatable != null)
            requireStatement.unbloatable = requireUsingDescriptor.unbloatable

        requireStatement.resolved = requireUsingDescriptor.resolved
        requireStatement.unresolved = requireUsingDescriptor.unresolved

        return this
    }

    RequireDescriptor into(String variable) {
        assert variable, 'Variable is required'

        requireStatement.into = variable

        return this
    }
}
