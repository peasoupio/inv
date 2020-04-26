package io.peasoup.inv.run;

import groovy.lang.Closure;
import org.apache.commons.lang.StringUtils;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.codehaus.groovy.runtime.HandleMetaClass;
import org.codehaus.groovy.runtime.StringGroovyMethods;

import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

public class Inv {

    private final Context context;

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
    private final Queue<Step> steps;
    private final Queue<WhenData> whens;

    private final Queue<Statement> totalStatements;
    private int stepCount;

    private Inv(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("Context is required");
        }

        this.context = context;

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

        if (StringUtils.isEmpty(name))
            throw new IllegalStateException("Name is required");

        if (StringUtils.isEmpty(path))
            throw new IllegalStateException("Path is required");

        if (ready == null) ready = properties.getReady();
        if (tail == null) tail = properties.isTail();
        if (pop == null) pop = properties.isPop();
        if (tags == null) {
            tags = delegate.getTags();

            // Print tags (if exist)
            if (tags != null && !tags.isEmpty())
                Logger.info("[" + name + "]" + " => [TAGS] " + DefaultGroovyMethods.toString(tags));
        }

        Boolean dumpedSomething = context.pool.include(this);

        // Transfer Statement(s) from delegate to INV
        for (Statement statement : properties.getStatements()) {
            dumpedSomething = true;

            statement.setInv(this);

            this.totalStatements.add(statement);
            this.remainingStatements.add(statement);

            context.pool.checkAvailability(statement.getName());

            Logger.system("[STATEMENT] " + statement.toString() + " [INIT]");
        }

