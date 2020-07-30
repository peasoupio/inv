package io.peasoup.inv.run;

import groovy.lang.Closure;
import groovy.lang.DelegatesTo;

public class BroadcastDescriptor {
    private final BroadcastStatement broadcastStatement;

    public BroadcastDescriptor(BroadcastStatement broadcastStatement) {
        if (broadcastStatement == null) {
            throw new IllegalArgumentException("BroadcastStatement is required");
        }

        this.broadcastStatement = broadcastStatement;
    }

    /**
     * Defines the broadcast with a BroadcastUsingDescriptor instance.
     *
     * @param broadcastUsingDescriptor BroadcastUsingDescriptor instance
     * @return BroadcastDescriptor owner reference
     */
    public BroadcastDescriptor using(BroadcastUsingDescriptor broadcastUsingDescriptor) {
        if (broadcastUsingDescriptor == null) throw new IllegalArgumentException("broadcastUsingDescriptor");

        if (broadcastUsingDescriptor.getId() != null)
            broadcastStatement.setId(broadcastUsingDescriptor.getId());

        broadcastStatement.setMarkdown(broadcastUsingDescriptor.getMarkdown());
        broadcastStatement.setReady(broadcastUsingDescriptor.getReady());

        return this;
    }

    /**
     * Defines the broadcast using descriptor using a closure.
     *
     * @param usingBody Closure representation of BroadcastUsingDescriptor
     * @return BroadcastDescriptor owner reference
     */
    public BroadcastDescriptor using(@DelegatesTo(BroadcastUsingDescriptor.class) Closure<Object> usingBody) {
        if (usingBody == null) {
            throw new IllegalArgumentException("Using body is required");
        }

        BroadcastUsingDescriptor broadcastUsingDescriptor = new BroadcastUsingDescriptor();

        usingBody.setResolveStrategy(Closure.DELEGATE_FIRST);
        usingBody.setDelegate(broadcastUsingDescriptor);
        usingBody.call();

        return using(broadcastUsingDescriptor);
    }
}
