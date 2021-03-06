package io.peasoup.inv.run;

import io.peasoup.inv.Logger;

import java.util.Iterator;
import java.util.Map;
import java.util.Queue;

public class NetworkValuablePoolEater {

    private final NetworkValuablePool pool;

    private volatile int latestStagedCount = 0;

    public NetworkValuablePoolEater(NetworkValuablePool pool) {
        this.pool = pool;
    }

    /**
     * Do the actual "eating" for a single INV
     * @param inv The INV to eat
     * @param poolErrors Pool errors collection
     * @return A new digestion for this specific INV
     */
    @SuppressWarnings("squid:S1181")
    public EatenInv eatInv(final Inv inv, final Queue<PoolReport.PoolError> poolErrors) {
        Inv.Digestion currentDigest = new Inv.Digestion();
        boolean hasError = false;

        try {
            currentDigest.concat(inv.digest());
        } catch (Throwable t) {
            poolErrors.add(new PoolReport.PoolError(inv, t));

            // Remove upon errors
            pool.getRemainingInvs().remove(inv);

            // Set error tracker to true
            hasError = true;

            Logger.fail(inv + " caught an error. Report will be displayed on pool termination.");
        }

        return new EatenInv(inv, currentDigest, hasError);
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
    public static class EatenInv {

        private final Inv inv;
        private final Inv.Digestion digestion;
        private final boolean hasError;

        private EatenInv(Inv inv, Inv.Digestion digestion, boolean hasError) {
            this.inv = inv;
            this.digestion = digestion;
            this.hasError = hasError;
        }

        public Inv getInv() {
            return inv;
        }

        public Inv.Digestion getDigestion() {
            return digestion;
        }

        public boolean hasError() {
            return hasError;
        }
    }
}
