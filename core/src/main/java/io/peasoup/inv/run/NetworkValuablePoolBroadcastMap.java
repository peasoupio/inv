package io.peasoup.inv.run;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gather an index (map) of available or staged broadcast
 */
@RequiredArgsConstructor
public class NetworkValuablePoolBroadcastMap {

    @Getter
    private final Map<StatementIdentifier, BroadcastResponse> staticIdStatements = new ConcurrentHashMap<>(24, 0.9f, 1);

    @Getter
    private final Map<String, BroadcastResponse> dynamicIdStatements = new ConcurrentHashMap<>(24, 0.9f, 1);

    private final NetworkValuablePool pool;

    /**
     * Adds a broadcast response according to a broadcast statement.
     * If an existing ID is found, an exception is raised.
     *
     * @param statementIdentifier The broadcast statement
     * @param response The broadcast response.
     *
     * @return True if added, otherwise false.
     */
    public boolean add(StatementIdentifier statementIdentifier, BroadcastResponse response) {
        if (statementIdentifier == null)
            throw new IllegalArgumentException("statementIdentifier");

        // Do not attempt to apply resolved ID is pool is not ingesting
        if (pool.isIngesting())
            statementIdentifier.applyResolvedID();

        // Add the actual response to the statements
        synchronized (this) {
            // Act as a "double-lock checking" mechanism since we expect "alreadyBroadcast" being called before "add".
            if (exists(statementIdentifier))
                return false;

            // If it a broadcast statement
            if (statementIdentifier instanceof BroadcastStatement) {
                BroadcastStatement broadcastStatement = (BroadcastStatement) statementIdentifier;

                // Check if it belongs to the dynamicIdStatements
                if (broadcastStatement.isDynamic() && statementIdentifier.getId() == StatementDescriptor.DEFAULT_ID) {
                    dynamicIdStatements.put(statementIdentifier.getName(), response);
                // Or the default staticId statements
                } else {
                    staticIdStatements.put(statementIdentifier, response);
                }
            // Otherwise, use the default staticId statements
            } else {
                staticIdStatements.put(statementIdentifier, response);
            }

            return true;
        }
    }

    /**
     * Add all the statements from another NetworkValuablePoolBroadcastMap into this one
     * @param otherMap The other NetworkValuablePoolBroadcastMap
     */
    public synchronized void addAll(NetworkValuablePoolBroadcastMap otherMap) {
        if (otherMap == null)
            throw new IllegalArgumentException("otherMap");

        synchronized (this) {
            NetworkValuablePoolBroadcastMap extract = otherMap.extract();
            this.staticIdStatements.putAll(extract.staticIdStatements);
            this.dynamicIdStatements.putAll(extract.dynamicIdStatements);
        }
    }

    /**
     * Gets a broadcast response for a mathcing statement identifier.
     *
     * @param statementIdentifier The statement identifier
     * @return If no match is found, returns null.
     *         If a matching ID is found, return the specific response
     *         If ID is null and a matching dynamic response is found, return the dynamic response
     */
    public BroadcastResponse get(StatementIdentifier statementIdentifier) {
        if (statementIdentifier == null)
            throw new IllegalArgumentException("statementIdentifier");

        // Do not attempt to apply resolved ID is pool is not ingesting
        if (pool.isIngesting())
            statementIdentifier.applyResolvedID();

        synchronized (this) {

            // If a matching ID is found, returns the associated response
            if (staticIdStatements.containsKey(statementIdentifier))
                return staticIdStatements.get(statementIdentifier);
            // Otherwise, attempt to get a dynamic ID response
            else {
                BroadcastResponse response = dynamicIdStatements.get(statementIdentifier.getName());

                // If a response is found, make it static
                if (response != null)
                    staticIdStatements.put(statementIdentifier, response);

                return response;
            }
        }
    }

    /**
     * Checks if a statement is already added
     * @param statement The broadcast statement
     * @return True if already added, otherwise false
     */
    public boolean exists(StatementIdentifier statement) {
        if (statement == null)
            throw new IllegalArgumentException("statement");

        return staticIdStatements.containsKey(statement) ||
                dynamicIdStatements.containsKey(statement.getName());
    }

    /**
     * Gets the size of this map
     * @return The sum of the size of static and dynamic ids.
     */
    public int size() {
        return staticIdStatements.size() + dynamicIdStatements.size();
    }

    /**
     * Extract an exchange instance with its values.
     * Upon completion, the original (this) instance values are cleaned.
     * @return The extracted NetworkValuablePoolBroadcastMap
     */
    private NetworkValuablePoolBroadcastMap extract() {
        synchronized (this) {
            NetworkValuablePoolBroadcastMap extracted = new NetworkValuablePoolBroadcastMap(this.pool);
            extracted.staticIdStatements.putAll(this.staticIdStatements);
            extracted.dynamicIdStatements.putAll(this.dynamicIdStatements);
            this.staticIdStatements.clear();
            this.dynamicIdStatements.clear();

            return extracted;
        }
    }
}
