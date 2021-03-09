package io.peasoup.inv.run;

import groovy.lang.Closure;
import io.peasoup.inv.Logger;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang.StringUtils;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;

import java.util.LinkedHashMap;
import java.util.Map;

@SuppressWarnings({"rawtypes", "unchecked"})
@Getter
@Setter
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

            // Remove INV of watchlist for this statement
            pool.getWatchList().unwatch(requireStatement);
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
            String intoVariable;

            if (StringUtils.isNotEmpty(requireStatement.getInto()))
                intoVariable = requireStatement.getInto();
            else {
                // Be default, a require statement includes an "into" variable which equals to the
                // require statement name first letter lowercase, preceded by a '$'.
                char[] c = requireStatement.getName().toCharArray();
                c[0] = Character.toLowerCase(c[0]);
                intoVariable = "$" + new String(c);
            }

            Object shell = broadcastResponse;
            if (broadcastResponse.getResponse() != null)
                shell = new BroadcastResponseMetaClass(
                        broadcastResponse,
                        requireStatement.getInv(),
                        requireStatement.getDefaults()).getShell();

            requireStatement.getInv().addProperty(
                    intoVariable,
                    shell);

            // Sends message to resolved (if defined)
            if (requireStatement.getResolved() != null) {
                requireStatement.getResolved().setDelegate(shell);
                requireStatement.getResolved().run();
            }

            // Check if NV would have dumped something
            requireStatement.getInv().dumpDelegate();
        }

    }
}
