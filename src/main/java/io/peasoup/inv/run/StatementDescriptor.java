package io.peasoup.inv.run;

import org.apache.commons.lang.StringUtils;

import java.util.Map;

public class StatementDescriptor {
    private final String name;
    private Object id;

    public StatementDescriptor(String name) {
        assert StringUtils.isNotEmpty(name) : "Name is required";
        assert StringUtils.isAlphanumeric(name) : "Name must be an alphanumeric value";

        this.name = name;
    }

    public StatementDescriptor call() {
        return this;
    }

    public StatementDescriptor call(Object id) {
        assert id != null : "Id, as an object, is required";

        this.id = id;

        return this;
    }

    public StatementDescriptor call(Map id) {
        assert id != null : "Id, as a Map, is required";

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
