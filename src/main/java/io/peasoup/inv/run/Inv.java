package io.peasoup.inv.run;

import groovy.lang.Closure;
import org.apache.commons.lang.StringUtils;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.codehaus.groovy.runtime.HandleMetaClass;
import org.codehaus.groovy.runtime.StringGroovyMethods;

import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

public class Inv {

    private final NetworkValuablePool pool;

    private final Digestion digestionSummary;
    private final InvDescriptor delegate;
    private final InvDescriptor.Properties properties;

    private String name;
    private String path;
    private Map<String, String> tags;
    private Boolean tail;
    private Boolean pop;


    private Closure ready;
    private final Queue<Statement> remainingStatements;
    private final Queue<Closure> steps;
    private final Queue<WhenData> whens;

    private final Queue<Statement> totalStatements;

    public Inv(NetworkValuablePool pool) {
        if (pool == null) {
            throw new IllegalArgumentException("Pool is required");
        }

        this.pool = pool;

        this.digestionSummary  = new Digestion();
        this.properties = new InvDescriptor.Properties();
        this.delegate = new InvDescriptor(this.properties);

        this.remainingStatements = new LinkedBlockingQueue<>();
        this.steps = new LinkedBlockingQueue<>();
        this.whens = new LinkedBlockingQueue<>();

        this.totalStatements = new LinkedBlockingQueue<>();
    }

    public synchronized boolean dumpDelegate() {
        if (StringUtils.isEmpty(name)) name = delegate.getName();
        if (StringUtils.isEmpty(path)) path = delegate.getPath();

        if (ready == null) ready = properties.getReady();
        if (tail == null) tail = properties.isTail();
        if (pop == null) pop = properties.isPop();
        if (tags == null) tags = delegate.getTags();

        Boolean dumpedSomething = pool.include(this);

        // Transfer Statement(s) from delegate to INV
        for (Statement statement : properties.getStatements()) {
            dumpedSomething = true;

            statement.setInv(this);

            this.totalStatements.add(statement);
            this.remainingStatements.add(statement);

            pool.checkAvailability(statement.getName());
        }

        // Transfer Step(s) from delegate to INV
        if (!properties.getSteps().isEmpty()) {
            steps.addAll(properties.getSteps());
            dumpedSomething = true;
        }

        // Transfer When(s) from delegate to INV
        for(WhenData when : properties.getWhens()) {
            // If not completed, do not process. It will be cleaned during reset()
            if (!when.isOk())
                continue;

            whens.add(when);
        }

        properties.reset();

        return dumpedSomething;
    }

    public synchronized void addProperty(String propertyName, Object value) {
        if (StringUtils.isEmpty(propertyName)) {
            throw new IllegalArgumentException("PropertyName is required");
        }

        HandleMetaClass metaClass = (HandleMetaClass)DefaultGroovyMethods.getMetaClass(delegate);
        metaClass.setProperty(propertyName, value);
    }

    /**
     * Digest this inv.
     * This method is meant to be called during a digest cycle of the pool.
     * This method is also meant to be called synchronously.
     * It allows to match the remaining statements.
     * Steps are also handled here.
     *
     * @return A new digestion instance
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
            // Reset flag
            checkOnce = false;
            List<Statement> toRemove = new ArrayList();

            // Manage statements
            boolean keepGoing = manageStatements(digestion, toRemove);

            // Remove all NV meant to be deleted
            this.remainingStatements.removeAll(toRemove);

            // Stops processing if a  statement told to
            if (!keepGoing) {
                break;
            }

            // Look for remaining steps
            hasDumpedSomething = manageSteps();

            // If steps dumped something, repeat loop
            if (hasDumpedSomething)
                continue;

            // Check if any when criteria is met
            hasDumpedSomething = manageWhens();
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

            // Do not proceed if failed
            if (statement.getState() == StatementStatus.FAILED) {
                break;
            }

            // Indicate statement is done for this INV
            done.add(statement);

            // If the statement prevents unbloating, stops processing the remaining statements
            if (pool.preventUnbloating(statement)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Check for new steps if :
     *  1. has a remaining step
     *  2. has not (previously dumped something)
     *  3. has no more statements
     *  4. Is not halting
     *
     * @return True if something was dumped, otherwise false
     */
    private boolean manageSteps() {
        boolean hasDumpedSomething = false;

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

        return hasDumpedSomething;
    }

    private boolean manageWhens() {

        boolean hasDumpedSomething = false;
        List<WhenData> completedWhenData = new ArrayList<>();

        // Do not process until ALL statements are managed
        if (!this.remainingStatements.isEmpty())
            return false;

        for(WhenData whenData : whens) {

            // Do not process not completed When request
            if (!whenData.isOk())
                continue;

            // Process When data
            boolean processedPositively = whenData.getProcessor().qualify(pool, this) > 0;

            // If processed, raise callback and check for dumps
            if (processedPositively) {
                // Tell this 'WhenData' is completed
                completedWhenData.add(whenData);

                // Raise callback
                if (whenData.raiseCallback(this)) {
                    hasDumpedSomething = true;
                }
            }
        }

        // Remove all completed whens
        whens.removeAll(completedWhenData);

        return hasDumpedSomething;
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

    public Map<String, String> getTags() {
        return tags;
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

    public final Collection<WhenData> getWhens() {
        return whens;
    }

    public boolean isTail() {
        return tail;
    }

    public boolean isPop() {
        return pop;
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
