package io.peasoup.inv.run

import groovy.transform.CompileStatic

@CompileStatic
class PoolStateTree {

    final private NetworkValuablePool pool

    final Map<String, Map<Object, List<RequireStatement>>> remainingRequirements = [:]

    final Map<String, Map<Object, BroadcastStatement>> remainingBroadcasts = [:]

    PoolStateTree(NetworkValuablePool pool) {
        assert pool, 'Pool is required'
        assert !pool.isDigesting, "It is unsafe to get a pool state during a pool digestion cycle"

        this.pool = pool

        generate()
    }

    List<Inv> sortRemainingByRequireWeight() {
        pool
            .remainingInvs
            .sort { a, b ->
                Integer weightA = a.remainingStatements.findAll {it.match == RequireStatement.REQUIRE }.size()
                Integer weightB = b.remainingStatements.findAll {it.match == RequireStatement.REQUIRE }.size()

                return weightA - weightB
            }
    }

    List<RemainingBroadcast> getRemainingBroadcastStatements(Inv inv) {
        assert inv, 'Inv is required'

        return inv.remainingStatements
                .findAll { it.match == BroadcastStatement.BROADCAST }
                .collect { Statement statement ->

                    return new RemainingBroadcast(
                            statement: statement as BroadcastStatement,
                            requireBy: remainingRequirements[statement.name]?[statement.id]?.size() ?: 0
                    )}
    }

    List<RemainingRequire> getRemainingRequireStatements(Inv inv) {
        assert inv, 'Inv is required'

        return inv.remainingStatements
            .findAll { it.match == RequireStatement.REQUIRE }
            .collect { Statement statement ->
                boolean wouldHaveMatched = pool.availableStatements[statement.name]?[statement.id] != null
                boolean couldHaveMatched = remainingBroadcasts[statement.name]?[statement.id] != null

                return new RemainingRequire(
                        statement: statement as RequireStatement,
                        wouldMatch: wouldHaveMatched,
                        couldMatch: couldHaveMatched
                )}
    }

    List<RemainingRequire> sortRemainingRequireStatementByWeight(Inv inv) {
        assert inv, 'Inv is required'

        return getRemainingRequireStatements(inv)
                .sort { a, b ->
                    Integer weightA = -1
                    Integer weightB = -1

                    if (a.couldMatch)
                        weightA = 1
                    if (a.wouldMatch)
                        weightA = 0

                    if (b.couldMatch)
                        weightB = 1
                    if (b.wouldMatch)
                        weightB = 0

                    return weightA - weightB
                }

    }

    private void generate() {

        for(Statement statement: (pool.remainingInvs.collectMany { it.remainingStatements } as List<Statement>)) {

            // Get remaining requirement statement(s)
            if (statement.match == RequireStatement.REQUIRE) {
                def requireStatement = statement as RequireStatement

                if (!remainingRequirements.containsKey(statement.name))
                    remainingRequirements[statement.name] = [:] as Map<Object, List<RequireStatement>>

                def statementsForNames = remainingRequirements[statement.name]

                if (!statementsForNames.containsKey(statement.id))
                    statementsForNames[statement.id] = []

                statementsForNames[statement.id].add(requireStatement)
            }

            // Get remaining broadcast statement(s)
            if (statement.match == BroadcastStatement.BROADCAST) {
                def broadcastStatement = statement as BroadcastStatement

                if (!remainingBroadcasts.containsKey(statement.name))
                    remainingBroadcasts[statement.name] = [:] as Map<Object, BroadcastStatement>

                remainingBroadcasts[statement.name][statement.id] = broadcastStatement
            }
        }
    }

    static class RemainingRequire {
        RequireStatement statement

        boolean couldMatch
        boolean wouldMatch
    }

    static class RemainingBroadcast {
        BroadcastStatement statement

        Integer requireBy = 0
    }
}
