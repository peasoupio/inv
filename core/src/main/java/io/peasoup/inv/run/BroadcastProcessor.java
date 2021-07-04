package io.peasoup.inv.run;

import io.peasoup.inv.Logger;

public class BroadcastProcessor implements Statement.Processor<BroadcastStatement> {

    public synchronized void process(NetworkValuablePool pool, BroadcastStatement broadcastStatement) {
        if (pool == null)
            throw new IllegalArgumentException("pool");

        if (broadcastStatement == null)
            throw new IllegalArgumentException("broadcastStatement");

        // Reset state
        broadcastStatement.state = StatementStatus.NOT_PROCESSED;

        if (pool.isHalting()) // Do nothing if halting
            return;

        // Avoid restaging "this" statement
        if (pool.getAvailableMap().exists(broadcastStatement) ||
                pool.getStagingMap().exists(broadcastStatement)) {

            broadcastStatement.state = StatementStatus.ALREADY_BROADCAST;

            return;
        }

        broadcastStatement.state = StatementStatus.SUCCESSFUL;

        Logger.info(broadcastStatement);

        // Staging response
        BroadcastResponse response =  new BroadcastResponse(broadcastStatement);
        pool.getStagingMap().add(broadcastStatement, response);
    }


}
