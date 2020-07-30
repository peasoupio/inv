package io.peasoup.inv.run;

import groovy.lang.Closure;
import org.apache.commons.lang.StringUtils;

import java.util.Collection;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public class InvDescriptor {

    /**
     * Sets the default ID for statements.
     */
    public static final String DEFAULT_ID = "undefined";

    private final Properties properties;

    private final String baseFilename;

    protected InvDescriptor(Properties properties, String baseFilename) {
        this.properties = properties;
        this.baseFilename = baseFilename;
    }

    /**
     * Gets the base file location used to create this INV instance
     * @return String representation of the filesystem location
     */
    @SuppressWarnings("squid:S100")
    public String get$0() {
        return baseFilename;
    }

    /**
     * Defines the name of the new INV object.
     * <p>
     * Defaults to Script name.
     *
     * @param name @required @default Current file name. Per example filename "MyInv.groovy" would give "MyInv" and "MyInv/inv.groovy" would result "MyInv"
     */
    public void name(String name) {
        if (StringUtils.isEmpty(name)) {
            throw new IllegalArgumentException("Name is required");
        }

        this.properties.name = name;
    }

    /**
     * Defines the path for this Inv.
     * <p>
     * The parent folder of the physical file is the default value.
     *
     * @param path @optional @default None, value required
     */
    public void path(String path) {
        if (StringUtils.isEmpty(path)) {
            throw new IllegalArgumentException("Path is required");
        }

        this.properties.path = path;
    }

    /**
     * Defines the markdown documentation for this INV
     * @param markdown the markdown string
     */
    public void markdown(String markdown) {
        if (StringUtils.isEmpty(markdown)) {
            throw new IllegalArgumentException("Markdown is required");
        }

        this.properties.markdown = markdown.trim();
    }

    /**
     * Determines if this INV should be in the first position when digestion occurs.
     * <p>
     * By default, it is false.
     *
     * @param value @optional @default None, value required
     */
    public void tail(boolean value) {
        if (value && properties.isPop()) {
            throw new IllegalArgumentException("Can't have both 'tail' and 'pop' set both to true.");
        }

        this.properties.tail = value;
    }

    /**
     * Determines if this INV should be in the last position when digestion occurs.
     * <p>
     * By default, it is false.
     *
     * @param value @optional @default None, value required
     */
    public void pop(boolean value) {
        if (value && properties.isTail()) {
            throw new IllegalArgumentException("Can't have both 'tail' and 'pop' set both to true.");
        }

        this.properties.pop = value;
    }

    /**
     * Defines custom tags to be attached during the whole processing.
     * <p>
     * Tags are synonyms to labels.
     * <p>
     * They enable this INV to a 'when tags' features.
     * <p>
     * Tags are also available as a property.
     * Per example:
     * <pre>
     *     tags(myType: 'type1')
     *     require $inv.Type(tags.myType)
     * </pre>
     *
     * @param tags @optional @default None, value required
     */
    public void tags(Map<String, String> tags) {
        if (tags == null || tags.isEmpty()) {
            throw new IllegalArgumentException("Tags are required and cannot be empty");
        }

        this.properties.tags = tags;
    }

    /**
     * Defines a shorthand broadcast statement.
     * <p>
     * Each broadcast needs a *name* (below it's "MyBroadcastStatement").
     * <p>
     * Also, it allows associating a unique identifier.
     *
     * @param statementDescriptor @optional @default None, the value required
     * @return a new BroadcastDescriptor
     */
    public BroadcastDescriptor broadcast(StatementDescriptor statementDescriptor) {
        if (statementDescriptor == null) throw new IllegalArgumentException("Statement descriptor is required");

        BroadcastStatement broadcastStatement = new BroadcastStatement();

        final Object id = statementDescriptor.getId();
        broadcastStatement.setId(id != null ? id : DEFAULT_ID);
        broadcastStatement.setName(statementDescriptor.getName());

        properties.statements.add(broadcastStatement);

        return new BroadcastDescriptor(broadcastStatement);
    }

    /**
     * Defines a shorthand requirement statement.
     * <p>
     * Each requirement needs a *name* (below it's "MyRequireStatement").
     * <p>
     * Also, it allows associating a unique identifier.
     *
     * @param statementDescriptor @optional @default None, the value required
     * @return a new BroadcastDescriptor
     */
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

    /**
     * Defines a callback when the INV is at the beginning of the sequencing process.
     * <p>
     * @param readyBody @optional @default None, value required
     */
    public void ready(Closure<Object> readyBody) {
        if (readyBody == null) {
            throw new IllegalArgumentException("Ready body is required");
        }

        this.properties.ready = readyBody;
    }

    /**
     * Defines a new step that acts as a "group wait".
     * <p>
     * It is raised only when ALL previous statements (require or broadcast) are met.
     * <p>
     * It is a way to generate dynamic statements using previous statement responses.
     *
     * @param stepBody @optional @default None, value required
     */
    public void step(Closure<Object> stepBody) {
        if (stepBody == null) {
            throw new IllegalArgumentException("Step body is required");
        }

        properties.steps.add(stepBody);
    }

    /**
     * Allows trapping system-wide events about other INV.
     * <p>
     * IMPORTANT: When's event are not considered 'require' links.
     * <p>
     * Callbacks are only fired once (even for 'any' scope).
     * <p>
     * A when's event CAN'T apply for the current INV where it is defined.
     * <p>
     * Per example:
     * <blockquote>
     * <pre>
     *     // Will not work
     *     inv {
     *         tags(my: 'tag')
     *
     *         when any tags (my: 'tag') ...
     *     }
     *     // Will work
     *     inv {
     *         tags(my: 'tag')
     *     }
     *     ...
     *     inv {
     *         when any tags (my: 'tag') ...
     *     }
     * </pre></blockquote>
     *
     * @param scope @optional @default None, value require
     * @return a new WhenType
     */
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

    public Map<String, String> getTags() {
        return this.properties.tags;
    }


    /**
     * @deprecated Check get$() instead. Will be removed in 2021.
     */
    @Deprecated
    public final InvNames getInv() {
        return InvNames.Instance;
    }

    /**
     * Create a new generic StatementDescriptor.
     * <p>
     * INV names are dynamically generated.
     * <p>
     * Per example:
     * <pre>
     *     inv.Something
     *     inv.Else
     *     inv.Even("more)
     * </pre>
     * @return a new StatementDescriptor
     */
    @SuppressWarnings("squid:S100")
    public InvNames get$inv() {
        return InvNames.Instance;
    }

    /**
     * Gets the All scope for a when criteria
     * @return The reference for All scope
     */
    public WhenScope getAll() { return WhenScope.ALL; }

    /**
     * Gets the Any scope for a when criteria
     * @return The reference for Any scope
     */
    public WhenScope getAny() { return WhenScope.ANY; }

    /**
     * Equivalent of "this" keyword.
     * @return The InvDescriptor instance.
     */
    public InvDescriptor getMyself() { return this; }

    /**
     * Keep and hides descriptor current properties
     */
    protected static class Properties {

        private String name;
        private String path;
        private String markdown;
        private Map<String, String> tags;
        private boolean tail;
        private boolean pop;

        private Closure<Object> ready;
        private final Queue<Statement> statements = new LinkedBlockingQueue<>();
        private final Queue<Closure<Object>> steps = new LinkedBlockingQueue<>();
        private final Queue<WhenData> whens = new LinkedBlockingQueue<>();

        protected void reset() {
            ready = null;
            statements.clear();
            steps.clear();
            whens.clear();
        }

        public String getMarkdown() { return markdown; }

        protected boolean isTail() {
            return tail;
        }

        protected boolean isPop() {
            return pop;
        }

        protected Closure<Object> getReady() {
            return ready;
        }

        protected final Collection<Statement> getStatements() {
            return statements;
        }

        protected final Collection<Closure<Object>> getSteps() {
            return steps;
        }

        protected final Collection<WhenData> getWhens() {
            return whens;
        }
    }

}
