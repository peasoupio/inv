package io.peasoup.inv

class RequireDelegate {

    Object id
    Closure resolved
    Closure unresolved

    void id(Object id) {
        this.id = id
    }

    void id(Map id) {
        this.id = id
    }

    void resolved(Closure resolvedBody) {
        this.resolved  = resolvedBody
    }

    void unresolved(Closure unresolvedBody) {
        this.unresolved  = unresolvedBody
    }
}
