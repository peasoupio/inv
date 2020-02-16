package io.peasoup.inv.run

import groovy.transform.CompileStatic
import org.apache.commons.lang.StringUtils

@CompileStatic
class StatementDescriptor {

    final String name
    Object id

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
}
