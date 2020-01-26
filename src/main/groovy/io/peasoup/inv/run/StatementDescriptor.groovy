package io.peasoup.inv.run

import groovy.transform.CompileStatic
import org.apache.commons.lang.StringUtils

@CompileStatic
class StatementDescriptor {

    final String name
    Object id

    Closure usingDigestor
    Closure intoDigestor

    StatementDescriptor(String name) {
        assert name, 'Name is required'
        assert StringUtils.isAlphanumeric(name), 'Name must be an alphanumeric value'

        this.name = name
    }

    // it protects from involuntarily null name calling inv.Something() (instead of inv.Somathing)
    StatementDescriptor call() {
        return this
    }

    StatementDescriptor call(Object id) {
        assert id, 'Id, as an object, is required'

        this.id = id

        return this
    }

    StatementDescriptor call(Map id) {
        assert id, 'Id, as a Map, is required'

        this.id = id

        return this
    }

    StatementDescriptor using(Closure usingBody) {
        assert usingDigestor, 'Using digestor must be defined before calling'
        assert usingBody, 'Using body is required'

        usingDigestor.resolveStrategy = Closure.DELEGATE_FIRST
        usingDigestor.call(usingBody)

        return this
    }

    StatementDescriptor into(String variable) {
        assert intoDigestor, "Into digestor must be defined. Are you sure you are using 'into' with a require and not a broadcast?"
        assert variable, 'Variable is required'

        intoDigestor.resolveStrategy = Closure.DELEGATE_FIRST
        intoDigestor.call(variable)

        return this
    }
}
