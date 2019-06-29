package io.peasoup.inv

class BroadcastDelegate {

    Object id
    Closure ready
    Closure unready

    def id(Object id) {
        this.id = id
    }

    def id(Map id) {
        this.id = id
    }

    def ready(Closure readyBody) {

        assert readyBody

        this.ready = readyBody
    }

    def unready(Closure unreadyBody) {
        this.unready = unreadyBody
    }

}
