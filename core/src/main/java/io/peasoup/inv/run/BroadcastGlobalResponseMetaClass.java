package io.peasoup.inv.run;

import groovy.lang.*;
import lombok.Getter;
import org.apache.commons.lang.NotImplementedException;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;

public class BroadcastGlobalResponseMetaClass extends ExpandoMetaClass {

    private final static String PROPERTY_RESPONSE = "response";
    private final static String PROPERTY_RESOLVED_BY = "resolvedBy";

    private final static String METHOD_AS_BOOLEAN = "asBoolean";
    private final static String METHOD_AS_TYPE = "asType";

    @Getter
    private final Object globalResponse;

    private final BroadcastResponse broadcastResponse;
    private final MetaClass globalResponseMetaClass;


    private final MetaclassLookup reflector;

    /**
     * Cast Response into a thread-safe delegate.
     *
     * @param broadcastResponse the response to delegate
     * @param globalResponse the global response
     */
    BroadcastGlobalResponseMetaClass(BroadcastResponse broadcastResponse, Object globalResponse) {
        super(globalResponse.getClass());

        this.broadcastResponse = broadcastResponse;
        this.globalResponseMetaClass = DefaultGroovyMethods.getMetaClass(globalResponse);
        this.globalResponse = globalResponse;

        this.reflector = new MetaclassLookup(this, globalResponseMetaClass, globalResponse);

        this.initialize();

        // Register this as the metaclass of the newshell.
        if (globalResponse instanceof GroovyObject)
            DefaultGroovyMethods.setMetaClass((GroovyObject) globalResponse, this);
        else
            DefaultGroovyMethods.setMetaClass(globalResponse, this);
    }

    @Override
    public Object invokeMethod(Class sender, Object object, String methodName, Object[] originalArguments, boolean isCallToSuper, boolean fromInsideClass) {
        switch (methodName) {
            case METHOD_AS_BOOLEAN: return true;
            case METHOD_AS_TYPE: return DefaultGroovyMethods.asType (object, (Class<?>)originalArguments[0]);
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

        if (reflector.setProperty(property, newValue, globalResponse, globalResponseMetaClass))
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