        // Transfer Step(s) from delegate to INV
        for(Closure body : properties.getSteps()) {
            steps.add(new Step(this, body, stepCount++));
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
    @SuppressWarnings("squid:S135")
    public Digestion digest() {
        if (!context.pool.isDigesting()) {
            throw new IllegalArgumentException("digest() is only callable during its pool digest cycle");
        }

        Digestion currentDigestion = new Digestion();
        boolean checkOnce = true;
        boolean hasDumpedSomething = false;

        // Loop because dump
        while (checkOnce || hasDumpedSomething) {
            // Reset flag
            checkOnce = false;

            // Manage statements
            Digestion latestDigestion = digestStatements();
            currentDigestion.concat(latestDigestion);

            // Stops processing if a statement told so
            if (latestDigestion.isInterrupted())
                break;

            // Look for remaining steps
            hasDumpedSomething = digestSteps();

            // If steps dumped something, repeat loop
            if (hasDumpedSomething)
                continue;

            // Check if any when criteria is met
            hasDumpedSomething = digestWhensEvent();
        }

        digestionSummary.concat(currentDigestion);
        return currentDigestion;
    }

    private Digestion digestStatements() {
        Digestion currentDigestion = new Digestion();
        Queue<Statement> statementsLeft = new LinkedList<>(this.remainingStatements);

        while(!statementsLeft.isEmpty() && !currentDigestion.isInterrupted()) {
            Statement statement = statementsLeft.poll();

            // (try to) manage statement
            statement.getMatch().manage(context.pool, statement);

            // Process results for digestion
            currentDigestion.checkStatementResult(context.pool, statement);

            // Remove statement is completed
            if (!currentDigestion.isInterrupted())
                this.remainingStatements.remove(statement);
        }

        return currentDigestion;
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
    private boolean digestSteps() {
        boolean hasDumpedSomething = false;

        while (!steps.isEmpty() &&
                !hasDumpedSomething &&
                this.remainingStatements.isEmpty() &&
                !context.pool.isHalting()) {

            // Call next step
            Step step = steps.poll();
            // If the step dumped something, remainingStatements won't be empty and exit loop
            hasDumpedSomething = step.execute();
        }

        return hasDumpedSomething;
    }

    private boolean digestWhensEvent() {

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
            boolean processedPositively = whenData.getProcessor().qualify(context.pool, this) > 0;

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

    /**
     * Gets whether if it is completed or not.
     * Completed means: no more statements, steps or when's event remaining.
     * @return true if completed, otherwise false
     */
    boolean isCompleted() {
        return remainingStatements.isEmpty() &&
               steps.isEmpty() &&
               whens.isEmpty();
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

    protected Context getContext() {
        return context;
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

    public final Collection<Step> getSteps() {
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

    @Override
    public String toString() {
        return "[" + name + "]";
    }

    public static class Context {
        public static final String WORKING_DIR = System.getProperty("user.dir");

        private final NetworkValuablePool pool;
        private String defaultName;
        private String defaultPath = WORKING_DIR;
        private String scm;
        private String scriptFilename;

        public Context(NetworkValuablePool pool) {
            if (pool == null) {
                throw new IllegalArgumentException("Pool is required");
            }

            this.pool = pool;
        }

        public Context setDefaultName(String name) {
            if (StringUtils.isEmpty(name))
                return this;

            this.defaultName = name;

            return this;
        }

        public Context setDefaultPath(String path) {
            if (StringUtils.isEmpty(path))
                return this;

            this.defaultPath = path;

            return this;
        }

        public Context setSCM(String scm) {
            if (StringUtils.isEmpty(scm))
                return this;

            this.scm = scm;

            return this;
        }

        public Context setScriptFilename(String scriptFilename) {
            if (StringUtils.isEmpty(scriptFilename))
                return this;

            this.scriptFilename = scriptFilename;

            return this;
        }

        public Inv build() {

            Inv inv = new Inv(this);

            // Set name from delegate - needs digestion to apply.
            if (StringUtils.isNotEmpty(defaultName))
                inv.delegate.name(defaultName);

            // Set path from delegate - needs digestion to apply.
            if (StringUtils.isNotEmpty(defaultPath))
                inv.delegate.path(defaultPath);

            return inv;
        }

        public String getDefaultName() {
            return defaultName;
        }

        public String getDefaultPath() {
            return defaultPath;
        }

        public String getScm() {
            return scm;
        }

        public String getScriptFilename() {
            return scriptFilename;
        }
    }

    public static class Digestion {
        private Integer requires = 0;
        private Integer broadcasts = 0;
        private Integer unbloats = 0;

        private boolean interrupt = false;

        public void checkStatementResult(NetworkValuablePool pool, Statement statement) {
            if (pool == null) {
                throw new IllegalArgumentException("Pool is required");
            }

            if (statement == null) {
                throw new IllegalArgumentException("Statement is required");
            }

            // Indicates if statement is blocking others
            if (statement.getState() == StatementStatus.FAILED)
                interrupt = true;

            // Gets metrics if unbloating
            if (statement.getState() == StatementStatus.SUCCESSFUL) {
                unbloats++;

                // If the statement prevents unbloating, stops processing the remaining statements
                if (pool.preventUnbloating(statement))
                    interrupt = true;
            }

            // Gets metrics if successful
            if (statement.getState().level >= StatementStatus.SUCCESSFUL.level) {
                if (statement.getMatch().equals(RequireStatement.REQUIRE)) {
                    requires++;
                }

                if (statement.getMatch().equals(BroadcastStatement.BROADCAST)) {
                    broadcasts++;
                }
            }
        }

        /**
         * Concat another digestion to this one
         * @param digestion the other digestion
         */
        public synchronized void concat(Digestion digestion) {
            if (digestion == null) {
                return;
            }

            this.requires += digestion.getRequires();
            this.broadcasts += digestion.getBroadcasts();
            this.unbloats += digestion.getUnbloats();

            if (digestion.interrupt)
                this.interrupt = true;
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

        public boolean hasDoneSomething() { return broadcasts > 0 || unbloats > 0; }

        public boolean isInterrupted() { return interrupt; }
    }
}
