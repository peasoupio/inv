package io.peasoup.inv.run;

import groovy.lang.Closure;

public class Step {

    private final Inv inv;
    private final Closure body;
    private final Integer index;

    public Step(Inv inv, Closure body, int index) {
        if (inv == null) {
            throw new IllegalArgumentException("Inv is required");
        }

        if (body == null) {
            throw new IllegalArgumentException("Body is required");
        }

        if (index < 0) {
            throw new IllegalArgumentException("Index must be 0 or positive");
        }

        this.inv = inv;
        this.body = body;
        this.index = index;

        Logger.debug( toString() + " [INIT]");
    }

    boolean execute() {
        // Call next step
        Logger.debug( toString() + " [BEFORE]");

        body.setResolveStrategy(Closure.DELEGATE_FIRST);
        body.call(index);

        Logger.debug(toString() + " [END]");

        return inv.dumpDelegate();
    }

    @Override
    public String toString() {
        return "[STEP] inv: " + inv.getName() + ", index: " + index;
    }
}
