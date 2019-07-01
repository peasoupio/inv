package io.peasoup.inv

class BroadcastDelegate {

    Object id
    Closure ready
    Closure unready

    void id(Object id) {
        this.id = id
    }

    void id(Map id) {
        this.id = id
    }

    void ready(Closure readyBody) {
        this.ready = readyBody
    }

    void unready(Closure unreadyBody) {
        this.unready = unreadyBody
    }

}
