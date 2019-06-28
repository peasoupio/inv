package main.groovy.io.peasoup.inv

class NetworkValuableDescriptor {

    Object id
    String name
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

    def using(Closure usingBody) {
        this.digestor.call(usingBody)
    }

}
