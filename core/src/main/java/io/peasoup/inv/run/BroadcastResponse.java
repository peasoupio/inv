package io.peasoup.inv.run;

import groovy.lang.MetaClass;
import lombok.Getter;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.codehaus.groovy.runtime.InvokerHelper;

import java.util.Map;

public class BroadcastResponse {

    private final BroadcastStatement broadcastStatement;

    /**
     * Gets the name of the INV who created this broadcast.
     * @return String representation of the name
     */
    @Getter
    private final String resolvedBy;

    /**
     * Gets if this broadcast has a dynamic response
     * @return True if has dynamic, otherwise false
     */
    @Getter
    private final boolean dynamic;

    private boolean onGlobalRaised = false;
    private Object globalResponse;

    BroadcastResponse(BroadcastStatement broadcastStatement) {
        if (broadcastStatement == null) {
            throw new IllegalArgumentException("broadcastStatement");
        }

        this.broadcastStatement = broadcastStatement;
        this.resolvedBy = broadcastStatement.getInv().getName();
        this.dynamic = broadcastStatement.isDynamic();

        if (!broadcastStatement.isDelayed())
            onGlobal();
    }

    /**
     * Attemtps to resolve the global response and returns it.
     * @return The global response.
     */
    public Object getGlobalResponse() {
        // If response available, return its value
        if (globalResponse != null)
            return globalResponse;

        // If onReady has not been raised, attempt to do it
        if (!onGlobalRaised)
            onGlobal();

        return globalResponse;
    }

    private synchronized void onGlobal() {
        // If onReadyRaised, avoid recalling "ready"
        if (onGlobalRaised)
            return;

        // Make sure onReadyRaised is raised only once
        onGlobalRaised = true;

        Object responseObject = null;
        if (broadcastStatement.getGlobal() != null) {
            responseObject = broadcastStatement.getGlobal().call();
            checkResponseType(responseObject);
        }

        // If response object is null, return a null metaclass
        if (responseObject == null) {
            this.globalResponse = new BroadcastNullResponseNonMetaclass(this);
        } else {

            // Otherwise, check if its a primitive
            if (isPrimitive(responseObject))
                // If so, wrap the value inside an hashmap inside "value" as its key.
                responseObject = Map.of("value", responseObject);

            this.globalResponse = new BroadcastGlobalResponseMetaClass(this, responseObject).getGlobalResponse();
        }
    }

    /**
     * Evaluate a dynamic response for a specific require statement.
     *
     * @param requireStatement The require statement
     * @return The evaluated response.
     */
    public Object getDynamicResponse(RequireStatement requireStatement) {
        if (!dynamic) return null;
        if (broadcastStatement.getDynamic() == null) return null;

        Object responseObject = broadcastStatement.getDynamic().call(
                requireStatement.getId());

        checkResponseType(responseObject);

        // If null, return a null metaclass
        if (responseObject == null)
            return new BroadcastNullResponseNonMetaclass(this);

        // If primitive, wrap the value inside an hashmap inside "value" as its key.
        if (isPrimitive(responseObject))
            responseObject = Map.of("value", responseObject);

        // Return a new Dynamic metaclass
        return new BroadcastDynamicResponseMetaClass(
                requireStatement,
                this,
                responseObject).getShell().getDynamicResponse();
    }

    /**
     * Gets if an object is considered a primitive from the point of view of a response.
     *
     * @param object The object
     * @return True if a class primitive of a CharSequence, otherwise (also null) returns false
     */
    public boolean isPrimitive(Object object) {
        if (object == null)
            return false;

        if (object instanceof CharSequence)
            return true;

        if (object instanceof Boolean)
            return true;

        return object.getClass().isPrimitive();
    }

    /*
    @Override
    public Object getId() {
        return broadcastStatement.getId();
    }

    @Override
    public String getName() {
        return broadcastStatement.getName();
    }

    @Override
    public int hashCode() {
        return StatementHasher.hashcode(broadcastStatement);
    }

    @Override
    public boolean equals(Object obj) {
        return StatementHasher.equals(broadcastStatement, obj);
    }
     */

    @Override
    public String toString() {
        if (globalResponse == null)
            return "";

        return DefaultGroovyMethods.toString(globalResponse);
    }

    private void checkResponseType(Object responseObject) {
        if (responseObject == null)
            return;

        MetaClass responseObjectClz = InvokerHelper.getMetaClass(responseObject);

        if (
            responseObjectClz instanceof BroadcastDynamicResponseMetaClass ||
            responseObjectClz instanceof BroadcastGlobalResponseMetaClass ||
            responseObjectClz instanceof BroadcastNullResponseNonMetaclass
        )
            throw new IllegalStateException("Cannot return the response object from a previous required statement.");
    }


}
