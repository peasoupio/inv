package io.peasoup.inv.run;

import groovy.lang.Closure;

import java.util.Map;

public class BroadcastResponse {

    public String getResolvedBy() {
        return resolvedBy;
    }

    public void setResolvedBy(String resolvedBy) {
        this.resolvedBy = resolvedBy;
    }

    public Map getResponse() {
        return response;
    }

    public void setResponse(Map response) {
        this.response = response;
    }

    public Closure getDefaultClosure() {
        return defaultClosure;
    }

    public void setDefaultClosure(Closure defaultClosure) {
        this.defaultClosure = defaultClosure;
    }

    private String resolvedBy;
    private Map response;
    private Closure defaultClosure;
}
