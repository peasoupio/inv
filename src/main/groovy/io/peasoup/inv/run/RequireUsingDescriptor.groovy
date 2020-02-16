package io.peasoup.inv.run

import groovy.transform.CompileStatic

@CompileStatic
class RequireUsingDescriptor {

    Object id
    Closure resolved
    Closure unresolved
    Boolean unbloatable
    Boolean defaults

    /**
     * Defines the requirement id from a generic object
     * NOTE : Null is authorized and is meant as an "undefined/global" id.
     *        In this case, the network valuable name must be relevant enough.
     * @param id the object id
     */
    void id(Object id) {
        this.id = id
    }

    /**
     * Defines the requirement id from a Map object
     * @param id the map id
     */
    void id(Map id) {
        this.id = id
    }

    /**
     * Event raised when requirement is resolved during the running cycle.
     * No return value is expected.
     *
     * @param resolvedBody the closure body receiving the resolved event
     */
    void resolved(Closure resolvedBody) {
        this.resolved  = resolvedBody
    }

    /**
     * Event raised when requirement is not resolved during the unbloating and halting cycles
     * No return value is expected.
     *
     * @param unresolvedBody the closure body receiving the unresolved event
     */
    void unresolved(Closure unresolvedBody) {
        this.unresolved  = unresolvedBody
    }

    /**
     * Defines if this requirement (network valuable) can unbloat/disappear upon the unbloating cycle
     * @param value the boolean value
     */
    void unbloatable(boolean value) {
        this.unbloatable = value
    }

    /**
     * Defines if this requirement allow to call the default ("$") closure of the associated response
     * @param value the boolean value
     */
    void defaults(boolean value) {
        this.defaults = value
    }
}
