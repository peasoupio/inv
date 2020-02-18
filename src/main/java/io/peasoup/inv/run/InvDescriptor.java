package io.peasoup.inv.run;

import groovy.lang.Closure;
import org.apache.commons.lang.StringUtils;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;

import java.util.Collection;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public class InvDescriptor {
    private final InvNames inv = InvNames.Instance;
    private final Queue<Statement> statements = new LinkedBlockingQueue<>();
    private final Queue<Closure> steps = new LinkedBlockingQueue<>();
    private String name;
    private String path;
    private Closure ready;

    public void name(String name) {
        assert StringUtils.isNotEmpty(name) : "Name is required";

        this.name = name;
    }

    public void path(String path) {
        assert StringUtils.isNotEmpty(path) : "Path is required";

        this.path = path;
    }

    public void ready(Closure readyBody) {
        assert readyBody != null : "Ready body is required";

        this.ready = readyBody;
    }

    public BroadcastDescriptor broadcast(StatementDescriptor statementDescriptor) {
        assert statementDescriptor != null : "Statement descriptor is required";

        BroadcastStatement broadcastStatement = new BroadcastStatement();

        final Object id = statementDescriptor.getId();
        broadcastStatement.setId(DefaultGroovyMethods.asBoolean(id) ? id : Statement.DEFAULT_ID);
        broadcastStatement.setName(statementDescriptor.getName());

        DefaultGroovyMethods.leftShift(statements, broadcastStatement);

        return new BroadcastDescriptor(broadcastStatement);
    }

    public RequireDescriptor require(StatementDescriptor statementDescriptor) {
        assert statementDescriptor != null : "Statement descriptor is required";

        RequireStatement requireStatement = new RequireStatement();

        final Object id = statementDescriptor.getId();
        requireStatement.setId(DefaultGroovyMethods.asBoolean(id) ? id : Statement.DEFAULT_ID);
        requireStatement.setName(statementDescriptor.getName());

        DefaultGroovyMethods.leftShift(statements, requireStatement);

        return new RequireDescriptor(requireStatement);
    }

    public void step(Closure stepBody) {
        assert stepBody != null : "Step body is required";

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
