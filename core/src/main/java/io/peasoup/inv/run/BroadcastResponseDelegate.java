package io.peasoup.inv.run;

import groovy.lang.Closure;
import groovy.lang.GroovyInterceptable;
import groovy.lang.MetaClass;
import org.apache.commons.lang.NotImplementedException;
import org.codehaus.groovy.runtime.InvokerHelper;

public class BroadcastResponseDelegate implements GroovyInterceptable {

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

        this.defaultResponse = checkDefault();
    }

    @Override
    public String toString() {
        return broadcastResponse.toString();
    }

    @Override
    public Object invokeMethod(String methodName, Object args) {
        // Check from default response
        Object fromDefault = BroadcastResponseInvoker.tryInvokeMethod(defaultResponse, methodName, args);
        if (fromDefault != null)
            return wrapReturnValue(fromDefault, args);

        // Check from default response
        Object fromBroadcast = BroadcastResponseInvoker.tryInvokeMethod(broadcastResponse.getResponse(), methodName, args);
        if (fromBroadcast != null)
            return wrapReturnValue(fromBroadcast, args);

        // For asserts or ifs...
        if ("asBoolean".equals(methodName))
            return true;

        return null;
    }

    @Override
    public Object getProperty(String propertyName) {
        if (propertyName.equals("response"))
            return this;

        if (propertyName.equals("resolvedBy"))
            return broadcastResponse.getResolvedBy();

        // Check from default response
        Object fromDefault = BroadcastResponseInvoker.tryInvokeProperty(defaultResponse, propertyName);
        if (fromDefault != null)
            return fromDefault;

        // Check from general response
        return BroadcastResponseInvoker.tryInvokeProperty(broadcastResponse.getResponse(), propertyName);
    }

    @Override
    public void setProperty(String propertyName, Object newValue) {
        // Check from default response
        if (BroadcastResponseInvoker.tryDefineProperty(defaultResponse, propertyName, newValue))
            return;

        // Check from general response
        BroadcastResponseInvoker.tryDefineProperty(broadcastResponse.getResponse(), propertyName, newValue);
    }

    @Override
    public MetaClass getMetaClass() {
        if (broadcastResponse.getResponse() == null)
            return null;

        return InvokerHelper.getMetaClass(broadcastResponse.getResponse());
    }

    @Override
    public void setMetaClass(MetaClass metaClass) {
        throw new NotImplementedException("setMetaClass");
    }

    private Object wrapReturnValue(Object returnValue, Object args) {
        if (returnValue instanceof Closure)
            return wrapClosure((Closure<Object>) returnValue, args);

        return returnValue;
    }

    private Object wrapClosure(Closure<Object> source, Object args) {
        Closure<Object> copy = source.dehydrate().rehydrate(
                caller.getDelegate(),
                source.getOwner(),
                source.getThisObject());
        copy.setResolveStrategy(Closure.DELEGATE_FIRST);

        return copy.invokeMethod("call", args);
    }

    private Object checkDefault() {
        // If response has a default closure, call it right now
        if (!defaults || broadcastResponse.getDefaultClosure() == null)
            return null;

        Closure<Object> defaultClosure = broadcastResponse.getDefaultClosure();
        Closure<Object> copy = defaultClosure.dehydrate().rehydrate(
                caller.getDelegate(),
                defaultClosure.getOwner(),
                defaultClosure.getThisObject());
        copy.setResolveStrategy(Closure.DELEGATE_FIRST);

        return copy.call();
    }
}
