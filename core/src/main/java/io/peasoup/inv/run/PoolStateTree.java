package io.peasoup.inv.run;

import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("EqualsBetweenInconvertibleTypes")
public class PoolStateTree {
    public PoolStateTree(NetworkValuablePool pool) {
        if (pool == null) {
            throw new IllegalArgumentException("Pool is required");
        }
        if (pool.getIsDigesting()) {
            throw new IllegalArgumentException("It is unsafe to get a pool state during a pool digestion cycle");
        }

        this.pool = pool;

        generate();
    }

    public List<Inv> sortRemainingByRequireWeight() {
        List<Inv> sortedInvs = new ArrayList<>(pool.getRemainingInvs());

        sortedInvs.sort((Inv a, Inv b) -> {
            Integer weightA = (int) a.getRemainingStatements()
                    .stream()
                    .filter(statement -> RequireStatement.REQUIRE.equals(statement.getMatch()))
                    .count();

            Integer weightB = (int) b.getRemainingStatements()
                    .stream()
                    .filter(statement -> RequireStatement.REQUIRE.equals(statement.getMatch()))
                    .count();

            return weightA - weightB;
        });

        return sortedInvs;
    }

    public List<RemainingBroadcast> getRemainingBroadcastStatements(Inv inv) {
        if (inv == null) {
            throw new IllegalArgumentException("Inv is required");
        }

        return inv.getRemainingStatements()
                .stream()
                .filter(statement -> BroadcastStatement.BROADCAST.equals(statement.getMatch()))
                .map(statement -> new RemainingBroadcast(
                        (BroadcastStatement) statement,
                        getRequiredByCount((BroadcastStatement) statement)
                ))
                .collect(Collectors.toList());
    }


    public List<RemainingRequire> getRemainingRequireStatements(Inv inv) {
        if (inv == null) {
            throw new IllegalArgumentException("Inv is required");
        }

        return inv.getRemainingStatements()
                .stream()
                .filter(statement -> RequireStatement.REQUIRE.equals(statement.getMatch()) )
                .map(statement -> {
                    boolean wouldHaveMatched = pool.getAvailableStatements().get(statement.getName()).get(statement.getId()) != null;
                    boolean couldHaveMatched = couldHaveMatched((RequireStatement) statement);

                    return new RemainingRequire(
                            (RequireStatement) statement,
                            couldHaveMatched,
                            wouldHaveMatched
                    );
                })
                .collect(Collectors.toList());
    }

    public List<RemainingRequire> sortRemainingRequireStatementByWeight(Inv inv) {
        if (inv == null) {
            throw new IllegalArgumentException("Inv is required");
        }

        List<RemainingRequire> sortedRequireStatements = getRemainingRequireStatements(inv);

        sortedRequireStatements.sort((RemainingRequire a, RemainingRequire b) -> {
            int weightA = -1;
            int weightB = -1;

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
                            remainingRequirements.put(statement.getName(), new HashMap<>());

                        Map<Object, List<RequireStatement>> statementsForNames = remainingRequirements.get(statement.getName());

                        if (!statementsForNames.containsKey(statement.getId()))
                            statementsForNames.put(statement.getId(), new ArrayList<>());

                        statementsForNames.get(statement.getId()).add(requireStatement);
                    }


                    // Get remaining broadcast statement(s)
                    if (statement.getMatch().equals(BroadcastStatement.BROADCAST)) {
                        BroadcastStatement broadcastStatement = (BroadcastStatement) statement;

                        if (!remainingBroadcasts.containsKey(statement.getName()))
                            remainingBroadcasts.put(statement.getName(), new HashMap<>());

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
        if (!getRemainingRequirements().containsKey(statement.getName()))
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
    private final Map<String, Map<Object, List<RequireStatement>>> remainingRequirements = new LinkedHashMap<>();
    private final Map<String, Map<Object, BroadcastStatement>> remainingBroadcasts = new LinkedHashMap<>();

    public static class RemainingRequire {

        private final RequireStatement statement;
        private final boolean couldMatch;
        private final boolean wouldMatch;

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

        private final BroadcastStatement statement;
        private final Integer requireBy;

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
