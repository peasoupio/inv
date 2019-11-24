package io.peasoup.inv

import groovy.transform.CompileStatic
import org.apache.commons.lang.StringUtils

@CompileStatic
class NetworkValuableDescriptor {

    final String name
    Object id

    Closure usingDigestor
    Closure intoDigestor

    NetworkValuableDescriptor(String name) {

        assert name
        assert StringUtils.isAlphanumeric(name)

        this.name = name
    }

    // it protects from involuntarily null name calling inv.Something() (instead of inv.Somathing)
    NetworkValuableDescriptor call() {
        return this
    }

    NetworkValuableDescriptor call(Object id) {
        assert id

        this.id = id

        return this
    }

    NetworkValuableDescriptor call(Map id) {
        assert id

        this.id = id

        return this
    }

    NetworkValuableDescriptor using(Closure usingBody) {
        assert usingDigestor
        assert usingBody

        this.usingDigestor.call(usingBody)

        return this
    }

    NetworkValuableDescriptor into(String variable) {
        assert variable

        this.intoDigestor.call(variable)

        return this
    }
}
