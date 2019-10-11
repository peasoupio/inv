package io.peasoup.inv

class BroadcastDelegate {

    Object id
    Closure ready

    void id(Object id) {
        this.id = id
    }

    void id(Map id) {
        this.id = id
    }

    void ready(Closure readyBody) {
        this.ready = readyBody
    }

}
