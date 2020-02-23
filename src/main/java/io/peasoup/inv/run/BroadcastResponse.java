package io.peasoup.inv.run;

import groovy.lang.Closure;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import spark.utils.StringUtils;

import java.util.Map;

public class BroadcastResponse {

    private final String resolvedBy;
    private final Map response;
    private final Closure defaultClosure;

    BroadcastResponse(String resolvedBy, Map response, Closure defaultClosure) {
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

    public Map getResponse() {
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
