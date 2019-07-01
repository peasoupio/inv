package io.peasoup.inv

class NetworkValuableDescriptor {

    String name
    Object id
    Closure digestor

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

    void using(Closure usingBody) {
        assert digestor
        assert usingBody

        this.digestor.call(usingBody)
    }

}
