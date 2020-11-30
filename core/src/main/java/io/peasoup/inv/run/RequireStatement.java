package io.peasoup.inv.run;

import groovy.lang.Closure;
import io.peasoup.inv.Logger;
import org.apache.commons.lang.StringUtils;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;

import java.util.LinkedHashMap;
import java.util.Map;

@SuppressWarnings({"rawtypes", "unchecked"})
public class RequireStatement implements Statement {
    public static final Require REQUIRE = new Require();
    private Object id;
    private String name;
    private String markdown;
    private Boolean unbloatable = false;
    private Boolean defaults = true;
    private Inv inv;
    private String into;
    private Closure<Object> resolved;
    private Closure<Object> unresolved;
    protected StatementStatus state = StatementStatus.NOT_PROCESSED;

    public Manageable getMatch() {
        return REQUIRE;
    }

    @Override
    public String getLabel() {
        if (Boolean.TRUE.equals(unbloatable))
            return "[UNBLOATABLE] [" + getName() + "] " + DefaultGroovyMethods.toString(getId());

        return "[REQUIRE] [" + getName() + "] " + DefaultGroovyMethods.toString(getId());
    }

    @Override
    public String toString() {
        return getInv() + " => " + getLabel();
    }

    public Object getId() {
        return id;
    }

    public void setId(Object id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMarkdown() {
        return markdown;
    }

    public void setMarkdown(String markdown) {
        this.markdown = markdown;
    }

    public Boolean getUnbloatable() {
        return unbloatable;
    }

    public void setUnbloatable(Boolean unbloatable) {
        this.unbloatable = unbloatable;
    }

    public Boolean getDefaults() {
        return defaults;
    }

    public void setDefaults(Boolean defaults) {
        this.defaults = defaults;
    }

    public Inv getInv() {
        return inv;
    }

    public void setInv(Inv inv) {
        this.inv = inv;
    }

    public String getInto() {
        return into;
    }

    public void setInto(String into) {
        this.into = into;
    }

    public Closure<Object> getResolved() {
        return resolved;
    }

    public void setResolved(Closure<Object> resolved) {
        this.resolved = resolved;
    }

    public Closure<Object> getUnresolved() {
        return unresolved;
    }

    public void setUnresolved(Closure<Object> unresolved) {
        this.unresolved = unresolved;
    }

    public StatementStatus getState() {
        return state;
    }

    public static class Require implements Manageable<RequireStatement> {

        public void manage(NetworkValuablePool pool, RequireStatement requireStatement) {
            if (pool == null)
                throw new IllegalArgumentException("pool");

            if (requireStatement == null)
                throw new IllegalArgumentException("requireStatement");

            // Reset state
            requireStatement.state = StatementStatus.NOT_PROCESSED;

            if (pool.isHalting()) // Do nothing if halting
                return;

            // Get broadcast
            Map<Object, BroadcastResponse> channel = pool.getAvailableStatements().get(requireStatement.getName());

            Object id = requireStatement.getId();

            // Resolved delayed id
            if (id instanceof Closure) {
                Closure delayedId = (Closure)id;
                delayedId.setResolveStrategy(Closure.DELEGATE_ONLY);
                delayedId.setDelegate(requireStatement.getInv().getDelegate());
                id = delayedId.call();
            }

            BroadcastResponse broadcastResponse = channel.get(id);

            if (!isBroadcastAvailable(pool, requireStatement, broadcastResponse))
                return;

            requireStatement.state = StatementStatus.SUCCESSFUL;

            Logger.info(requireStatement);

            // Resolve require statement with broadcast response
            resolveRequire(requireStatement, broadcastResponse);
        }

        private boolean isBroadcastAvailable(NetworkValuablePool pool, RequireStatement requireStatement, BroadcastResponse broadcastResponse) {
            if (broadcastResponse != null)
                return true;

            // By default
            requireStatement.state = StatementStatus.FAILED;

            boolean toUnbloat = false;

            // Did it already unbloated
            if (pool.getUnbloatedStatements().get(requireStatement.getName()).contains(requireStatement.getId())) {
                toUnbloat = true;
            }

            // Does this one unbloats
            if (pool.isUnbloating() && Boolean.TRUE.equals(requireStatement.getUnbloatable())) {
                toUnbloat = true;

                // Cache for later
                pool.getUnbloatedStatements().get(requireStatement.getName()).add(requireStatement.getId());
            }

            if (toUnbloat) {
                requireStatement.state = StatementStatus.UNBLOADTING;
                Logger.info(requireStatement);

                if (requireStatement.getUnresolved() != null) {
                    LinkedHashMap<String, Object> map = new LinkedHashMap<>(3);
                    map.put("name", requireStatement.getName());
                    map.put("id", requireStatement.getId());
                    map.put("owner", requireStatement.getInv().getName());
                    requireStatement.getUnresolved().call(map);
                }
            }

            return false;
        }

        private void resolveRequire(RequireStatement requireStatement, BroadcastResponse broadcastResponse) {

            // Be default, a require statement includes an "into" variable which equals to the
            // require statement name preceded by a '$'.
            String intoVariable = "$" + requireStatement.getName();
            if (StringUtils.isNotEmpty(requireStatement.getInto()))
                intoVariable = requireStatement.getInto();

            requireStatement.getInv().addProperty(
                    intoVariable,
                    new BroadcastResponseDelegate(broadcastResponse, requireStatement.getInv(), requireStatement.getDefaults()));

            // Sends message to resolved (if defined)
            if (requireStatement.getResolved() != null) {
                requireStatement.getResolved().setDelegate(new BroadcastResponseDelegate(broadcastResponse, requireStatement.getInv(), requireStatement.getDefaults()));
                requireStatement.getResolved().run();
            }

            // Check if NV would have dumped something
            requireStatement.getInv().dumpDelegate();
        }

    }
}
