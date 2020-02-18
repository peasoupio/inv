package io.peasoup.inv.run;

import java.util.*;
import java.util.stream.Collectors;

public class PoolStateTree {
    public PoolStateTree(NetworkValuablePool pool) {
        assert pool != null: "Pool is required";
        assert !pool.getIsDigesting() : "It is unsafe to get a pool state during a pool digestion cycle";

        this.pool = pool;

        generate();
    }

    public List<Inv> sortRemainingByRequireWeight() {
        List<Inv> sortedInvs =  pool
                .getRemainingInvs()
                .stream()
                .collect(Collectors.toList());

        sortedInvs.sort((Inv a, Inv b) -> {
            Integer weightA = (int) a.getRemainingStatements()
                    .stream()
                    .filter(statement -> statement.getMatch() == RequireStatement.REQUIRE)
                    .count();

            Integer weightB = (int) b.getRemainingStatements()
                    .stream()
                    .filter(statement -> statement.getMatch() == RequireStatement.REQUIRE)
                    .count();

            return weightA - weightB;
        });

        return sortedInvs;
    }

    public List<RemainingBroadcast> getRemainingBroadcastStatements(Inv inv) {
        assert inv != null : "Inv is required";

        return inv.getRemainingStatements()
                .stream()
                .filter(statement -> statement.getMatch() == BroadcastStatement.BROADCAST)
                .map(statement -> new RemainingBroadcast(
                        (BroadcastStatement) statement,
                        getRequiredByCount((BroadcastStatement)statement)
                ))
                .collect(Collectors.toList());
    }

    public List<RemainingRequire> getRemainingRequireStatements(Inv inv) {
        assert inv != null : "Inv is required";

        return inv.getRemainingStatements()
                .stream()
                .filter(statement -> statement.getMatch() == RequireStatement.REQUIRE )
                .map(statement -> {
                    boolean wouldHaveMatched = pool.getAvailableStatements().get(statement.getName()).get(statement.getId()) != null;
                    boolean couldHaveMatched = couldHaveMatched((RequireStatement)statement);

                    return new RemainingRequire(
                            (RequireStatement)statement,
                            couldHaveMatched,
                            wouldHaveMatched
                    );
                })
                .collect(Collectors.toList());
    }

    public List<RemainingRequire> sortRemainingRequireStatementByWeight(Inv inv) {
        assert inv != null : "Inv is required";

        List<RemainingRequire> sortedRequireStatements = getRemainingRequireStatements(inv);

        sortedRequireStatements.sort ( (RemainingRequire a, RemainingRequire b) -> {
            Integer weightA = -1;
            Integer weightB = -1;

            if (a.isCouldMatch())
                weightA = 1;
            if (a.isWouldMatch())
                weightA = 0;

            if (b.isCouldMatch())
                weightB = 1;
            if (b.isWouldMatch())
                weightB = 0;

            return weightA - weightB;
        });

        return sortedRequireStatements;
    }

    private void generate() {

        pool.getRemainingInvs()
                .stream()
                .flatMap(inv -> inv.getRemainingStatements().stream())
                .collect(Collectors.toList())
                .forEach(statement -> {
                    // Get remaining requirement statement(s)
                    if (statement.getMatch().equals(RequireStatement.REQUIRE)) {
                        RequireStatement requireStatement = (RequireStatement) statement;

                        if (!remainingRequirements.containsKey(statement.getName()))
                            remainingRequirements.put(statement.getName(), new HashMap<Object, List<RequireStatement>>() {
                            });

                        Map<Object, List<RequireStatement>> statementsForNames = remainingRequirements.get(statement.getName());

                        if (!statementsForNames.containsKey(statement.getId()))
                            statementsForNames.put(statement.getId(), new ArrayList());

                        statementsForNames.get(statement.getId()).add(requireStatement);
                    }


                    // Get remaining broadcast statement(s)
                    if (statement.getMatch().equals(BroadcastStatement.BROADCAST)) {
                        BroadcastStatement broadcastStatement = (BroadcastStatement)statement;

                        if (!remainingBroadcasts.containsKey(statement.getName()))
                            remainingBroadcasts.put(statement.getName(), new HashMap<Object, BroadcastStatement>() {
                            });

                        remainingBroadcasts.get(statement.getName()).put(statement.getId(), broadcastStatement);
                    }
                });
    }

    public final Map<String, Map<Object, List<RequireStatement>>> getRemainingRequirements() {
        return remainingRequirements;
    }

    public final Map<String, Map<Object, BroadcastStatement>> getRemainingBroadcasts() {
        return remainingBroadcasts;
    }

    private int getRequiredByCount(BroadcastStatement statement) {
        if (getRemainingRequirements().containsKey(statement.getName()))
            return 0;

        Map<Object, List<RequireStatement>> ids = getRemainingRequirements().get(statement.getName());

        if (!ids.containsKey(statement.getId()))
            return 0;

        return ids.get(statement.getId()).size();
    }

    private boolean couldHaveMatched(RequireStatement statement) {
        if (!getRemainingBroadcasts().containsKey(statement.getName()))
            return false;

        return getRemainingBroadcasts().get(statement.getName()).containsKey(statement.getId());
    }

    private final NetworkValuablePool pool;
    private final Map<String, Map<Object, List<RequireStatement>>> remainingRequirements = new LinkedHashMap<String, Map<Object, List<RequireStatement>>>();
    private final Map<String, Map<Object, BroadcastStatement>> remainingBroadcasts = new LinkedHashMap<String, Map<Object, BroadcastStatement>>();

    public static class RemainingRequire {

        private RequireStatement statement;
        private boolean couldMatch;
        private boolean wouldMatch;

        public RemainingRequire(RequireStatement statement, boolean couldMatch, boolean wouldMatch) {
            this.statement = statement;
            this.couldMatch = couldMatch;
            this.wouldMatch = wouldMatch;
        }

        public RequireStatement getStatement() {
            return statement;
        }

        public boolean isCouldMatch() {
            return couldMatch;
        }

        public boolean isWouldMatch() {
            return wouldMatch;
        }
    }

    public static class RemainingBroadcast {

        private BroadcastStatement statement;
        private Integer requireBy = 0;

        RemainingBroadcast(BroadcastStatement statement, Integer requireBy) {
            this.statement = statement;
            this.requireBy = requireBy;
        }

        public BroadcastStatement getStatement() {
            return statement;
        }

        public Integer getRequireBy() {
            return requireBy;
        }
    }
}
