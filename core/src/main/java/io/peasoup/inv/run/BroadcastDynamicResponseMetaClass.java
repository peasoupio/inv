package io.peasoup.inv.run;

import groovy.lang.*;
import lombok.Getter;
import org.apache.commons.lang.NotImplementedException;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;

public class BroadcastDynamicResponseMetaClass extends ExpandoMetaClass {

    private final static String PROPERTY_CALLER = "caller";
    private final static String PROPERTY_RESPONSE = "response";
    private final static String PROPERTY_RESOLVED_BY = "resolvedBy";

    private final static String METHOD_AS_BOOLEAN = "asBoolean";
    private final static String METHOD_AS_TYPE = "asType";
    private final static String METHOD_CALLER = "getCaller";

    @Getter
    private final BroadcastResponseShell shell;

    private final RequireStatement caller;
    private final BroadcastResponse broadcastResponse;
    private final MetaClass dyanmicResponseMetaClass;

    private final MetaclassLookup reflector;

    /**
     * Cast Response into a thread-safe delegate.
     * It also expands response closures at top level with the calling INV delegate (path, name, etc)
     *
     * @param caller the actual inv requiring this response
     * @param broadcastResponse the response to delegate
     * @param dynamicResponse the dynamic response
     */
    BroadcastDynamicResponseMetaClass(RequireStatement caller, BroadcastResponse broadcastResponse, Object dynamicResponse) {
        super(dynamicResponse.getClass());

        if (caller == null) {
            throw new IllegalArgumentException("Caller is required");
        }

        this.broadcastResponse = broadcastResponse;
        this.caller = caller;

        this.dyanmicResponseMetaClass = DefaultGroovyMethods.getMetaClass(dynamicResponse);
        this.shell = new BroadcastResponseShell(broadcastResponse, dyanmicResponseMetaClass, dynamicResponse);

        this.reflector = new MetaclassLookup(
                this,
                dyanmicResponseMetaClass,
                this.shell.getDynamicResponse());

        this.initialize();

        // Register this as the metaclass of the newshell.
        if (this.shell.getDynamicResponse() instanceof GroovyObject)
            DefaultGroovyMethods.setMetaClass((GroovyObject) this.shell.getDynamicResponse(), this);
        else
            DefaultGroovyMethods.setMetaClass(this.shell.getDynamicResponse(), this);
    }

    @Override
    public Object invokeMethod(Class sender, Object object, String methodName, Object[] originalArguments, boolean isCallToSuper, boolean fromInsideClass) {
        switch (methodName) {
            case METHOD_AS_BOOLEAN: return true;
            case METHOD_AS_TYPE: return DefaultGroovyMethods.asType (object, (Class<?>)originalArguments[0]);
            case METHOD_CALLER: return caller.getInv().getDelegate();
            default:
                // Try looking through known methods "holders"
                Tuple2<Boolean, Object> methodValue = reflector.lookUpMethod(methodName, originalArguments);
                if (Boolean.TRUE.equals(methodValue.getV1())) return methodValue.getV2();

                // Otherwise, try default behaviour
                return super.invokeMethod(sender, object, methodName, originalArguments, isCallToSuper, fromInsideClass);
        }

    }

    @Override
    public Object getProperty(Object instance, String propertyName) {
        switch (propertyName) {
            case PROPERTY_RESOLVED_BY: return broadcastResponse.getResolvedBy();
            case PROPERTY_RESPONSE: return instance;
            case PROPERTY_CALLER: return caller.getInv().getDelegate();
            default:
                // Try looking for known properties "holders"
                Tuple2<Boolean, Object> propertyValue = reflector.lookProperty(propertyName);
                if(Boolean.TRUE.equals(propertyValue.getV1())) return propertyValue.getV2();

                // try default behaviour
                return super.getProperty(instance, propertyName);
        }
    }

    @Override
    public void setProperty(Object object, String property, Object newValue) {
        // If value is readonly
        if (this.shell.isReadOnly())
            return;

        if (reflector.setProperty(property, newValue, this.shell.getDynamicResponse(), dyanmicResponseMetaClass))
            return;

        // Try to set the property using the default behaviour
        super.setProperty(object, property, newValue);
    }

    @Override
    public MetaProperty hasProperty(Object obj, String name) {

        // Try looking for known properties "holders"
        Tuple2<Boolean, Object> propertyValue = reflector.lookProperty(name);
        if(Boolean.TRUE.equals(propertyValue.getV1()))
            // Return a mocked property
            return new MetaProperty(name, propertyValue.getV2().getClass()) {
                @Override
                public Object getProperty(Object object) {
                    return propertyValue.getV2();
                }

                @Override
                public void setProperty(Object object, Object newValue) {
                    throw new NotImplementedException();
                }
            };

        return super.hasProperty(obj, name);
    }

    @Override
    public String toString() {
        return broadcastResponse.toString();
    }
}
