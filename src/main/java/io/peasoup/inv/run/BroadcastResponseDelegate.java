package io.peasoup.inv.run;

import groovy.lang.Closure;

public class BroadcastResponseDelegate {

    private final BroadcastResponse broadcastResponse;
    private final Inv caller;
    private final boolean defaults;
    private final Object defaultResponse;

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

        defaultResponse = checkDefault();
    }

    public Object propertyMissing(String propertyName) {
        if (propertyName.equals("response"))
            return this;

        if (propertyName.equals("resolvedBy"))
            return broadcastResponse.getResolvedBy();

        // Check from default response
        Object fromDefault = BroadcastResponse.tryInvokeProperty(defaultResponse, propertyName);
        if (fromDefault != null)
            return fromDefault;

        // Check from general response
        Object fromResponse = BroadcastResponse.tryInvokeProperty(broadcastResponse.getResponse(), propertyName);
        if (fromResponse != null)
            return fromResponse;

        return null;
    }

    public Object methodMissing(String methodName, Object args) {

        // Check from default response
        Object fromDefault = BroadcastResponse.tryInvokeMethod(defaultResponse, methodName, args);
        if (fromDefault != null)
            return wrapReturnValue(fromDefault, args);

        // Check from default response
        Object fromBroadcast = BroadcastResponse.tryInvokeMethod(broadcastResponse.getResponse(), methodName, args);
        if (fromBroadcast != null)
            return wrapReturnValue(fromBroadcast, args);

        return null;
    }

    private Object wrapReturnValue(Object returnValue, Object args) {
        if (returnValue instanceof Closure)
            return wrapClosure((Closure) returnValue, args);

        return returnValue;
    }

    private Object wrapClosure(Closure source, Object args) {
        Closure copy = source.dehydrate().rehydrate(
                caller.getDelegate(),
                source.getOwner(),
                source.getThisObject());
        copy.setResolveStrategy(Closure.DELEGATE_FIRST);

        return copy.invokeMethod("call", args);
    }

    @Override
    public String toString() {
        return broadcastResponse.toString();
    }

    private Object checkDefault() {
        // If response has a default closure, call it right now
        if (!defaults || broadcastResponse.getDefaultClosure() == null)
            return null;

        Closure defaultClosure = broadcastResponse.getDefaultClosure();
        Closure copy = defaultClosure.dehydrate().rehydrate(
                caller.getDelegate(),
                defaultClosure.getOwner(),
                defaultClosure.getThisObject());
        copy.setResolveStrategy(Closure.DELEGATE_FIRST);

        return copy.call();
    }
}
