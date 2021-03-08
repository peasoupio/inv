package io.peasoup.inv.run;

import io.peasoup.inv.Logger;

import java.util.Iterator;
import java.util.Map;
import java.util.Queue;

public class NetworkValuablePoolIngester {

    private final NetworkValuablePool pool;

    private volatile int latestStagedCount = 0;

    public NetworkValuablePoolIngester(NetworkValuablePool pool) {
        this.pool = pool;
    }

    /**
     * Ingest an INV instance.
     * @param inv The INV to eat
     * @param poolErrors Pool errors collection
     * @return A new digestion for this specific INV
     */
    @SuppressWarnings("squid:S1181")
    public IngestedInv ingest(final Inv inv, final Queue<PoolReport.PoolError> poolErrors) {
        Inv.Digestion currentDigest = null;
        boolean hasError = false;

        try {
            currentDigest = inv.digest();
        } catch (Throwable t) {
            poolErrors.add(new PoolReport.PoolError(inv, t));

            // Remove upon errors
            pool.getRemainingInvs().remove(inv);

            // Set error tracker to true
            hasError = true;

            Logger.fail(inv + " caught an error. Report will be displayed on pool termination.");
        }

        return new IngestedInv(currentDigest, hasError);
    }

    /**
     * Batch and add staging broadcasts once to prevent double-broadcasts on the same digest.
     */
    public synchronized void stageBroadcasts() {
        for (Map.Entry<String, Map<Object, BroadcastResponse>> statements : pool.getStagingStatements().entrySet()) {
            Map<Object, BroadcastResponse> inChannel = pool.getAvailableStatements().get(statements.getKey());
            Iterator<Map.Entry<Object, BroadcastResponse>> outChannel = statements.getValue().entrySet().iterator();

            while(outChannel.hasNext()) {

                Map.Entry<Object,BroadcastResponse> response = outChannel.next();
                inChannel.putIfAbsent(response.getKey(), response.getValue());

                outChannel.remove();
            }
        }
    }

    public void printStagedBroadcasts() {
        // Get the pool actual size
        int actualSize = 0;
        for (Map.Entry<String, Map<Object, BroadcastResponse>> statements : pool.getStagingStatements().entrySet()) {
            actualSize += pool.getAvailableStatements().get(statements.getKey()).size();
        }

        int stagedSize = actualSize - latestStagedCount;
        Logger.system("[POOL] available:" + actualSize + " " + ", staged:" +  stagedSize);

        latestStagedCount = actualSize;
    }

    /**
     * Results of eating an IV
     */
    public static class IngestedInv {

        private final Inv.Digestion digestion;
        private final boolean hasError;

        private IngestedInv(Inv.Digestion digestion, boolean hasError) {
            this.digestion = digestion;
            this.hasError = hasError;
        }

        public Inv.Digestion getDigestion() {
            return digestion;
        }

        public boolean hasError() {
            return hasError;
        }
    }
}
