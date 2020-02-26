package io.peasoup.inv.run;

import groovy.lang.Closure;
import org.apache.commons.lang.StringUtils;

import java.util.Collection;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public class InvDescriptor {

    private static final String DEFAULT_ID = "undefined";
    private static final InvNames inv = InvNames.Instance;

    private final Properties properties;

    protected InvDescriptor(Properties properties) {
        if (properties == null) {
            throw new IllegalArgumentException("Temporaries is required");
        }

        this.properties = properties;
    }


    public void name(String name) {
        if (StringUtils.isEmpty(name)) {
            throw new IllegalArgumentException("Name is required");
        }

        this.properties.name = name;
    }

    public void path(String path) {
        if (StringUtils.isEmpty(path)) {
            throw new IllegalArgumentException("Path is required");
        }

        this.properties.path = path;
    }

    public void tags(Map<String, String> tags) {
        if (tags == null || tags.isEmpty()) {
            throw new IllegalArgumentException("Tags are required and cannot be empty");
        }

        this.properties.tags = tags;
    }

    public void tail(boolean value) {
        if (value && properties.isPop()) {
            throw new IllegalArgumentException("Can't have both 'tail' and 'pop' set both to true.");
        }

        this.properties.tail = value;
    }

    public void pop(boolean value) {
        if (value && properties.isTail()) {
            throw new IllegalArgumentException("Can't have both 'tail' and 'pop' set both to true.");
        }

        this.properties.pop = value;
    }

    public void ready(Closure readyBody) {
        if (readyBody == null) {
            throw new IllegalArgumentException("Ready body is required");
        }

        this.properties.ready = readyBody;
    }

    public BroadcastDescriptor broadcast(StatementDescriptor statementDescriptor) {
        if (statementDescriptor == null) {
            throw new IllegalArgumentException("Statement descriptor is required");
        }

        BroadcastStatement broadcastStatement = new BroadcastStatement();

        final Object id = statementDescriptor.getId();
        broadcastStatement.setId(id != null ? id : DEFAULT_ID);
        broadcastStatement.setName(statementDescriptor.getName());

        properties.statements.add(broadcastStatement);

        return new BroadcastDescriptor(broadcastStatement);
    }

    public RequireDescriptor require(StatementDescriptor statementDescriptor) {
        if (statementDescriptor == null) {
            throw new IllegalArgumentException("Statement descriptor is required");
        }

        RequireStatement requireStatement = new RequireStatement();

        final Object id = statementDescriptor.getId();
        requireStatement.setId(id != null ? id : DEFAULT_ID);
        requireStatement.setName(statementDescriptor.getName());

        properties.statements.add(requireStatement);

        return new RequireDescriptor(requireStatement);
    }

    public void step(Closure stepBody) {
        if (stepBody == null) {
            throw new IllegalArgumentException("Step body is required");
        }

        properties.steps.add(stepBody);
    }

    public WhenType when(WhenScope scope) {
        if (scope == null) {
            throw new IllegalArgumentException("Scope is required");
        }

        // Create WhenData
        WhenData whenData = new WhenData(scope);
        properties.whens.add(whenData);

        return new WhenType(whenData);
    }

    public String getName() {
        return this.properties.name;
    }

    public String getPath() {
        return this.properties.path;
    }

    public Map getTags() {
        return this.properties.tags;
    }

    public final InvNames getInv() {
        return inv;
    }

    public WhenScope getAll() {return WhenScope.All; }

    public WhenScope getAny() {return WhenScope.Any; }

    /**
     * Keep and hides descriptor current properties
     */
    protected static class Properties {

        private String name;
        private String path;
        private Map<String, String> tags;
        private boolean tail;
        private boolean pop;

        private Closure ready;
        private final Queue<Statement> statements = new LinkedBlockingQueue<>();
        private final Queue<Closure> steps = new LinkedBlockingQueue<>();
        private final Queue<WhenData> whens = new LinkedBlockingQueue<>();

        protected void reset() {
            ready = null;
            statements.clear();
            steps.clear();
            whens.clear();
        }

        protected boolean isTail() {
            return tail;
        }

        protected boolean isPop() {
            return pop;
        }

        protected Closure getReady() {
            return ready;
        }

        protected final Collection<Statement> getStatements() {
            return statements;
        }

        protected final Collection<Closure> getSteps() {
            return steps;
        }

        protected final Collection<WhenData> getWhens() {
            return whens;
        }
    }

}
