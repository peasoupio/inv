package io.peasoup.inv.run;

import groovy.lang.Closure;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;

public class WhenData {

    private final WhenScope scope;
    private WhenEvent.Events event;
    private WhenType.Types type;

    private Object value;
    private Closure callback;

    public WhenData(WhenScope scope) {
        if (scope == null) {
            throw new IllegalArgumentException("Scope is required");
        }

        this.scope = scope;
    }

    public boolean isOk() {
        return
            event != null &&
            type != null &&
            value != null &&
            callback != null;
    }

    @Override
    public String toString() {
        return scope.value + " " + type.value + " " + DefaultGroovyMethods.toString(value) + " " + event.value;
    }

    public WhenScope getScope() {
        return scope;
    }

    public WhenEvent.Events getEvent() {
        return event;
    }

    public void setEvent(WhenEvent.Events event) {
        this.event = event;
    }

    public WhenType.Types getType() {
        return type;
    }

    public void setType(WhenType.Types type) {
        this.type = type;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public Closure getCallback() {
        return callback;
    }

    public void setCallback(Closure callback) {
        this.callback = callback;
    }

}
