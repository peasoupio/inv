package main.groovy.io.peasoup.inv

class RequireDelegate {

    Object id
    Closure resolved
    Closure unresolved

    def id(Object id) {
        this.id = id
    }

    def id(Map id) {
        this.id = id
    }

    def received(Closure resolvedBody) {
        this.resolved  = resolvedBody
    }

    def unresolved(Closure unresolvedBody) {
        this.unresolved  = unresolvedBody
    }
}
