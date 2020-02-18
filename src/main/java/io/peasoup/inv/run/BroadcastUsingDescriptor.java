package io.peasoup.inv.run;

import groovy.lang.Closure;

import java.util.Map;

public class BroadcastUsingDescriptor {
    private Object id;
    private Closure ready;

    /**
     * Defines the broadcast id from a generic object
     * NOTE : Null is authorized and is meant as an "undefined/global" id.
     * In this case, the network valuable name must be relevant enough.
     *
     * @param id the object id
     */
    public void id(Object id) {
        this.id = id;
    }

    /**
     * Defines the broadcast id from a Map object
     *
     * @param id the map id
     */
    public void id(Map id) {
        this.id = id;
    }

    /**
     * Event raised when broadcast is ready during the running cycle.
     * A return value is expected if something is meant to be shared to other requirements.
     * Otherwise, "null" will be shared.
     *
     * @param readyBody the closure body receiving the ready event
     */
    public void ready(Closure readyBody) {
        this.ready = readyBody;
    }

    public Object getId() {
        return id;
    }

    public Closure getReady() {
        return ready;
    }
}
