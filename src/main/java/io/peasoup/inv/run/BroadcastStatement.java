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
    private int state = NOT_PROCESSED;

    public Manageable getMatch() {
        return BROADCAST;
    }

    @Override
    public String toString() {
        return "[" + getInv().getName() + "] => [BROADCAST] [" + getName() + "] " + DefaultGroovyMethods.toString(getId());
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

    public int getState() {
        return state;
    }

    public static class Broadcast implements Manageable<BroadcastStatement> {
        public void manage(NetworkValuablePool pool, final BroadcastStatement broadcastValuable) {

            // Reset to make sure NV is fine
            broadcastValuable.state = Statement.NOT_PROCESSED;

            // Do nothing if halting
            if (pool.runningState.equals(NetworkValuablePool.getHALTING())) return;

            Map<Object, BroadcastResponse> channel = pool.getAvailableStatements().get(broadcastValuable.getName());
            Map<Object, BroadcastResponse> staging = pool.getStagingStatements().get(broadcastValuable.getName());

            if (channel.containsKey(broadcastValuable.getId()) || staging.containsKey(broadcastValuable.getId())) {
                Logger.warn(broadcastValuable.getId() + " already broadcasted. Skipped");

                broadcastValuable.state = Statement.ALREADY_BROADCAST;
                return;

            }

            broadcastValuable.state = Statement.SUCCESSFUL;

            Logger.info(broadcastValuable);

            Map responseObject = null;
            Closure<Map> defaultClosure = null;

            if (broadcastValuable.getReady() != null) {
                Object rawReponnse = broadcastValuable.getReady().call();

                if (rawReponnse instanceof Map) {
                    responseObject = (Map) rawReponnse;

                    // Resolve default closure
                    if (responseObject.get("$") instanceof Closure) {
                        defaultClosure = (Closure<Map>) responseObject.get("$");
                        responseObject.remove("$");
                    }
                }
            }

            // Staging response
            BroadcastResponse broadcastResponse = new BroadcastResponse();
            broadcastResponse.setResolvedBy(broadcastValuable.getInv().getName());
            broadcastResponse.setResponse(responseObject);
            broadcastResponse.setDefaultClosure(defaultClosure);

            staging.put(broadcastValuable.getId(), broadcastResponse);
        }

    }
}
