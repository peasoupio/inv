package io.peasoup.inv

import groovy.transform.CompileStatic

@CompileStatic
class BroadcastDescriptor {

    Object id
    Closure ready

    /**
     * Defines the broadcast id from a generic object
     * NOTE : Null is authorized and is meant as an "undefined/global" id.
     *        In this case, the network valuable name must be relevant enough.
     * @param id the object id
     */
    void id(Object id) {
        this.id = id
    }

    /**
     * Defines the broadcast id from a Map object
     * @param id the map id
     */
    void id(Map id) {
        this.id = id
    }

    /**
     * Event raised when broadcast is ready during the running cycle.
     * A return value is expected if something is meant to be shared to other requirements.
     * Otherwise, "null" will be shared.
     *
     * @param resolvedBody the closure body receiving the ready event
     */
    void ready(Closure readyBody) {
        this.ready = readyBody
    }

}
