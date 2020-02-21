package io.peasoup.inv.run;

import groovy.lang.Closure;

import java.util.HashMap;
import java.util.Map;

public class BroadcastResponseDelegate {

    private final BroadcastResponse broadcastResponse;
    private final Inv caller;
    private final boolean defaults;

    private final Map<String, Object> properties;

    /**
     * Cast Response into a thread-safe delegate.
     * It also expands response closures at top level already bound to the inv's delegate (for inner broadcasts and requires)
     *
     * @param broadcastResponse the response to delegate
     * @param caller the actual inv requiring this response
     * @param defaults active or not default closure
     */
    BroadcastResponseDelegate(BroadcastResponse broadcastResponse, Inv caller, boolean defaults) {
        if (broadcastResponse == null) {
            throw new IllegalArgumentException("BroadcastResponse is required");
        }
        if (caller == null) {
            throw new IllegalArgumentException("Caller is required");
        }

        this.broadcastResponse = broadcastResponse;
        this.caller = caller;
        this.defaults = defaults;

        properties = new HashMap<>();
        properties.put("resolvedBy", broadcastResponse.getResolvedBy());
        if (broadcastResponse.getResponse() != null)
            properties.putAll(broadcastResponse.getResponse());

        checkDefault();
    }

    public Object propertyMissing(String propertyName) {
        if (propertyName.equals("response"))
            return this;

        return properties.get(propertyName);
    }

    public Object methodMissing(String methodName, Object args) {
        Object closure = properties.get(methodName);

        if (closure == null) return null;
        if (!(closure instanceof Closure)) return null;

        Closure actualClosure = (Closure)closure;

        Closure copy = actualClosure.dehydrate().rehydrate(caller.getDelegate(), actualClosure.getOwner(), actualClosure.getThisObject());
        copy.setResolveStrategy(Closure.DELEGATE_FIRST);

        return copy.invokeMethod("call", args);
    }

    @Override
    public String toString() {
        return broadcastResponse.toString();
    }

    private void checkDefault() {
        // If response has a default closure, call it right now
        if (!defaults || broadcastResponse.getDefaultClosure() == null)
            return;

        Closure defaultClosure = broadcastResponse.getDefaultClosure();

        Closure copy = defaultClosure.dehydrate().rehydrate(caller.getDelegate(), defaultClosure.getOwner(), defaultClosure.getThisObject());
        copy.setResolveStrategy(Closure.DELEGATE_FIRST);
        Object defaultResponse = copy.call();

        if (defaultResponse instanceof Map) {
            properties.putAll((Map)defaultResponse);
        }
    }
}
