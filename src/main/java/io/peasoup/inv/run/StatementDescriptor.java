package io.peasoup.inv.run;

import org.apache.commons.lang.StringUtils;

import java.util.Map;

public class StatementDescriptor {
    private final String name;
    private Object id;

    public StatementDescriptor(String name) {
        if (StringUtils.isEmpty(name)) {
            throw new IllegalArgumentException("Name is required");
        }
        if (!StringUtils.isAlphanumeric(name)) {
            throw new IllegalArgumentException("Name must be an alphanumeric value");
        }

        this.name = name;
    }

    public StatementDescriptor call() {
        return this;
    }

    public StatementDescriptor call(Object id) {
        if (id == null) {
            throw new IllegalArgumentException("Id, as an object, is required");
        }

        this.id = id;

        return this;
    }

    public StatementDescriptor call(Map id) {
        if (id == null) {
            throw new IllegalArgumentException("Id, as a Map, is required");
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
