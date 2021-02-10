package io.peasoup.inv.run;

import org.apache.commons.lang.StringUtils;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;

public class BroadcastResponse {

    public static final String DEFAULT_RESPONSE_HOOK  = "$default";
    public static final String DEFAULT_RESPONSE_HOOK_SHORT  = "$";

    private final String resolvedBy;
    private final Object response;

    BroadcastResponse(String resolvedBy, Object response) {
        if (StringUtils.isEmpty(resolvedBy)) {
            throw new IllegalArgumentException("ResolvedBy is required");
        }

        this.resolvedBy = resolvedBy;
        this.response = response;
    }

    public String getResolvedBy() {
        return resolvedBy;
    }

    public Object getResponse() {
        return response;
    }

    @Override
    public String toString() {
        return DefaultGroovyMethods.toString(response);
    }
}
