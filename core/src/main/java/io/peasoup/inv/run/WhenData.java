package io.peasoup.inv.run;

import groovy.lang.Closure;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;

public class WhenData {

    private final WhenScope scope;
    private WhenEvent.Events event;
    private WhenType.TypeProcessor processor;

    private Object value;
    private Closure<Object> callback;

    public WhenData(WhenScope scope) {
        if (scope == null) {
            throw new IllegalArgumentException("Scope is required");
        }

        this.scope = scope;
    }

    /**
     * @return Determines if this WhenData has all the needed information before any processing
     */
    public boolean isOk() {
        return
            event != null &&
            processor != null &&
            value != null &&
            callback != null;
    }

    /**
     * Raise the callback for a specific INV
     * @param inv Inv object who owns the callback
     * @return True if dumped something, otherwise false
     */
    public boolean raiseCallback(Inv inv) {
        if (inv == null) {
            throw new IllegalArgumentException("Inv is required");
        }

        callback.setResolveStrategy(Closure.DELEGATE_FIRST);
        callback.run();

        return inv.dumpDelegate();
    }

    @Override
    public String toString() {
        return scope.value + " " + processor.toString() + " " + DefaultGroovyMethods.toString(value) + " " + event.value;
    }

    @SuppressWarnings("unused")
    public WhenScope getScope() {
        return scope;
    }

    public WhenEvent.Events getEvent() {
        return event;
    }

    public void setEvent(WhenEvent.Events event) {
        this.event = event;
    }

    public WhenType.TypeProcessor getProcessor() {
        return processor;
    }

    public void setProcessor(WhenType.TypeProcessor processor) {
        this.processor = processor;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }



    public void setCallback(Closure<Object> callback) {
        this.callback = callback;
    }

}
