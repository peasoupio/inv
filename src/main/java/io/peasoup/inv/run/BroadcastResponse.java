package io.peasoup.inv.run;

import groovy.lang.Closure;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import spark.utils.StringUtils;

public class BroadcastResponse {

    public static final String DEFAULT_RESPONSE_HOOK  = "onDefault";
    public static final String DEFAULT_RESPONSE_HOOK_SHORT  = "$";

    private final String resolvedBy;
    private final Object response;
    private final Closure<Object> defaultClosure;

    BroadcastResponse(String resolvedBy, Object response, Closure<Object> defaultClosure) {
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

    public Closure<Object> getDefaultClosure() {
        return defaultClosure;
    }

    @Override
    public String toString() {
        return DefaultGroovyMethods.toString(response);
    }
}
