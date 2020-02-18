package io.peasoup.inv.run;

import groovy.lang.Closure;

import java.util.Map;

public class RequireUsingDescriptor {
    private Object id;
    private Closure resolved;
    private Closure unresolved;
    private Boolean unbloatable;
    private Boolean defaults;

    /**
     * Defines the requirement id from a generic object
     * NOTE : Null is authorized and is meant as an "undefined/global" id.
     * In this case, the network valuable name must be relevant enough.
     *
     * @param id the object id
     */
    public void id(Object id) {
        this.id = id;
    }

    /**
     * Defines the requirement id from a Map object
     *
     * @param id the map id
     */
    public void id(Map id) {
        this.id = id;
    }

    /**
     * Event raised when requirement is resolved during the running cycle.
     * No return value is expected.
     *
     * @param resolvedBody the closure body receiving the resolved event
     */
    public void resolved(Closure resolvedBody) {
        this.resolved = resolvedBody;
    }

    /**
     * Event raised when requirement is not resolved during the unbloating and halting cycles
     * No return value is expected.
     *
     * @param unresolvedBody the closure body receiving the unresolved event
     */
    public void unresolved(Closure unresolvedBody) {
        this.unresolved = unresolvedBody;
    }

    /**
     * Defines if this requirement (network valuable) can unbloat/disappear upon the unbloating cycle
     *
     * @param value the boolean value
     */
    public void unbloatable(boolean value) {
        this.unbloatable = value;
    }

    /**
     * Defines if this requirement allow to call the default ("$") closure of the associated response
     *
     * @param value the boolean value
     */
    public void defaults(boolean value) {
        this.defaults = value;
    }

    public Object getId() {
        return id;
    }

    public Closure getResolved() {
        return resolved;
    }

    public Closure getUnresolved() {
        return unresolved;
    }

    public Boolean getUnbloatable() {
        return unbloatable;
    }

    public Boolean getDefaults() {
        return defaults;
    }
}
