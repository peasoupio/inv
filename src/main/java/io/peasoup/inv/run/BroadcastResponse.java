package io.peasoup.inv.run;

import groovy.lang.Closure;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.codehaus.groovy.runtime.InvokerHelper;
import spark.utils.StringUtils;

import java.util.Map;

public class BroadcastResponse {

    public static final String DEFAULT_RESPONSE_HOOK  = "onDefault";
    public static final String DEFAULT_RESPONSE_HOOK_SHORT  = "$";

    public static Object tryInvokeMethod(Object self, String methodName, Object args) {
        if (self == null)
            return null;

        if (StringUtils.isEmpty(methodName))
            return null;

        Object matchedValue = null;

        if (self instanceof Map) {
            Map selfMap = (Map)self;

            if (!selfMap.containsKey(methodName))
                return null;

            matchedValue = selfMap.get(methodName);
        } else if (!InvokerHelper.getMetaClass(self).respondsTo(self, methodName).isEmpty()) {
            matchedValue = InvokerHelper.invokeMethod(self, methodName, args);
        }

        return matchedValue;
    }

    public static Object tryInvokeProperty(Object self, String propertyName) {
        if (self == null)
            return null;

        if (StringUtils.isEmpty(propertyName))
            return null;

        Object matchedValue = null;

        if (self instanceof Map) {
            Map selfMap = (Map)self;

            if (!selfMap.containsKey(propertyName))
                return null;

            matchedValue = selfMap.get(propertyName);
        } else if (InvokerHelper.getMetaClass(self).respondsTo(self, propertyName) != null) {
            matchedValue = InvokerHelper.getProperty(self, propertyName);
        }

        return matchedValue;
    }

    private final String resolvedBy;
    private final Object response;
    private final Closure defaultClosure;

    BroadcastResponse(String resolvedBy, Object response, Closure defaultClosure) {
        if (StringUtils.isEmpty(resolvedBy)) {
            throw new IllegalArgumentException("ResolvedBy is required");
        }

        this.resolvedBy = resolvedBy;
        this.response = response;
        this.defaultClosure = defaultClosure;
    }

    public String getResolvedBy() {
        return resolvedBy;
    }

    public Object getResponse() {
        return response;
    }

    public Closure getDefaultClosure() {
        return defaultClosure;
    }

    @Override
    public String toString() {
        return DefaultGroovyMethods.toString(response);
    }
}
