package io.peasoup.inv.run;

import groovy.lang.Closure;
import io.peasoup.inv.Logger;
import io.peasoup.inv.io.FileUtils;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.apache.commons.lang.StringUtils;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.codehaus.groovy.runtime.HandleMetaClass;
import org.codehaus.groovy.runtime.StringGroovyMethods;

import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

@Getter
public class Inv {

    private final Context context;

    private final Digestion digestionSummary;
    private final InvDescriptor delegate;
    private final InvDescriptor.Properties properties;

    private String name;
    private String path;
    private String markdown;
    private Map<String, String> tags;

    @Accessors(fluent = true)
    private Boolean isTail;
    @Accessors(fluent = true)
    private Boolean isPop;

    private Closure<Object> ready;
    private final Queue<Statement> remainingStatements;
    private final Queue<Step> steps;
    private final Queue<WhenData> whens;

    private final Queue<Statement> totalStatements;
    private int stepCount;

    private Inv(Context context) {
        if (context == null) throw new IllegalArgumentException("Context is required");

        this.context = context;

        this.digestionSummary  = new Digestion();
        this.properties = new InvDescriptor.Properties(context.getBaseFilename());
        this.delegate = new InvDescriptor(this.properties);

        this.remainingStatements = new LinkedBlockingQueue<>();
        this.steps = new LinkedBlockingQueue<>();
        this.whens = new LinkedBlockingQueue<>();

        this.totalStatements = new LinkedBlockingQueue<>();
    }

    /**
     * Dump and process delegate data.
     * Delegate data is resetted.
     * @return True if something was dumped, otherwise false
     */
    public synchronized boolean dumpDelegate() {
        initalizeProperties();

        boolean dumpedSomething = context.pool.include(this);

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
        for(Closure<Object> body : properties.getSteps()) {
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

    /**
     * Initialize properties from latest dump
     */
    private void initalizeProperties() {
        if (StringUtils.isEmpty(name)) name = delegate.getName();
        if (StringUtils.isEmpty(path)) path = delegate.getPath();

        if (StringUtils.isEmpty(name))
            throw new IllegalStateException("Name is required");

        if (StringUtils.isEmpty(path))
            throw new IllegalStateException("Path is required");

        if (markdown == null) markdown = properties.getMarkdown();
        if (ready == null) ready = properties.getReady();
        if (isTail == null) isTail = properties.isTail();
        if (isPop == null) isPop = properties.isPop();
        if (tags == null) {
            tags = delegate.getTags();

            // Print tags (if exist)
            if (tags != null && !tags.isEmpty())
                Logger.info("[" + name + "]" + " => [TAGS] " + DefaultGroovyMethods.toString(tags));
        }
    }

    /**
     * Adds a property to the delegate metaclass instance.
     * @param propertyName Property name
     * @param value Property value
     */
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
            indigestStatements(currentDigestion);

            // Stops processing if a statement told so
            if (currentDigestion.isInterrupted())
                break;

            // Look for remaining steps
            hasDumpedSomething = indigestSteps();

            // If steps dumped something, repeat loop
            if (hasDumpedSomething)
                continue;

            // Check if any when criteria is met
            hasDumpedSomething = indigestWhensEvent();
        }

        digestionSummary.concat(currentDigestion);
        return currentDigestion;
    }

    /**
     * Ingest require and broadcast statements
     *
     * Digestion is calculated here, but analyzed later in the main digestion cycle (in NetworkValuablePool).
     *
     * @param currentDigestion Current digestion instance
     */
    private void indigestStatements(Digestion currentDigestion) {
        Queue<Statement> statementsLeft = new LinkedList<>(this.remainingStatements);

        while(!statementsLeft.isEmpty() &&
              // Relevant when an unbloating is prevented)
              !currentDigestion.isInterrupted()) {

            Statement statement = statementsLeft.poll();
            if (statement == null)
                break;

            // (try to) manage statement
            statement.getMatch().manage(context.pool, statement);

            // Process results for digestion
            int currentStatus = Digestion.checkStatementResult(context.pool, statement);

            // Update current digestion
            currentDigestion.concat(currentStatus);

            // If interrupted status, quit right away
            if (currentStatus == Digestion.INTERRUPT_STATUS)
                break;
            else {
                // Remove statement if completed
                this.remainingStatements.remove(statement);
            }
        }
    }

    /**
     * Ingest new steps.
     * Conditions:
     *  1. has a remaining step
     *  2. has not (previously dumped something)
     *  3. has no more statements
     *  4. Is not halting
     *
     * @return True if something was digested, otherwise false
     */
    private boolean indigestSteps() {
        boolean hasDumpedSomething = false;

        while (!steps.isEmpty() &&
                !hasDumpedSomething &&
                this.remainingStatements.isEmpty() &&
                !context.pool.isHalting()) {

            // Call next step
            Step step = steps.poll();
            if (step == null)
                break;

            // If the step dumped something, remainingStatements won't be empty and exit loop
            hasDumpedSomething = step.execute();
        }

        return hasDumpedSomething;
    }

