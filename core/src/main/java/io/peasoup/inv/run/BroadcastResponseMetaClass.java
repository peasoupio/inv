package io.peasoup.inv.run;

import groovy.lang.*;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.codehaus.groovy.runtime.InvokerHelper;

import java.util.Map;

public class BroadcastResponseMetaClass extends ExpandoMetaClass {

    private final static MetaClass CALLER_METACLASS = DefaultGroovyMethods.getMetaClass(InvDescriptor.class);

    private final Object shell;

    private final Inv caller;
    private final BroadcastResponse broadcastResponse;


    private final MetaClass responseMetaClass;

    private final Object defaultObject;
    private final MetaClass defaultsMetaClass;

    /**
     * Cast Response into a thread-safe delegate.
     * It also expands response closures at top level already bound to the inv's delegate (for inner broadcasts and requires)
     *
     * @param broadcastResponse the response to delegate
     * @param caller the actual inv requiring this response
     * @param defaults active or not default closure
     */
    BroadcastResponseMetaClass(BroadcastResponse broadcastResponse, Inv caller, boolean defaults) {
        super(broadcastResponse.getResponse().getClass());

        if (caller == null) {
            throw new IllegalArgumentException("Caller is required");
        }

        this.broadcastResponse = broadcastResponse;
        this.caller = caller;

        this.responseMetaClass = DefaultGroovyMethods.getMetaClass(broadcastResponse.getResponse());
        this.shell = createShell();

        // Try getting default method
        Tuple2<Boolean, Object> defaultMetaMethod = lookUpMethod(BroadcastResponse.DEFAULT_RESPONSE_HOOK_SHORT, null);
        if (Boolean.FALSE.equals(defaultMetaMethod.getV1()))
            defaultMetaMethod = lookUpMethod(BroadcastResponse.DEFAULT_RESPONSE_HOOK, null);

        if (defaults && Boolean.TRUE.equals(defaultMetaMethod.getV1())) {
            defaultObject = defaultMetaMethod.getV2();
            defaultsMetaClass = DefaultGroovyMethods.getMetaClass(defaultObject);

        } else {
            defaultObject = null;
            defaultsMetaClass = null;
        }
    }

    public Object getShell() {
        return this.shell;
    }

    @Override
    public Object invokeMethod(Class sender, Object object, String methodName, Object[] originalArguments, boolean isCallToSuper, boolean fromInsideClass) {
        switch (methodName) {
            case "asBoolean": return true;
            case "asType": return DefaultGroovyMethods.asType (object, (Class<?>)originalArguments[0]);
            default:
                // Try looking through known methods "holders"
                Tuple2<Boolean, Object> methodValue = lookUpMethod(methodName, originalArguments);
                if (Boolean.TRUE.equals(methodValue.getV1())) return methodValue.getV2();

                // Otherwise, try default behaviour
                return super.invokeMethod(sender, object, methodName, originalArguments, isCallToSuper, fromInsideClass);
        }

    }

    @Override
    public Object getProperty(Object instance, String propertyName) {
        switch (propertyName) {
            case "resolvedBy":
                return broadcastResponse.getResolvedBy();
            case "response":
                return instance;
            default:
                // Try looking for known properties "holders"
                Tuple2<Boolean, Object> propertyValue = lookProperty(propertyName);
                if(Boolean.TRUE.equals(propertyValue.getV1())) return propertyValue.getV2();

                // try default behaviour
                return super.getProperty(instance, propertyName);
        }
    }

    @Override
    public void setProperty(Object object, String property, Object newValue) {

        // Try setting value to default
        if (defaultsMetaClass != null && setProperty(property, newValue, defaultObject, defaultsMetaClass))
            return;

        // Try setting to response
        // IMPORTANT: Return value will not be processed since:
        //                   if "false", proceed with "shell",
        //                   if "true", need to update "shell" also.
        setProperty(property, newValue, broadcastResponse.getResponse(), responseMetaClass);

        if (setProperty(property, newValue, shell, responseMetaClass))
            return;

        // Try setting property using default behaviour
        super.setProperty(object, property, newValue);
    }

    @Override
    public String toString() {
        return broadcastResponse.toString();
    }

    private Object createShell() {
        this.initialize();

        // Create actual object
        Object shell = InvokerHelper.invokeNoArgumentsConstructorOf(broadcastResponse.getResponse().getClass());

        // Duplicate values from original to shell
        if (shell instanceof Map) {
            ((Map)shell).putAll((Map)broadcastResponse.getResponse());
        }

        if (shell instanceof GroovyObject) {
            for (Map.Entry<String, Object> entry : ((Map<String, Object>) DefaultGroovyMethods.getProperties(broadcastResponse.getResponse())).entrySet()) {
                if ("class".equals(entry.getKey()))
                    continue;

                responseMetaClass.setProperty(shell, entry.getKey(), entry.getValue());
            }
        }

        if(shell instanceof GroovyObject)
            DefaultGroovyMethods.setMetaClass((GroovyObject) shell, this);
        else
            DefaultGroovyMethods.setMetaClass(shell, this);

        return shell;
    }

