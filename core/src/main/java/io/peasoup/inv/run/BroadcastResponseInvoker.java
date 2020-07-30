package io.peasoup.inv.run;

import org.apache.commons.lang.StringUtils;
import org.codehaus.groovy.runtime.InvokerHelper;

import java.util.Map;

public class BroadcastResponseInvoker {

    private BroadcastResponseInvoker() {

    }

    @SuppressWarnings("unchecked")
    protected static Object tryInvokeMethod(Object self, String methodName, Object args) {
        if (self == null)
            return null;

        if (StringUtils.isEmpty(methodName))
            return null;

        Object matchedValue = null;

        if (self instanceof Map) {
            Map<String,Object> selfMap = (Map<String,Object>)self;

            if (!selfMap.containsKey(methodName))
                return null;

            matchedValue = selfMap.get(methodName);
        } else if (!InvokerHelper.getMetaClass(self).respondsTo(self, methodName).isEmpty()) {
            matchedValue = InvokerHelper.invokeMethod(self, methodName, args);
        }

        return matchedValue;
    }

    @SuppressWarnings("unchecked")
    protected static Object tryInvokeProperty(Object self, String propertyName) {
        if (self == null)
            return null;

        if (StringUtils.isEmpty(propertyName))
            return null;

        Object matchedValue = null;

        if (self instanceof Map) {
            Map<String,Object> selfMap = (Map<String,Object>)self;

            if (!selfMap.containsKey(propertyName))
                return null;

            matchedValue = selfMap.get(propertyName);
        } else if (InvokerHelper.getMetaClass(self).respondsTo(self, propertyName) != null) {
            matchedValue = InvokerHelper.getProperty(self, propertyName);
        }

        return matchedValue;
    }

    @SuppressWarnings("unchecked")
    protected static boolean tryDefineProperty(Object self, String propertyName, Object newValue) {
        if (self == null)
            return false;

        if (StringUtils.isEmpty(propertyName))
            return false;

        if (self instanceof Map) {
            Map<String,Object> selfMap = (Map<String,Object>)self;

            if (!selfMap.containsKey(propertyName))
                return false;

            selfMap.put(propertyName, newValue);
        } else if (InvokerHelper.getMetaClass(self).respondsTo(self, propertyName) != null) {
            InvokerHelper.setProperty(self, propertyName, newValue);
        }

        return true;
    }
}
