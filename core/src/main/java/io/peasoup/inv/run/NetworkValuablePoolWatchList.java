package io.peasoup.inv.run;

import lombok.Getter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class NetworkValuablePoolWatchList {

    @Getter
    private final Map<String, Map<Object, Set<Inv>>> watchlist = new ConcurrentHashMap<>(24, 0.9f, 1);

    /**
     * Add an INV to the watchlist
     * @param inv The INV
     */
    public synchronized void addWatcher(Inv inv) {
        if (inv == null)
            throw new IllegalArgumentException("inv");

        // Add remaining broadcast statements to the watch list
        for (Statement remainingStatement : inv.getRemainingStatements()) {

            // Proceed only Require statements
            if (!RequireStatement.REQUIRE.equals(remainingStatement.getMatch()))
                continue;

            // Get watchlist for statement name
            String watchlistKey = remainingStatement.getName();
            if (!watchlist.containsKey(watchlistKey))
                watchlist.put(watchlistKey, new ConcurrentHashMap<>());
            Map<Object, Set<Inv>> watchlistForName = watchlist.get(watchlistKey);

            // Get watchlist for statement id
            Object watchlistForNameKey = remainingStatement.getId();
            if (!watchlistForName.containsKey(watchlistForNameKey))
                watchlistForName.put(watchlistForNameKey, ConcurrentHashMap.newKeySet());

            // Add inv to watchlist
            watchlistForName.get(watchlistForNameKey).add(inv);
        }
    }

    synchronized void unwatch(Statement statement) {

        if (statement == null)
            throw new IllegalArgumentException("statement");

        if (!watchlist.containsKey(statement.getName()))
            return;

        Map<Object, Set<Inv>> idsToWatchers = watchlist.get(statement.getName());
        if (!idsToWatchers.containsKey(statement.getId()))
            return;

        Inv watcher = statement.getInv();
        Set<Inv> watchers =  idsToWatchers.get(statement.getId());
        watchers.remove(watcher);

        if (watchers.isEmpty())
            idsToWatchers.remove(statement.getId());

        if (idsToWatchers.isEmpty())
            watchlist.remove(statement.getName());
    }

    /**
     * Get the watchers for a specific statement.
     * @param statement The statment
     * @return Set of watching INVs
     */
    Set<Inv> getWatchers(Statement statement) {
        if (statement == null)
            throw new IllegalArgumentException("statement");

        if (!watchlist.containsKey(statement.getName()))
            return Collections.emptySet();

        Map<Object, Set<Inv>> idsToWatchers = watchlist.get(statement.getName());
        if (!idsToWatchers.containsKey(statement.getId()))
            return Collections.emptySet();

        return idsToWatchers.get(statement.getId());
    }


}
