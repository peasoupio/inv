package io.peasoup.inv.run;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class PoolReport {
    private final Queue<Inv> digested = new LinkedList<>();
    private final Queue<PoolException> exceptions = new LinkedList<>();
    private volatile boolean halted = false;

    public PoolReport() {

    }

    public PoolReport(List<Inv> digested, Queue<PoolException> exceptions, Boolean halted) {
        assert digested != null : "Digested collection is required. NOTE: can be empty";
        assert exceptions != null : "Exceptions collection is required. NOTE: can be empty";

        concat(digested, exceptions, halted);
    }

    public void eat(PoolReport other) {
        assert other != null : "Other (pool report) is required";

        concat(other.getDigested(), other.getExceptions(), other.getHalted());
    }

    public boolean isOk() {
        if (halted) return false;

        return exceptions.isEmpty();
    }

    public void reset() {
        digested.clear();
        exceptions.clear();
    }

    public void printPoolTrace(final NetworkValuablePool pool) {
        assert pool != null : "Pool is required";

        Logger.info("Completed INV(s): " + (pool.getTotalInvs().size() - pool.getRemainingInvs().size()));
        Logger.info("Incompleted INV(s): " + pool.getRemainingInvs().size());

        StringBuilder output = new StringBuilder();
        PoolStateTree tree = new PoolStateTree(pool);

        if (!pool.getRemainingInvs().isEmpty()) {

            output.append("Incompleted INV(s) details: " + System.lineSeparator());

            for (Inv remaining : tree.sortRemainingByRequireWeight()) {

                if (remaining.getRemainingStatements().isEmpty()) {
                    output.append("- " + remaining.getName() + " has no statement left. Look below for exception(s).");
                    continue;
                }


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
                        else if (remainingRequire.getStatement().getUnbloatable())
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

        if (!exceptions.isEmpty()) {
            Logger.info("Exception(s) caught: " + getExceptions().size());

            for (PoolException ex : exceptions) {
                Logger.error(ex.getInv().getName(), ex.getException());
            }
        }

    }

    private void concat(Collection<Inv> digested, Collection<PoolException> exceptions, boolean halted) {
        assert digested != null : "Digested collection is required. NOTE: can be empty";
        assert exceptions != null : "Exceptions collection is required. NOTE: can be empty";

        this.digested.addAll(digested);
        this.exceptions.addAll(exceptions);

        // Once halted, can't put it back on
        if (halted) this.halted = true;
    }

    public final Queue<Inv> getDigested() {
        return digested;
    }

    public final Queue<PoolException> getExceptions() {
        return exceptions;
    }

    public boolean getHalted() {
        return halted;
    }

    public boolean isHalted() {
        return halted;
    }

    public static class PoolException {
        private Inv inv;
        private Exception exception;

        public Inv getInv() {
            return inv;
        }

        public void setInv(Inv inv) {
            this.inv = inv;
        }

        public Exception getException() {
            return exception;
        }

        public void setException(Exception exception) {
            this.exception = exception;
        }
    }
}
