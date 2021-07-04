package io.peasoup.inv.run;

import groovy.lang.Closure;

/**
 * Resolves delayed ID
 */
public class IdResolver {

    private IdResolver() {
        // private ctor
    }

    /**
     * Resolves a delayed ID (if conditions are met) for a statement.
     * A delayed ID means a statement has an ID with the groovy.lang.Closure type.
     *
     * @param statement The statement
     * @return The resolved delayed ID
     */
    public static Object resolve(Statement statement) {
        if (statement == null)
            throw new IllegalArgumentException("statement");

        // Get ID
        Object id = statement.getId();

        // // Resolve if ID is a closure (delayed)
        if (id instanceof Closure) {
            Closure<?> delayedId = (Closure<?>)id;
            delayedId.setResolveStrategy(Closure.DELEGATE_ONLY);
            delayedId.setDelegate(statement.getInv().getDelegate());
            id = delayedId.call();
        }

        return id;
    }
}
