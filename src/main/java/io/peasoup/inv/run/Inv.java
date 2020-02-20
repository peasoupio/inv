package io.peasoup.inv.run;

import groovy.lang.Closure;
import org.apache.commons.lang.StringUtils;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.codehaus.groovy.runtime.StringGroovyMethods;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public class Inv {
    private final Digestion digestionSummary = new Digestion();
    private final InvDescriptor delegate = new InvDescriptor();
    private final Queue<Statement> remainingStatements = new LinkedBlockingQueue<>();
    private final Queue<Statement> totalStatements = new LinkedBlockingQueue<>();
    private final Queue<Closure> steps = new LinkedBlockingQueue<>();
    private final NetworkValuablePool pool;
    private String name;
    private String path;
    private Closure ready;

    public Inv(NetworkValuablePool pool) {
        if (pool == null) {
            throw new IllegalArgumentException("Pool is required");
        }

        this.pool = pool;
    }

    public synchronized boolean dumpDelegate() {
        if (StringUtils.isEmpty(name)) name = delegate.getName();
        if (StringUtils.isEmpty(path)) path = delegate.getPath();
        if (ready == null) ready = delegate.getReady();

        Boolean dumpedSomething = pool.include(this);

        if (!delegate.getSteps().isEmpty()) {
            steps.addAll(delegate.getSteps());
            dumpedSomething = true;
        }

        // use for-loop to keep order
        for (Statement statement : delegate.getStatements()) {

            dumpedSomething = true;

            statement.setInv(this);

            this.totalStatements.add(statement);
            this.remainingStatements.add(statement);

            pool.checkAvailability(statement.getName());
        }

        delegate.reset();

        return dumpedSomething;
    }

    /**
     * Digest this inv.
     * This method is meant to be called during a digest cycle of the pool.
     * This method is also meant to be called synchronously.
     * <p>
     * It allows to match the remaining statements.
     * Steps are also handled here.
     *
     * @return
     */
    public synchronized Digestion digest() {
        if (!pool.isDigesting()) {
            throw new IllegalArgumentException("digest() is only callable during its pool digest cycle");
        }

        Digestion digestion = new Digestion();
        boolean checkOnce = true;
        boolean hasDumpedSomething = false;

        // Loop because dump
        while (checkOnce || hasDumpedSomething) {
            // Reset flags
            checkOnce = false;
            hasDumpedSomething = false;

            List<Statement> toRemove = new ArrayList();

            boolean keepGoing = manageStatements(digestion, toRemove);

            // Remove all NV meant to be deleted
            this.remainingStatements.removeAll(toRemove);

            if (!keepGoing) break;

            // Check for new steps if :
            // 1. has a remaining step
            // 2. has not (previously dumped something)
            // 3. has no more statements
            // 4. Is not halting
            while (!steps.isEmpty() &&
                   !hasDumpedSomething &&
                   this.remainingStatements.isEmpty() &&
                   !pool.isHalting()) {

                // Call next step
                Closure step = steps.poll();
                step.setResolveStrategy(Closure.DELEGATE_FIRST);
                step.call();

                // If the step dumped something, remainingStatements won't be empty and exit loop
                hasDumpedSomething = dumpDelegate();
            }
        }

        digestionSummary.concat(digestion);
        return digestion;
    }

    private boolean manageStatements(Digestion currentDigestion, List<Statement> done) {
        // Use fori-loop for speed
        for (Statement statement : this.remainingStatements) {

            // (try to) manage statement
            statement.getMatch().manage(pool, statement);

            // Process results for digestion
            currentDigestion.addResults(statement);

            if (statement.getState() == StatementStatus.FAILED) break;

            done.add(statement);

            if (pool.preventUnbloating(statement)) {
                return false;
            }

        }

        return true;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!StringGroovyMethods.asBoolean(name)) return false;
        if (!DefaultGroovyMethods.asBoolean(o)) return false;
        if (!(o instanceof Inv)) return false;

        Inv invO = DefaultGroovyMethods.asType(o, Inv.class);

        return name.equals(invO.getName());
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

    public final Digestion getDigestionSummary() {
        return digestionSummary;
    }

    public final InvDescriptor getDelegate() {
        return delegate;
    }

    public final Collection<Statement> getRemainingStatements() {
        return remainingStatements;
    }

    public final Collection<Statement> getTotalStatements() {
        return totalStatements;
    }

    public final Collection<Closure> getSteps() {
        return steps;
    }

    public final NetworkValuablePool getPool() {
        return pool;
    }

    public static class Digestion {
        private Integer requires = 0;
        private Integer broadcasts = 0;
        private Integer unbloats = 0;

        public void addResults(Statement statement) {
            if (statement == null) {
                throw new IllegalArgumentException("Statement is required");
            }

            if (statement.getState().level >= StatementStatus.SUCCESSFUL.level) {
                if (statement.getMatch().equals(RequireStatement.REQUIRE)) {
                    requires++;
                }

                if (statement.getMatch().equals(BroadcastStatement.BROADCAST)) {
                    broadcasts++;
                }
            }

            if (statement.getState() == StatementStatus.UNBLOADTING) {
                unbloats++;
            }

        }

        public void concat(Digestion digestion) {
            if (digestion == null) {
                return;
            }

            this.requires += digestion.getRequires();
            this.broadcasts += digestion.getBroadcasts();
            this.unbloats += digestion.getUnbloats();
        }

        public Integer getRequires() {
            return requires;
        }

        public Integer getBroadcasts() {
            return broadcasts;
        }

        public Integer getUnbloats() {
            return unbloats;
        }
    }
}
