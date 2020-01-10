package io.peasoup.inv

import groovy.transform.CompileStatic
import org.apache.commons.lang.StringUtils

@CompileStatic
class StatementDescriptor {

    final String name
    Object id

    Closure usingDigestor
    Closure intoDigestor

    StatementDescriptor(String name) {

        assert name
        assert StringUtils.isAlphanumeric(name)

        this.name = name
    }

    // it protects from involuntarily null name calling inv.Something() (instead of inv.Somathing)
    StatementDescriptor call() {
        return this
    }

    StatementDescriptor call(Object id) {
        assert id

        this.id = id

        return this
    }

    StatementDescriptor call(Map id) {
        assert id

        this.id = id

        return this
    }

    StatementDescriptor using(Closure usingBody) {
        assert usingDigestor
        assert usingBody

        this.usingDigestor.call(usingBody)

        return this
    }

    StatementDescriptor into(String variable) {
        assert variable

        this.intoDigestor.call(variable)

        return this
    }
}
