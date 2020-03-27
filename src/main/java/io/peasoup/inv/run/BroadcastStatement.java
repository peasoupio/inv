package io.peasoup.inv.run;

import groovy.lang.Closure;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;

import java.util.Map;

public class BroadcastStatement implements Statement {
    public static final Manageable BROADCAST = new Broadcast();
    private Object id;
    private String name;
    private Closure<Map> ready;
    private Inv inv;
    private StatementStatus state = StatementStatus.NOT_PROCESSED;

    public Manageable getMatch() {
        return BROADCAST;
    }

    @Override
    public String toString() {
        return getInv() + " => [BROADCAST] [" + getName() + "] " + DefaultGroovyMethods.toString(getId());
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

    public Closure<Map> getReady() {
        return ready;
    }

    public void setReady(Closure<Map> ready) {
        this.ready = ready;
    }

    public Inv getInv() {
        return inv;
    }

    public void setInv(Inv inv) {
        this.inv = inv;
    }

    public StatementStatus getState() {
        return state;
    }

    public static class Broadcast implements Manageable<BroadcastStatement> {
        public void manage(NetworkValuablePool pool, final BroadcastStatement broadcastStatement) {
            if (pool == null || broadcastStatement == null)
                return;

            // Reset state
            broadcastStatement.state = StatementStatus.NOT_PROCESSED;

            if (pool.isHalting()) // Do nothing if halting
                return;

            Map<Object, BroadcastResponse> channel = pool.getAvailableStatements().get(broadcastStatement.getName());
            Map<Object, BroadcastResponse> staging = pool.getStagingStatements().get(broadcastStatement.getName());

            if (channel.containsKey(broadcastStatement.getId()) || staging.containsKey(broadcastStatement.getId())) {
                Logger.warn(broadcastStatement.getId() + " already broadcasted. Skipped");

                broadcastStatement.state = StatementStatus.ALREADY_BROADCAST;
                return;
            }

            broadcastStatement.state = StatementStatus.SUCCESSFUL;

            Logger.info(broadcastStatement);

            // Staging response
            BroadcastResponse response = createResponse(broadcastStatement);
            staging.putIfAbsent(broadcastStatement.getId(), response);
        }

        private BroadcastResponse createResponse(BroadcastStatement broadcastStatement) {

            Object responseObject = null;
            Closure<Object> defaultClosure = null;

            if (broadcastStatement.getReady() != null) {
                responseObject = broadcastStatement.getReady().call();

                if (responseObject != null) {
                    // Shorten hook
                    Object defaultResponseHook = BroadcastResponseInvoker.tryInvokeMethod(responseObject, BroadcastResponse.DEFAULT_RESPONSE_HOOK_SHORT, null);

                    // Normal hook
                    if (defaultResponseHook == null)
                        defaultResponseHook = BroadcastResponseInvoker.tryInvokeMethod(responseObject, BroadcastResponse.DEFAULT_RESPONSE_HOOK, null);

                    if (defaultResponseHook instanceof Closure)
                        defaultClosure = (Closure<Object>) defaultResponseHook;
                }
            }

            // Staging response
            return new BroadcastResponse(
                    broadcastStatement.getInv().getName(),
                    responseObject,
                    defaultClosure);
        }

    }
}
