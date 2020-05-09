package io.peasoup.inv.run;

import org.codehaus.groovy.runtime.StackTraceUtils;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class PoolReport {
    private final Queue<Inv> digested = new LinkedList<>();
    private final Queue<PoolError> poolErrors = new LinkedList<>();
    private volatile boolean halted = false;

    private int cycleCount = 0;

    public PoolReport() {

    }

    public PoolReport(List<Inv> digested, Queue<PoolError> poolErrors, Boolean halted) {
        if (digested == null) {
            throw new IllegalArgumentException("Digested collection is required. NOTE: can be empty");
        }
        if (poolErrors == null) {
            throw new IllegalArgumentException("Exceptions collection is required. NOTE: can be empty");
        }

        concat(digested, poolErrors, halted);
    }

    public void eat(PoolReport other) {
        if (other == null) {
            throw new IllegalArgumentException("Other (pool report) is required");
        }

        concat(other.getDigested(), other.getErrors(), other.isHalted());
    }

    public boolean isOk() {
        if (halted) return false;

        return poolErrors.isEmpty();
    }

    public void reset() {
        digested.clear();
        poolErrors.clear();
    }

    private void concat(Collection<Inv> digested, Collection<PoolError> exceptions, boolean halted) {
        if (digested == null) {
            throw new IllegalArgumentException("Digested collection is required. NOTE: can be empty");
        }
        if (exceptions == null) {
            throw new IllegalArgumentException("Exceptions collection is required. NOTE: can be empty");
        }

        this.digested.addAll(digested);
        this.poolErrors.addAll(exceptions);

        // Once halted, can't put it back on
        if (halted) this.halted = true;
    }

    public final Queue<Inv> getDigested() {
        return digested;
    }

    public final Queue<PoolError> getErrors() {
        return poolErrors;
    }

    public boolean isHalted() {
        return halted;
    }

    public int getCycleCount() {
        return cycleCount;
    }

    public void setCycleCount(int cycleCount) {
        this.cycleCount = cycleCount;
    }

    public static class PoolError {
        private final Inv inv;
        private final Throwable throwable;

        PoolError(Inv inv, Throwable throwable) {
            if (inv == null) {
                throw new IllegalArgumentException("Inv is required");
            }

            if (throwable == null) {
                throw new IllegalArgumentException("Throwable is required");
            }

            this.inv = inv;
            this.throwable = StackTraceUtils.sanitize(throwable);
        }

        public Inv getInv() {
            return inv;
        }

        public Throwable getThrowable() {
            return throwable;
        }
    }
}
