package io.peasoup.inv.run;

import groovy.lang.Closure;
import org.apache.commons.lang.StringUtils;

import java.util.Map;

public class StatementDescriptor {

    /**
     * Sets the default ID for statements.
     */
    public static final String DEFAULT_ID = "undefined";

    private final String name;
    private Object id = DEFAULT_ID;

    public StatementDescriptor(String name) {
        if (StringUtils.isEmpty(name)) {
            throw new IllegalArgumentException("Name is required");
        }
        if (!StringUtils.isAlphanumeric(name)) {
            throw new IllegalArgumentException("Name must be an alphanumeric value");
        }

        this.name = name;
    }

    /**
     * Default behaviour. No id is defined.
     * @return the current StatementDescriptor
     */
    public StatementDescriptor call() {
        return this;
    }

    /**
     * Defines a new java.lang.Object id.
     * @param id the id
     * @return the current StatementDescriptor
     */
    public StatementDescriptor call(Object id) {
        if (id == null) {
            throw new IllegalArgumentException("Id, as an object, is required");
        }

        this.id = id;

        return this;
    }

    /**
     * Defines a new java.util.Map id
     * @param id the id
     * @return the current StatementDescriptor
     */
    public StatementDescriptor call(Map id) {
        if (id == null) {
            throw new IllegalArgumentException("Id, as a Map, is required");
        }

        this.id = id;

        return this;
    }

    /**
     * Defines a new groovy.lang.Closure id for delayed resolution
     * @param id the id
     * @return the current StatementDescriptor
     */
    public StatementDescriptor call(Closure id) {
        if (id == null) {
            throw new IllegalArgumentException("Id, as a Closure, is required");
        }

        this.id = id;

        return this;
    }

    public final String getName() {
        return name;
    }

    public Object getId() {
        return id;
    }
}
