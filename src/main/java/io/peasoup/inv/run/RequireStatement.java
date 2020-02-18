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
    private int state = NOT_PROCESSED;

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

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public static class Require implements Manageable<RequireStatement> {
        public static void setPropertyToDelegate(Object delegate, String propertyName, Object value) {
            //noinspection GroovyAssignabilityCheck
            DefaultGroovyMethods.invokeMethod(DefaultGroovyMethods.getMetaClass(delegate), "setProperty", new Object[]{propertyName, value});
        }

        public void manage(NetworkValuablePool pool, RequireStatement requireValuable) {

            // Reset to make sure NV is fine
            requireValuable.setState(RequireStatement.NOT_PROCESSED);

            // Do nothing if halting
            if (pool.runningState.equals(NetworkValuablePool.getHALTING())) return;


            // Get broadcast
            Map<Object, BroadcastResponse> channel = pool.getAvailableStatements().get(requireValuable.getName());
            BroadcastResponse broadcast = channel.get(requireValuable.getId());

            if (broadcast == null) {

                // By default
                requireValuable.setState(RequireStatement.FAILED);

                boolean toUnbloat = false;

                // Did it already unbloated
                if (pool.getUnbloatedStatements().get(requireValuable.getName()).contains(requireValuable.getId())) {
                    toUnbloat = true;
                }


                // Does this one unbloats
                if (pool.runningState.equals(NetworkValuablePool.getUNBLOATING()) && requireValuable.getUnbloatable()) {
                    toUnbloat = true;

                    // Cache for later
                    pool.getUnbloatedStatements().get(requireValuable.getName()).add(requireValuable.getId());
                }


                if (toUnbloat) {

                    requireValuable.setState(RequireStatement.UNBLOADTING);
                    Logger.info(requireValuable);

                    if (requireValuable.getUnresolved() != null) {
                        LinkedHashMap<String, Object> map = new LinkedHashMap<>(3);
                        map.put("name", requireValuable.getName());
                        map.put("id", requireValuable.getId());
                        map.put("owner", requireValuable.getInv().getName());
                        requireValuable.getUnresolved().call(map);
                    }
                }

                return;
            }

            requireValuable.setState(RequireStatement.SUCCESSFUL);

            Logger.info(requireValuable);

            // Implement variable into NV inv (if defined)
            if (StringUtils.isNotEmpty(requireValuable.getInto()))
                setPropertyToDelegate(
                        requireValuable.getInv().getDelegate(),
                        requireValuable.getInto(),
                        new BroadcastResponseDelegate(broadcast, requireValuable.getInv(), requireValuable.getDefaults()));

            // Sends message to resolved (if defined)
            if (requireValuable.getResolved() != null) {
                requireValuable.getResolved().setDelegate(new BroadcastResponseDelegate(broadcast, requireValuable.getInv(), requireValuable.getDefaults()));
                requireValuable.getResolved().setResolveStrategy(Closure.DELEGATE_FIRST);
                requireValuable.getResolved().call();
            }

            // Check if NV would have dumped something
            requireValuable.getInv().dumpDelegate();
        }

    }
}
