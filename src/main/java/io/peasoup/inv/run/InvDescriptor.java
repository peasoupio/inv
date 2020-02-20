package io.peasoup.inv.run;

import groovy.lang.Closure;
import org.apache.commons.lang.StringUtils;

import java.util.Collection;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public class InvDescriptor {

    private static final String DEFAULT_ID = "undefined";
    private static final InvNames inv = InvNames.Instance;
    private final Queue<Statement> statements = new LinkedBlockingQueue<>();
    private final Queue<Closure> steps = new LinkedBlockingQueue<>();

    private String name;
    private String path;
    private Closure ready;


    public void name(String name) {
        if (StringUtils.isEmpty(name)) {
            throw new IllegalArgumentException("Name is required");
        }

        this.name = name;
    }

    public void path(String path) {
        if (StringUtils.isEmpty(path)) {
            throw new IllegalArgumentException("Path is required");
        }

        this.path = path;
    }

    public void ready(Closure readyBody) {
        if (readyBody == null) {
            throw new IllegalArgumentException("Ready body is required");
        }

        this.ready = readyBody;
    }

    public BroadcastDescriptor broadcast(StatementDescriptor statementDescriptor) {
        if (statementDescriptor == null) {
            throw new IllegalArgumentException("Statement descriptor is required");
        }

        BroadcastStatement broadcastStatement = new BroadcastStatement();

        final Object id = statementDescriptor.getId();
        broadcastStatement.setId(id != null ? id : DEFAULT_ID);
        broadcastStatement.setName(statementDescriptor.getName());

        statements.add(broadcastStatement);

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

        statements.add(requireStatement);

        return new RequireDescriptor(requireStatement);
    }

    public void step(Closure stepBody) {
        if (stepBody == null) {
            throw new IllegalArgumentException("Step body is required");
        }

        steps.add(stepBody);
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public Closure getReady() {
        return ready;
    }

    public final InvNames getInv() {
        return inv;
    }

    public final Collection<Statement> getStatements() {
        return statements;
    }

    public final Collection<Closure> getSteps() {
        return steps;
    }

    protected void reset() {
        ready = null;
        steps.clear();
        statements.clear();
    }
}
