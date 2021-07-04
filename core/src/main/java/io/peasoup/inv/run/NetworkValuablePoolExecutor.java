package io.peasoup.inv.run;

import io.peasoup.inv.Logger;

import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class NetworkValuablePoolExecutor {

    private static final int THREAD_COUNT = 4;

    private final NetworkValuablePool pool;
    private final NetworkValuablePoolIngester ingester;
    private final Inv.Digestion cycleDigestion;
    private final BlockingQueue<PoolReport.PoolError> poolErrors;

    private final CompletionService<Inv> ingesterService;

    private final Queue<Inv> unexecutedInvs;

    private final AtomicInteger amountOfWork = new AtomicInteger(0);

    public NetworkValuablePoolExecutor(
            NetworkValuablePool networkValuablePool,
            NetworkValuablePoolIngester ingester,
            Inv.Digestion cycleDigestion,
            BlockingQueue<PoolReport.PoolError> poolErrors) {
        this.pool = networkValuablePool;
        this.ingester = ingester;
        this.cycleDigestion = cycleDigestion;
        this.poolErrors = poolErrors;

        // Create executor and completion service
        this.ingesterService = new ExecutorCompletionService<>(
                Executors.newFixedThreadPool(THREAD_COUNT,
                        r -> {
                            Thread t = Executors.defaultThreadFactory().newThread(r);
                            t.setDaemon(true);
                            return t;
                        }));

        this.unexecutedInvs = new ConcurrentLinkedQueue<>(networkValuablePool.sortRemainings());
    }

    public void start() {
        if (unexecutedInvs.isEmpty())
            return;

        // Execute first INV
        execute(unexecutedInvs.poll());

        // Wait for all work to be done
        for (int i=0; i < amountOfWork.get(); i++) {
            try {
                 ingesterService.take().get();
            } catch (Exception e) {
                Logger.error(e);
            }
        }
    }

    /**
     * Execute a INV to be ingested
     * @param toIngest The INV.
     */
    protected void execute(Inv toIngest) {
        if (toIngest == null)
            return;

        amountOfWork.incrementAndGet();
        ingesterService.submit(new IngestWorker(toIngest));
    }

    private class IngestWorker implements Callable<Inv> {

        private final Inv toIngest;

        private IngestWorker(Inv toIngest) {
            this.toIngest = toIngest;
        }

        @Override
        public Inv call() throws Exception {

            // Do not process if already completed.
            // Can occur if it was schedule before being completed
            // by a provider (from the watchlist)
            if (toIngest.isCompleted())
                return toIngest;

            // Eat an INV
            NetworkValuablePoolIngester.IngestedInv ingestedInv = ingester.ingest(toIngest, poolErrors);

            // If an error was caught, check next Inv
            if (ingestedInv.hasError())
                return null;

            // Remove from active (remaining) pool if completed
            pool.removeIfCompleted(toIngest);

            // Concat digestion metrics
            cycleDigestion.concat(ingestedInv.getDigestion());

            // Try to execute INV requiring any of the broadcasted statements.
            if (ingestedInv.getDigestion().getBroadcasted() != null) {

                // Stage broadcasts
                ingester.stageBroadcasts();

                int watcherExecuted = 0;

                // Get watchers and execute them.
                for(Statement statement : ingestedInv.getDigestion().getBroadcasted()) {
                    for(Inv watchingInv : pool.getWatchList().getWatchers(statement)) {
                        execute(watchingInv);

                        watcherExecuted++;
                    }
                }

                if (watcherExecuted > 0)
                    return toIngest;
            }

            // If no broadcast occurred, execute an unprocessed INV.
            Inv next =  unexecutedInvs.poll();
            if (next == null)
                return toIngest;

            // Can't resend itself.
            if (next == toIngest)
                return toIngest;

            // Schedule next INV.
            execute(next);
            return toIngest;
        }
    }
}
