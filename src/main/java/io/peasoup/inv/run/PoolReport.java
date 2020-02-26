package io.peasoup.inv.run;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class PoolReport {
    private final Queue<Inv> digested = new LinkedList<>();
    private final Queue<PoolError> poolErrors = new LinkedList<>();
    private volatile boolean halted = false;

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

    public void printPoolTrace(final NetworkValuablePool pool) {
        if (pool == null) {
            throw new IllegalArgumentException("Pool is required");
        }

        Logger.info("Completed INV(s): " + (pool.getTotalInvs().size() - pool.getRemainingInvs().size()));
        Logger.info("Incompleted INV(s): " + pool.getRemainingInvs().size());

        StringBuilder output = new StringBuilder();
        PoolStateTree tree = new PoolStateTree(pool);

        if (!pool.getRemainingInvs().isEmpty()) {

            output.append("Incompleted INV(s) details: " + System.lineSeparator());

            for (Inv remaining : tree.sortRemainingByRequireWeight()) {
                boolean noMoreStatement = remaining.getRemainingStatements().isEmpty();

                if (noMoreStatement) {
                    output.append("- " + remaining.getName() + " has no statement left. Look below for exception(s) are when criteria(s) not met." + System.lineSeparator());
                }

                if (!remaining.getWhens().isEmpty()) {
                    output.append("\t" + remaining.getName() + " has " + remaining.getWhens().size() + " when criteria(s) left." + System.lineSeparator());

                    for(WhenData remainingWhen : remaining.getWhens()) {
                        output.append("\t\t" + remainingWhen.toString() + System.lineSeparator());
                    }
                }

                if (noMoreStatement)
                    continue;;

                output.append("- " + remaining.getName() + " has " + remaining.getRemainingStatements().size() + " statement(s) left:" + System.lineSeparator());

                //def requirements = tree.getRemainingRequireStatements(remaining)
                final List<PoolStateTree.RemainingRequire> requirements = tree.sortRemainingRequireStatementByWeight(remaining);

                if (!requirements.isEmpty()) {
                    output.append("\t" + requirements.size() + " requirement(s):" + System.lineSeparator());

                    for (PoolStateTree.RemainingRequire remainingRequire : requirements) {

                        if (remainingRequire.isWouldMatch())
                            output.append("\t\t[WOULD MATCH] " + remainingRequire.getStatement().toString() + System.lineSeparator());
                        else if (remainingRequire.isCouldMatch())
                            output.append("\t\t[COULD MATCH] " + remainingRequire.getStatement().toString() + System.lineSeparator());
                        else if (Boolean.TRUE.equals(remainingRequire.getStatement().getUnbloatable()))
                            output.append("\t\t[UNBLOATABLE] " + remainingRequire.getStatement().toString() + System.lineSeparator());
                        else
                            output.append("\t\t[NOT MATCHED] " + remainingRequire.getStatement().toString() + System.lineSeparator());
                    }

                }


                final List<PoolStateTree.RemainingBroadcast> broadcasts = tree.getRemainingBroadcastStatements(remaining);
                if (!broadcasts.isEmpty()) {
                    output.append("\t" + broadcasts.size() + " broadcast(s):" + System.lineSeparator());
                    for (PoolStateTree.RemainingBroadcast remainingBroadcast : broadcasts) {
                        output.append("\t\t[REQUIRED BY " + remainingBroadcast.getRequireBy() + "] " + remainingBroadcast.getStatement().toString() + System.lineSeparator());
                    }

                }

            }


            output.append(System.lineSeparator());
        }

        if (output.length() > 0) Logger.warn(output.toString());

        if (!poolErrors.isEmpty()) {
            Logger.info("Exception(s) caught: " + getErrors().size());

            for (PoolError ex : poolErrors) {
                Logger.error(ex.getInv().getName(), ex.getThrowable());
            }
        }

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

    public static class PoolError {
        private Inv inv;
        private Throwable throwable;

        public Inv getInv() {
            return inv;
        }

        public void setInv(Inv inv) {
            this.inv = inv;
        }

        public Throwable getThrowable() {
            return throwable;
        }

        public void setThrowable(Throwable throwable) {
            this.throwable = throwable;
        }
    }
}
