package io.peasoup.inv.run;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class NetworkValuablePoolExecutor {

    private static final int THREAD_COUNT = 4;

    private final NetworkValuablePoolEater eater;
    private final Queue<Inv> stack;
    private final Inv.Digestion cycleDigestion;
    private final BlockingDeque<PoolReport.PoolError> poolErrors;

    private final int threadCount;
    private final ExecutorService invExecutor;
    private final CompletionService<Object> invCompletionService;

    private final AtomicInteger working = new AtomicInteger(0);

    private int count = 0;

    public NetworkValuablePoolExecutor(NetworkValuablePoolEater eater, List<Inv> invs, Inv.Digestion cycleDigestion, BlockingDeque<PoolReport.PoolError> poolErrors) {
        this.eater = eater;
        this.stack = new ConcurrentLinkedQueue<>(invs);
        this.cycleDigestion = cycleDigestion;
        this.poolErrors = poolErrors;

        // Create executor and completion service
        this.threadCount = Math.min(THREAD_COUNT, invs.size());
        this.invExecutor = Executors.newFixedThreadPool(this.threadCount);
        this.invCompletionService = new ExecutorCompletionService<>(invExecutor);
    }

    public void start() {
        // Register threads
        for(int i=0;i<threadCount;i++) {
            schedule();
        }

        // Wait for threads until done
        while(working.get() > 0) {
            try {
                invCompletionService.take().get();
            } catch (Exception e) {
                Logger.error(e);
            }
        }

        this.invExecutor.shutdownNow();
    }

    private void schedule() {
        Logger.system("[EXECUTOR] scheduling: #" + ++count);

        working.incrementAndGet();
        invCompletionService.submit(createWorker());
    }

    private Callable<Object> createWorker() {
        return () -> {
            // Proceed only if INVs are left to process and only if all INVs are stuck
            while(!stack.isEmpty()) {
                Inv toEat = stack.poll();
                if (toEat == null)
                    break;

                NetworkValuablePoolEater.EatenInv eatenInv = eater.eatInv(toEat, poolErrors);

                // If error was caught, check next Inv
                if (eatenInv.hasError())
                    continue;

                // Stage broadcasts
                if (eatenInv.getDigestion().getBroadcasts() > 0)
                    eater.stageBroadcasts();

                // Concat digestion metrics
                cycleDigestion.concat(eatenInv.getDigestion());
            }

            working.decrementAndGet();

            return this;
        };
    }
}
