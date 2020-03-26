package io.peasoup.inv.run;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class NetworkValuablePoolExecutor {

    private final static int THREAD_COUNT = 4;
    private static int count = 0;

    private final NetworkValuablePoolEater eater;
    private final Queue<Inv> stack;
    private final Inv.Digestion cycleDigestion;
    private final BlockingDeque<PoolReport.PoolError> poolErrors;

    private final int threadCount;
    private final ExecutorService invExecutor;
    private final CompletionService invCompletionService;

    private final AtomicInteger stuck = new AtomicInteger(0);
    private final AtomicInteger released = new AtomicInteger(0);
    private final AtomicInteger working = new AtomicInteger(0);

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

    private Callable createWorker() {
        return () -> {
            // Proceed only if INVs are left to process and only if all INVs are stuck
            while(!stack.isEmpty() && stuck.get() < stack.size()) {
                Inv toEat = stack.poll();

                NetworkValuablePoolEater.EatenInv eatenInv = eater.eatInv(toEat, poolErrors);

                // If error was caught, check next Inv
                if (eatenInv.hasError())
                    continue;

                if (!eatenInv.getDigestion().hasDoneSomething())
                    // If INV has done nothing, increment stuck threshold.
                    stuck.incrementAndGet();
                else {
                    // Otherwise, reset to 0
                    stuck.set(0);

                    // Wake other threads if needed
                    if (released.get() > 0) {
                        while (released.getAndDecrement() > 0) {
                            schedule();
                        }
                    }
                }

                // Stage broadcasts
                if (eatenInv.getDigestion().getBroadcasts() > 0)
                    eater.stageBroadcasts();

                // Concat digestion metrics
                cycleDigestion.concat(eatenInv.getDigestion());

                // Put back INV at the end of the stack
                stack.add(toEat);
            }

            released.incrementAndGet();
            working.decrementAndGet();

            return this;
        };
    }
}
