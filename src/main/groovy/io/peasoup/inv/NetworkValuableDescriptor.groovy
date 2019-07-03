package io.peasoup.inv

class NetworkValuableDescriptor {

    String name
    Object id

    Closure usingDigestor
    Closure intoDigestor

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
