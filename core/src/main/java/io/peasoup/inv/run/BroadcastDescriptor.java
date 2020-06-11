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

    public BroadcastDescriptor using(@DelegatesTo(BroadcastUsingDescriptor.class) Closure<Object> usingBody) {
        if (usingBody == null) {
            throw new IllegalArgumentException("Using body is required");
        }

        BroadcastUsingDescriptor broadcastUsingDescriptor = new BroadcastUsingDescriptor();

        usingBody.setResolveStrategy(Closure.DELEGATE_FIRST);
        usingBody.setDelegate(broadcastUsingDescriptor);
        usingBody.call();

        if (broadcastUsingDescriptor.getId() != null)
            broadcastStatement.setId(broadcastUsingDescriptor.getId());


        broadcastStatement.setMarkdown(broadcastUsingDescriptor.getMarkdown());
        broadcastStatement.setReady(broadcastUsingDescriptor.getReady());

        return this;
    }
}
