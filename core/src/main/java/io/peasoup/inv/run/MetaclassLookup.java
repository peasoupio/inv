package io.peasoup.inv.run;

import groovy.lang.*;
import lombok.AllArgsConstructor;

import java.util.Map;

/**
 * Gather the logic to look up, invoke and set methods and properties using an object metaclass.
 */
@AllArgsConstructor
public class MetaclassLookup {

    /**
     * The parent metaclass
     */
    private final MetaClass delegator;

    /**
     * The metaclass of "value"
     */
    private final MetaClass valueMetaclass;

    /**
     * The actual object value.
     */
    private final Object value;

    /**
     * Look and invoke the appropriate method from different metaclasses.
     *
     * @param methodName The method name
     * @param args The method arguments
     * @return A tuple indicating if a matching method has been found and a returned value
     */
    public Tuple2<Boolean, Object> lookUpMethod(String methodName, Object[] args) {
        Tuple2<Boolean, Object> methodValue;

        // Try method from value metaclass
        methodValue = getMethod(valueMetaclass, value, methodName, args);
        if (Boolean.TRUE.equals(methodValue.getV1())) return methodValue;

        return new Tuple2<>(false, null);
    }

    /**
     * Look and invoke a method for an owning object and its metaclass.
     * Object may be a Map instance. In that case, a ClosureMetaClassWrapper is used to interpolate
     * the behaviour of a closure inside a metaclass.
     *
     * @param metaClass The owner metaclass
     * @param owner The owner
     * @param methodName The method name
     * @param args The method arguments
     * @return A tuple indicating if a matching method has been found and a returned value
     */
    public Tuple2<Boolean, Object> getMethod(MetaClass metaClass, Object owner, String methodName, Object[] args) {
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

    /**
     * Look and get value of a property from different metaclasses
     * @param propertyName The property name
     * @return A tuple indicating if a matching property has been found and its value
     */
    public Tuple2<Boolean, Object> lookProperty(String propertyName) {
        Tuple2<Boolean, Object> propertyValue;

        // Try getting from response
        propertyValue = getProperty(valueMetaclass, value, propertyName);
        if (Boolean.TRUE.equals(propertyValue.getV1())) return propertyValue;

        return new Tuple2<>(false ,null);
    }

    /**
     * Look and get value of a property of an owning object and its metaclass
     * @param metaClass The metaclass
     * @param owner The owner
     * @param propertyName The property name
     * @return A tuple indicating if a matching property has been found and its value
     */
    public Tuple2<Boolean, Object> getProperty(MetaClass metaClass, Object owner, String propertyName) {
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

    public boolean setProperty(String propertyName, Object newValue, Object owner, MetaClass metaClass) {
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
            return delegator.invokeMethod(sender, object, methodName, originalArguments, isCallToSuper, fromInsideClass);
        }

        @Override
        public Object getProperty(Object instance, String propertyName) {
            return delegator.getProperty(instance, propertyName);
        }
    }
}
