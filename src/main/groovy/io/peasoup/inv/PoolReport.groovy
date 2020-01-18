package io.peasoup.inv

import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue

class PoolReport {
    final BlockingQueue<Inv> digested = new LinkedBlockingQueue<>()
    final BlockingQueue<PoolException> exceptions = new LinkedBlockingQueue<>()
    volatile boolean halted = false

    PoolReport() {

    }

    PoolReport(List<Inv> digested, BlockingQueue<PoolException> exceptions, Boolean halted) {
        concat(digested, exceptions, halted)
    }

    void eat(PoolReport other) {
        assert other

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
        assert pool

        Logger.info "Completed INV(s): ${pool.totalInvs.size() - pool.remainingInvs.size()}"
        Logger.info "Incompleted INV(s): ${pool.remainingInvs.size()}"

        StringBuilder output = new StringBuilder()



        if (!pool.remainingInvs.isEmpty()) {

            Map<String, Map<String, List<RequireStatement>>> allRemainingRequirements = [:]
            Map<String, Map<String, RequireStatement>> allRemainingBroadcasts = [:]

            for(Statement statement: pool.remainingInvs.collectMany { it.remainingStatements }) {
                if (statement.match == RequireStatement.REQUIRE) {

                    if (!allRemainingRequirements.containsKey(statement.name))
                        allRemainingRequirements[statement.name] = [:]

                    def statementsForNames = allRemainingRequirements[statement.name]

                    if (!statementsForNames.containsKey(statement.id))
                        statementsForNames[statement.id] = []

                    statementsForNames[statement.id] << statement
                }

                if (statement.match == BroadcastStatement.BROADCAST) {

                    if (!allRemainingBroadcasts.containsKey(statement.name))
                        allRemainingBroadcasts[statement.name] = [:]

                    allRemainingBroadcasts[statement.name][statement.id] = statement
                }
            }

            output.append "INV(s): ${System.lineSeparator()}"

            def invs = pool
                .remainingInvs
                .sort { a, b ->
                    Integer weightA = a.remainingStatements.findAll {it.match == RequireStatement.REQUIRE }.size()
                    Integer weightB = b.remainingStatements.findAll {it.match == RequireStatement.REQUIRE }.size()

                    return weightA - weightB
                }


            for (Inv remaining : invs) {
                output.append "- ${remaining.name} has ${remaining.remainingStatements.size()} statement(s) left:${System.lineSeparator()}"


                def requirements = remaining.remainingStatements
                        .findAll { it.match == RequireStatement.REQUIRE }
                        .collect { Statement statement ->
                            boolean wouldHaveMatched = pool.availableStatements[statement.name]?[statement.id] != null
                            boolean couldHaveMatched = allRemainingBroadcasts[statement.name]?[statement.id] != null

                            return [
                                statement: statement,
                                wouldMatch: wouldHaveMatched,
                                couldMatch: couldHaveMatched
                        ]}
                        /*
                        TODO Weight is mesured, but is "natural ordering" more important for troubleshooting ?
                        .sort { a, b ->
                            Integer weightA = 1
                            Integer weightB = 1

                            if (a.couldMatch)
                                weightA = 0
                            if (a.wouldMatch)
                                weightA = -1

                            if (b.couldMatch)
                                weightB = 0
                            if (b.wouldMatch)
                                weightB = -1

                            return weightA - weightB
                        }*/

                if (!requirements.isEmpty()) {
                    output.append "\t${requirements.size()} requirement(s):${System.lineSeparator()}"

                    for (Map value : requirements) {

                        if (value.wouldMatch)
                            output.append "\t\t[WOULD MATCH] ${value.statement.toString()}${System.lineSeparator()}"
                        else if (value.couldMatch)
                            output.append "\t\t[COULD MATCH] ${value.statement.toString()}${System.lineSeparator()}"
                        else if (value.statement.unbloatable)
                            output.append "\t\t[UNBLOATABLE] ${value.statement.toString()}${System.lineSeparator()}"
                        else
                            output.append "\t\t[NOT MATCHED] ${value.statement.toString()}${System.lineSeparator()}"
                    }
                }

                def broadcasts = remaining.remainingStatements.findAll { it.match == BroadcastStatement.BROADCAST }
                if (!broadcasts.isEmpty()) {
                    output.append "\t${broadcasts.size()} broadcast(s):${System.lineSeparator()}"
                    for (Statement statement : broadcasts) {
                        Integer requireBy = allRemainingRequirements[statement.name]?[statement.id]?.size() ?: 0

                        output.append "\t\t[REQUIRED BY ${requireBy}] ${statement.toString()}${System.lineSeparator()}"
                    }
                }
            }

            output.append System.lineSeparator()
        }

        if (output.size() > 0)
            Logger.warn output.toString()
    }

    private void concat(Collection<Inv> digested, Collection<PoolException> exceptions, boolean halted) {
        assert digested != null
        assert exceptions != null

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