    /**
     * Ingest whens event
     * @return True if something was digested, otherwise false
     */
    private boolean indigestWhensEvent() {

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

    @Override
    public String toString() {
        return "[" + name + "]";
    }

    @Getter
    public static class Context {
        public static final String WORKING_DIR = System.getProperty("user.dir");

        private final NetworkValuablePool pool;

        private String defaultName;
        private String defaultPath = WORKING_DIR;
        private String repo;
        private String baseFilename;

        public Context(NetworkValuablePool pool) {
            if (pool == null) throw new IllegalArgumentException("Pool is required");

            this.pool = pool;
        }

        public void setDefaultName(String name) {
            if (StringUtils.isEmpty(name))
                return;

            this.defaultName = name;
        }

        public void setDefaultPath(String path) {
            if (StringUtils.isEmpty(path))
                return;

            this.defaultPath = path;
        }

        public void setRepo(String repo) {
            if (StringUtils.isEmpty(repo))
                return;

            this.repo = repo;
        }

        public void setBaseFilename(String scriptFilename) {
            if (StringUtils.isEmpty(scriptFilename))
                return;

            this.baseFilename = FileUtils.convertUnixPath(scriptFilename);
        }

        public Inv build() {

            Inv inv = new Inv(this);

            // Set name from delegate - needs digestion to apply.
            if (StringUtils.isNotEmpty(defaultName))
                inv.delegate.name(defaultName);

            // Set path from delegate - needs digestion to apply.
            if (StringUtils.isNotEmpty(defaultPath)) {
                inv.delegate.path(defaultPath);
            }

            return inv;
        }
    }

    public static class Digestion {

        // UNBLOATS - REQUIRES - BROADCAST - FAILED
        public static final int UNBLOAT_STATUS =  0x1000;
        public static final int REQUIRE_STATUS = 0x1100;
        public static final int BROADCAST_STATUS = 0x1110;

        public static final int INTERRUPT_STATUS = 0x0001;
        public static final int UNBLOAT_INTERRUPT_STATUS = 0x1001;

        /**
         * Check statement result and get its status
         * @param pool NetworkValuablePool
         * @param statement Statement
         * @return Integer status
         */
        @SuppressWarnings("EqualsBetweenInconvertibleTypes")
        public static int checkStatementResult(NetworkValuablePool pool, Statement statement) {
            if (pool == null)
                throw new IllegalArgumentException("Pool is required");

            if (statement == null)
                throw new IllegalArgumentException("Statement is required");

            // Indicates if statement is blocking others
            if (statement.getState() == StatementStatus.FAILED)
                return INTERRUPT_STATUS;

            // Gets metrics if successful
            if (statement.getState().level >= StatementStatus.SUCCESSFUL.level) {

                // If the statement prevents unbloating, stop processing the remaining statements
                if (pool.preventUnbloating(statement))
                    return UNBLOAT_INTERRUPT_STATUS;

                if (RequireStatement.REQUIRE.equals(statement.getMatch()))
                    return REQUIRE_STATUS;

                if (BroadcastStatement.BROADCAST.equals(statement.getMatch()))
                    return BROADCAST_STATUS;
            }

            // Gets metrics if unbloating
            if (statement.getState() == StatementStatus.UNBLOADTING)
                return UNBLOAT_STATUS;

            return 0;
        }

        private int requires = 0;
        private int broadcasts = 0;
        private int unbloats = 0;
        private boolean interrupt = false;

        /**
         * Concat status
         * @param status Status
         */
        public synchronized void concat(int status) {
            switch(status) {
                case UNBLOAT_STATUS:
                    unbloats++;
                    break;
                case REQUIRE_STATUS:
                    requires++;
                    break;
                case BROADCAST_STATUS:
                    broadcasts++;
                    break;
                case INTERRUPT_STATUS:
                    interrupt = true;
                    break;
                case UNBLOAT_INTERRUPT_STATUS:
                    broadcasts++; interrupt = true;
                    break;
            }
        }

        /**
         * Concat another digestion to this one
         * @param digestion the other digestion
         */
        public synchronized void concat(Digestion digestion) {
            if (digestion == null)
                return;

            this.requires += digestion.getRequires();
            this.broadcasts += digestion.getBroadcasts();
            this.unbloats += digestion.getUnbloats();

            if (digestion.interrupt)
                this.interrupt = true;
        }

        public int getRequires() {
            return requires;
        }

        public int getBroadcasts() {
            return broadcasts;
        }

        public int getUnbloats() {
            return unbloats;
        }

        public boolean isInterrupted() { return interrupt; }
    }
}
