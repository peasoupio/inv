package io.peasoup.inv.run;

import io.peasoup.inv.Logger;

public class RequireProcessor implements Statement.Processor<RequireStatement> {

    public void process(NetworkValuablePool pool, RequireStatement requireStatement) {
        if (pool == null)
            throw new IllegalArgumentException("pool");

        if (requireStatement == null)
            throw new IllegalArgumentException("requireStatement");

        // Reset state
        requireStatement.state = StatementStatus.NOT_PROCESSED;

        if (pool.isHalting()) // Do nothing if halting
            return;

        // Get broadcast
        BroadcastResponse response = pool.getAvailableMap().get(requireStatement);
        if (response == null) {
            // Check if statement is cleanable, otherwise change status to failed.
            if (!canCleanStatement(pool, requireStatement))
                requireStatement.state = StatementStatus.FAILED;

            return;
        }

        requireStatement.state = StatementStatus.SUCCESSFUL;

        Logger.info(requireStatement);

        // Resolve require statement with broadcast response
        requireStatement.resolve(response);

        // Remove INV of watchlist for this statement
        pool.getWatchList().unwatch(requireStatement);
    }

    private boolean canCleanStatement(NetworkValuablePool pool, RequireStatement requireStatement) {
        // By default
        requireStatement.state = StatementStatus.FAILED;

        // Was it already cleaned?
        boolean toClean = pool.getCleanedMap().exists(requireStatement);

        // Is this one clean-able?
        if (!toClean &&
                pool.isCleaning() &&
                Boolean.TRUE.equals(requireStatement.getOptional())) {

            toClean = true;

            // Cache for later
            pool.getCleanedMap().add(requireStatement, null);
        }

        if (toClean) {
            requireStatement.state = StatementStatus.CLEANED;
            Logger.info(requireStatement);

            requireStatement.unresolve();
        }

        return toClean;
    }
}
