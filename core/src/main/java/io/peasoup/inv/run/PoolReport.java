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

    /**
     * Create an empty PoolReport
     */
    public PoolReport() {
    }

    /**
     * Create a PoolReport with existing data.
     * @param digested List of "digested" INV's
     * @param poolErrors Queue of PoolErrors
     * @param halted True if the report should indicate the pool must halt, otherwise false.
     */
    public PoolReport(List<Inv> digested, Queue<PoolError> poolErrors, Boolean halted) {
        if (digested == null) {
            throw new IllegalArgumentException("Digested collection is required. NOTE: can be empty");
        }
        if (poolErrors == null) {
            throw new IllegalArgumentException("Exceptions collection is required. NOTE: can be empty");
        }

        concat(digested, poolErrors, halted);
    }

    /**
     * Eats another pool report and keeps it values.
     * Each invocation of this method may alter the PoolReport state.
     * If the "other" pool report "halt" is true, this current pool report will take this value.
     * Thus, bringing "the halting" to the top PoolReport instance.
     * @param other The other PoolReport
     */
    public void eat(PoolReport other) {
        if (other == null) {
            throw new IllegalArgumentException("Other (pool report) is required");
        }

        concat(other.getDigested(), other.getErrors(), other.isHalted());
    }

    /**
     * Indicates whether or not the pool is in an "ok state" and should keep digesting new INV's
     * @return True if pool should keep in digesting, otherwise false
     */
    public boolean isOk() {
        if (halted) return false;

        return poolErrors.isEmpty();
    }

    /**
     * Resets the digested and pool errors data.
     * This method should be used by the "top level" pool report instance only.
     */
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

    /**
     * Gets the completely digested INV objects.
     * If an INV has remaining statements, it is NOT considered digested.
     * @return Queue reference of digested INV objects
     */
    public final Queue<Inv> getDigested() {
        return digested;
    }

    /**
     * Gets the pool errors.
     * Usually, pool errors are exceptions (either throwables or exceptions).
     * @return Queue reference of pool error objects
     */
    public final Queue<PoolError> getErrors() {
        return poolErrors;
    }

    /**
     * Indicates this pool report received an "halting" instruction
     * @return True if halting, otherwise false
     */
    public boolean isHalted() {
        return halted;
    }

    /**
     * Gets the digestion cycle count
     * @return Cycle count integer
     */
    public int getCycleCount() {
        return cycleCount;
    }

    /**
     * Indicates how many digestion cycle occurred
     * @param cycleCount Cycle count integer
     */
    public void setCycleCount(int cycleCount) {
        this.cycleCount = cycleCount;
    }

    public static class PoolError {
        private final Inv inv;
        private final Throwable throwable;

        /**
         * Creates a new Pool Report, linking an INV and a throwable
         * @param inv INV object reference
         * @param throwable Throwable object reference
         */
        public PoolError(Inv inv, Throwable throwable) {
            if (inv == null) {
                throw new IllegalArgumentException("Inv is required");
            }

            if (throwable == null) {
                throw new IllegalArgumentException("Throwable is required");
            }

            this.inv = inv;
            this.throwable = StackTraceUtils.sanitize(throwable);
        }

        /**
         * Gets the INV object reference
         * @return INV object reference
         */
        public Inv getInv() {
            return inv;
        }

        /**
         * Gets the throwable object reference
         * @return Throwable object reference
         */
        public Throwable getThrowable() {
            return throwable;
        }
    }
}
