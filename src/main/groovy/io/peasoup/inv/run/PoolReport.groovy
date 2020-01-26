package io.peasoup.inv.run


import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue

class PoolReport {
    final BlockingQueue<Inv> digested = new LinkedBlockingQueue<>()
    final BlockingQueue<PoolException> exceptions = new LinkedBlockingQueue<>()
    volatile boolean halted = false

    PoolReport() {

    }

    PoolReport(List<Inv> digested, BlockingQueue<PoolException> exceptions, Boolean halted) {
        assert digested != null, 'Digested collection is required. NOTE: can be empty'
        assert exceptions != null, 'Exceptions collection is required. NOTE: can be empty'

        concat(digested, exceptions, halted)
    }

    void eat(PoolReport other) {
        assert other ,'Other (pool report) is required'

        concat(other.digested, other.exceptions, other.halted)
    }

    /*
        Determines if report should be considered successful or not
     */
    boolean isOk() {
        if (halted)
            return false

        if (!exceptions.isEmpty())
            return false

        return true
    }

    void reset() {
        digested.clear()
        exceptions.clear()
    }

    void printPoolTrace(NetworkValuablePool pool) {
        assert pool, 'Pool is required'

        Logger.info "Completed INV(s): ${pool.totalInvs.size() - pool.remainingInvs.size()}"
        Logger.info "Incompleted INV(s): ${pool.remainingInvs.size()}"

        StringBuilder output = new StringBuilder()
        PoolStateTree tree = new PoolStateTree(pool)

        if (!pool.remainingInvs.isEmpty()) {

            output.append "Incompleted INV(s) details: ${System.lineSeparator()}"

            for (Inv remaining : tree.sortRemainingByRequireWeight()) {

                if (remaining.remainingStatements.isEmpty()) {
                    output.append "- ${remaining.name} has no statement left. Look below for exception(s)."
                    continue
                }

                output.append "- ${remaining.name} has ${remaining.remainingStatements.size()} statement(s) left:${System.lineSeparator()}"

                def requirements = tree.getRemainingRequireStatements(remaining)

                if (!requirements.isEmpty()) {
                    output.append "\t${requirements.size()} requirement(s):${System.lineSeparator()}"

                    for (PoolStateTree.RemainingRequire remainingRequire : requirements) {

                        if (remainingRequire.wouldMatch)
                            output.append "\t\t[WOULD MATCH] ${remainingRequire.statement.toString()}${System.lineSeparator()}"
                        else if (remainingRequire.couldMatch)
                            output.append "\t\t[COULD MATCH] ${remainingRequire.statement.toString()}${System.lineSeparator()}"
                        else if (remainingRequire.statement.unbloatable)
                            output.append "\t\t[UNBLOATABLE] ${remainingRequire.statement.toString()}${System.lineSeparator()}"
                        else
                            output.append "\t\t[NOT MATCHED] ${remainingRequire.statement.toString()}${System.lineSeparator()}"
                    }
                }

                def broadcasts = tree.getRemainingBroadcastStatements(remaining)
                if (!broadcasts.isEmpty()) {
                    output.append "\t${broadcasts.size()} broadcast(s):${System.lineSeparator()}"
                    for (PoolStateTree.RemainingBroadcast remainingBroadcast : broadcasts) {
                        output.append "\t\t[REQUIRED BY ${remainingBroadcast.requireBy}] ${remainingBroadcast.statement.toString()}${System.lineSeparator()}"
                    }
                }
            }

            output.append System.lineSeparator()
        }

        if (output.size() > 0)
            Logger.warn output.toString()

        if (!exceptions.isEmpty()) {
            Logger.info "Exception(s) caught: ${exceptions.size()}"
            exceptions.each {
                Logger.error it.inv.name, it.exception
            }
        }
    }

    private void concat(Collection<Inv> digested, Collection<PoolException> exceptions, boolean halted) {
        assert digested != null, 'Digested collection is required. NOTE: can be empty'
        assert exceptions != null, 'Exceptions collection is required. NOTE: can be empty'

        this.digested.addAll(digested)
        this.exceptions.addAll(exceptions)

        // Once halted, can't put it back on
        if (halted)
            this.halted = true
    }

    static class PoolException {
        Inv inv
        Exception exception
    }
}


