package io.peasoup.inv.run;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;

public class PoolReport {

    /**
     * Gets the pool errors.
     * Usually, pool errors are exceptions (either throwables or exceptions).
     * @return Queue reference of pool error objects
     */
    @Getter private final Queue<PoolError> errors = new LinkedList<>();

    /**
     * Indicates this pool report received an "halting" instruction
     * @return True if halting, otherwise false
     */
    @Getter private volatile boolean halted = false;

    /**
     * Gets the digestion cycle count
     * @return Cycle count integer
     */
    @Getter @Setter private int cycleCount = 0;

    /**
     * Create an empty PoolReport
     */
    public PoolReport() {
    }

    /**
     * Create a PoolReport with existing data.
     * @param errors Queue of PoolErrors
     * @param halted True if the report should indicate the pool must halt, otherwise false.
     */
    public PoolReport(Queue<PoolError> errors, Boolean halted) {
        if (errors == null) {
            throw new IllegalArgumentException("Exceptions collection is required. NOTE: can be empty");
        }

        concat(errors, halted);
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

        concat(other.getErrors(), other.isHalted());
    }

    /**
     * Indicates whether or not the pool is in an "ok state" and should keep digesting new INV's
     * @return True if pool should keep in digesting, otherwise false
     */
    public boolean isOk() {
        if (halted) return false;

        return errors.isEmpty();
    }

    /**
     * Resets the digested and pool errors data.
     * This method should be used by the "top level" pool report instance only.
     */
    public void reset() {
        errors.clear();
    }

    private void concat(Collection<PoolError> exceptions, boolean halted) {
        this.errors.addAll(exceptions);

        // Once halted, can't put it back on
        if (halted) this.halted = true;
    }

    @AllArgsConstructor
    public static class PoolError {

        /**
         * Gets the INV object reference
         * @return INV object reference
         */
        @Getter @NonNull private final Inv inv;

        /**
         * Gets the throwable object reference
         * @return Throwable object reference
         */
        @Getter @NonNull private final Throwable throwable;
    }
}
