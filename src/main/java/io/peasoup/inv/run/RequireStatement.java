package io.peasoup.inv.run;

import groovy.lang.Closure;
import org.apache.commons.lang.StringUtils;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;

import java.util.LinkedHashMap;
import java.util.Map;

public class RequireStatement implements Statement {
    public static final Manageable REQUIRE = new Require();
    private Object id;
    private String name;
    private Boolean unbloatable = false;
    private Boolean defaults = true;
    private Inv inv;
    private String into;
    private Closure resolved;
    private Closure unresolved;
    private StatementStatus state = StatementStatus.NOT_PROCESSED;

    public Manageable getMatch() {
        return REQUIRE;
    }

    @Override
    public String toString() {
        return "[" + getInv().getName() + "] => [REQUIRE] [" + getName() + "] " + DefaultGroovyMethods.toString(getId());
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

    public Closure getResolved() {
        return resolved;
    }

    public void setResolved(Closure resolved) {
        this.resolved = resolved;
    }

    public Closure getUnresolved() {
        return unresolved;
    }

    public void setUnresolved(Closure unresolved) {
        this.unresolved = unresolved;
    }

    public StatementStatus getState() {
        return state;
    }

    public static class Require implements Manageable<RequireStatement> {
        public static void setPropertyToDelegate(Object delegate, String propertyName, Object value) {
            //noinspection GroovyAssignabilityCheck
            DefaultGroovyMethods.invokeMethod(DefaultGroovyMethods.getMetaClass(delegate), "setProperty", new Object[]{propertyName, value});
        }

        public void manage(NetworkValuablePool pool, RequireStatement requireStatement) {
            if (pool == null || requireStatement == null)
                return;

            // Reset state
            requireStatement.state = StatementStatus.NOT_PROCESSED;

            if (pool.isHalting()) // Do nothing if halting
                return;

            // Get broadcast
            Map<Object, BroadcastResponse> channel = pool.getAvailableStatements().get(requireStatement.getName());
            BroadcastResponse broadcastResponse = channel.get(requireStatement.getId());

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
            // Implement variable into NV inv (if defined)
            if (StringUtils.isNotEmpty(requireStatement.getInto()))
                setPropertyToDelegate(
                        requireStatement.getInv().getDelegate(),
                        requireStatement.getInto(),
                        new BroadcastResponseDelegate(broadcastResponse, requireStatement.getInv(), requireStatement.getDefaults()));

            // Sends message to resolved (if defined)
            if (requireStatement.getResolved() != null) {
                requireStatement.getResolved().setDelegate(new BroadcastResponseDelegate(broadcastResponse, requireStatement.getInv(), requireStatement.getDefaults()));
                requireStatement.getResolved().setResolveStrategy(Closure.DELEGATE_FIRST);
                requireStatement.getResolved().call();
            }

            // Check if NV would have dumped something
            requireStatement.getInv().dumpDelegate();
        }

    }
}