    private Tuple2<Boolean, Object> lookUpMethod(String methodName, Object[] args) {
        Tuple2<Boolean, Object> methodValue;

        // Try method from default
        if (defaultsMetaClass != null) {
            methodValue = getMethod(methodName, defaultObject, args, defaultsMetaClass);
            if (Boolean.TRUE.equals(methodValue.getV1())) return methodValue;
        }

        // Try method from caller
        methodValue = getMethod(methodName, caller.getDelegate(), args, CALLER_METACLASS);
        if (Boolean.TRUE.equals(methodValue.getV1())) return methodValue;


        // Try method from shell
        methodValue = getMethod(methodName, shell, args, responseMetaClass);
        if (Boolean.TRUE.equals(methodValue.getV1())) return methodValue;

        return new Tuple2<>(false, null);
    }

    private Tuple2<Boolean, Object> getMethod(String methodName, Object owner, Object[] args, MetaClass metaClass) {
        if (owner instanceof Map) {
            Map<?, ?> mapOwner = (Map<?, ?>)owner;
            Object closureObj = mapOwner.get(methodName);
            if (closureObj instanceof Closure) {
                Closure<?> copied = (Closure<?>) ((Closure<?>)closureObj).clone();
                copied.setMetaClass(new ClosureMetaClassWrapper(copied));
                copied.setResolveStrategy(Closure.TO_SELF);
                return new Tuple2<>(true, copied.call(args));
            }
        } else {
            MetaMethod metaMethod = metaClass.getMetaMethod(methodName, args);
            if (metaMethod != null) {
                return new Tuple2<>(true, metaMethod.invoke(owner, args));
            }
        }

        return new Tuple2<>(false, null);
    }

    private Tuple2<Boolean, Object> lookProperty(String propertyName) {
        Tuple2<Boolean, Object> propertyValue;

        // Try getting value from default
        if (defaultsMetaClass != null) {
            propertyValue = getProperty(propertyName, defaultObject, defaultsMetaClass);
            if (Boolean.TRUE.equals(propertyValue.getV1())) return propertyValue;
        }

        // Try getting from caller
        propertyValue = getProperty(propertyName, caller.getDelegate(), CALLER_METACLASS);
        if (Boolean.TRUE.equals(propertyValue.getV1())) return propertyValue;

        // Try getting from response
        propertyValue = getProperty(propertyName, shell, responseMetaClass);
        if (Boolean.TRUE.equals(propertyValue.getV1())) return propertyValue;

        return new Tuple2<>(false ,null);
    }

    private Tuple2<Boolean, Object> getProperty(String propertyName, Object owner, MetaClass metaClass) {
        if (owner instanceof Map){
            Map<?, ?> mapOwner = (Map<?, ?>)owner;
            if (mapOwner.containsKey(propertyName)) {
                return new Tuple2<>(true, mapOwner.get(propertyName));
            }
        } else {
            MetaProperty responseProperty = metaClass.hasProperty(owner, propertyName);
            if (responseProperty != null)
                return new Tuple2<>(true, responseProperty.getProperty(owner));
        }

        return new Tuple2<>(false, null);
    }

    private boolean setProperty(String propertyName, Object newValue, Object owner, MetaClass metaClass) {
        if (owner instanceof Map){
            Map<Object, Object> mapOwner = (Map<Object, Object>)owner;
            if (mapOwner.containsKey(propertyName)) {
                mapOwner.put(propertyName, newValue);
                return true;
            }
        } else {
            MetaProperty responseProperty = metaClass.hasProperty(owner, propertyName);
            if (responseProperty != null) {
                responseProperty.setProperty(owner, newValue);
                return true;
            }
        }

        return false;
    }

    private class ClosureMetaClassWrapper extends DelegatingMetaClass {

        public ClosureMetaClassWrapper(Closure<?> closure) {
            super(closure.getMetaClass());
        }

        @Override
        public Object invokeMethod(Class sender, Object object, String methodName, Object[] originalArguments, boolean isCallToSuper, boolean fromInsideClass) {
            // Try looking through known methods "holders"
            Tuple2<Boolean, Object> methodValue = lookUpMethod(methodName, originalArguments);
            if (Boolean.TRUE.equals(methodValue.getV1())) return methodValue.getV2();

            // Otherwise, try default behaviour
            return super.invokeMethod(sender, object, methodName, originalArguments, isCallToSuper, fromInsideClass);
        }

        @Override
        public Object getProperty(Object instance, String propertyName) {
            // Try looking for known properties "holders"
            Tuple2<Boolean, Object> propertyValue = lookProperty(propertyName);
            if(Boolean.TRUE.equals(propertyValue.getV1())) return propertyValue.getV2();

            // try default behaviour
            return super.getProperty(instance, propertyName);
        }
    }
}
