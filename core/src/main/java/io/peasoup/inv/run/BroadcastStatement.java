package io.peasoup.inv.run;

import groovy.lang.Closure;
import io.peasoup.inv.Logger;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;

import java.util.Map;

@SuppressWarnings({"rawtypes", "unchecked"})
public class BroadcastStatement implements Statement {
    public static final Broadcast BROADCAST = new Broadcast();
    private Object id;
    private String name;
    private String markdown;
    private Closure<Object> ready;
    private Inv inv;
    protected StatementStatus state = StatementStatus.NOT_PROCESSED;

    public Manageable getMatch() {
        return BROADCAST;
    }

    @Override
    public String getLabel() {
        return "[BROADCAST] [" + getName() + "] " + DefaultGroovyMethods.toString(getId());
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

    public Closure<Object> getReady() {
        return ready;
    }

    public void setReady(Closure<Object> ready) {
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

        public synchronized void manage(NetworkValuablePool pool, BroadcastStatement broadcastStatement) {
            if (pool == null)
                throw new IllegalArgumentException("pool");

            if (broadcastStatement == null)
                throw new IllegalArgumentException("broadcastStatement");

            // Reset state
            broadcastStatement.state = StatementStatus.NOT_PROCESSED;

            if (pool.isHalting()) // Do nothing if halting
                return;

            // Check if already available
            Map<Object, BroadcastResponse> channel = pool.getAvailableStatements().get(broadcastStatement.getName());
            if (alreadyBroadcast(channel, broadcastStatement))
                return;

            // Check if already staged
            Map<Object, BroadcastResponse> staging = pool.getStagingStatements().get(broadcastStatement.getName());
            if (alreadyBroadcast(staging, broadcastStatement))
                return;

            broadcastStatement.state = StatementStatus.SUCCESSFUL;

            Logger.info(broadcastStatement);

            Object id = broadcastStatement.getId();

            // Resolved delayed id
            if (id instanceof Closure) {
                Closure delayedId = (Closure) id;
                delayedId.setResolveStrategy(Closure.DELEGATE_ONLY);
                delayedId.setDelegate(broadcastStatement.getInv().getDelegate());
                id = delayedId.call();
            }

            // Staging response
            BroadcastResponse response = createResponse(broadcastStatement);
            staging.putIfAbsent(id, response);
        }

        @SuppressWarnings("unchecked")
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

        private boolean alreadyBroadcast(Map<Object, BroadcastResponse> statements, BroadcastStatement broadcastStatement) {
            if (!statements.containsKey(broadcastStatement.getId()))
                return false;

            String resolvedBy = statements.get(broadcastStatement.getId()).getResolvedBy();
            Logger.warn(broadcastStatement.toString() + " already broadcasted by [" + resolvedBy + "]. Will be skipped.");

            broadcastStatement.state = StatementStatus.ALREADY_BROADCAST;

            return true;
        }
    }
}
