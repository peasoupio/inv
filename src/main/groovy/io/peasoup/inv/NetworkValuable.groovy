package io.peasoup.inv

class NetworkValuable {

    final static int NOT_PROCESSED = -1, // Not processed yet
                     FAILED = 0, // Negative match
                     UNBLOADTING = 1, // No match verified
                     SUCCESSFUL = 2, // Positive match
                     ALREADY_BROADCAST = 3 // Positive match

    final static Broadcast BROADCAST = new Broadcast()
    final static Require REQUIRE = new Require()

    Object id
    String name
    Manageable match
    boolean unbloatable

    Closure ready
    Closure resolved
    Closure unresolved

    // When assigned to a Inv
    Inv inv
    // When resolving requirement into a variable
    String into
    // When processed
    int match_state = NOT_PROCESSED

    @Override
    String toString() {
        return "[$inv.name] => [${match ?: "UNSPECIFIED ACTION"}] [${name}] ${id}"
    }

    interface Manageable {
        void manage(NetworkValuablePool pool, NetworkValuable networkValuable)
    }

    static class Broadcast implements Manageable {

        void manage(NetworkValuablePool pool, NetworkValuable networkValuable) {

            // Reset to make sure NV is fine
            networkValuable.match_state = NetworkValuable.NOT_PROCESSED

            def channel = pool.availableValuables[networkValuable.name]
            def staging = pool.stagingValuables[networkValuable.name]

            if (channel.containsKey(networkValuable.id) || staging.containsKey(networkValuable.id)) {
                Logger.warn "${networkValuable.id} already broadcasted. Skipped"

                networkValuable.match_state = NetworkValuable.ALREADY_BROADCAST
                return
            }

            Logger.info networkValuable

            def response = "undefined"

            if (networkValuable.ready && pool.runningState != pool.HALTING) {
                response = networkValuable.ready()
            }

            staging.put(networkValuable.id, [
                resolvedBy: networkValuable.inv.name,
                response: response
            ])

            networkValuable.match_state = NetworkValuable.SUCCESSFUL
        }

        @Override
        String toString() {
            return "BROADCAST"
        }
    }

    static class Require implements Manageable {

        void manage(NetworkValuablePool pool, NetworkValuable networkValuable) {

            // Reset to make sure NV is fine
            networkValuable.match_state = NetworkValuable.NOT_PROCESSED

            // Is it in cleaning state ?
            if (pool.runningState == pool.HALTING) {

                if (networkValuable.unresolved)
                    networkValuable.unresolved([
                            name: networkValuable.name,
                            id: networkValuable.id,
                            owner: networkValuable.inv.name
                    ])

                Logger.warn networkValuable

                networkValuable.match_state = NetworkValuable.SUCCESSFUL
                return
            }

            def channel = pool.availableValuables[networkValuable.name]
            def broadcast = channel[networkValuable.id]

            if (!broadcast) {

                // Is it bloating ?

                if (pool.runningState == pool.UNBLOATING &&
                    networkValuable.unbloatable) {

                    if (networkValuable.unresolved)
                        networkValuable.unresolved([
                                name: networkValuable.name,
                                id: networkValuable.id,
                                owner: networkValuable.inv.name
                        ])

                    networkValuable.match_state = NetworkValuable.UNBLOADTING
                    return
                }

                networkValuable.match_state = NetworkValuable.FAILED
                return
            }

            Logger.info networkValuable

            // Implement variable into NV inv (if defined)
            if (networkValuable.into)
                networkValuable.inv.delegate.metaClass.setProperty(networkValuable.into, broadcast.response)

            networkValuable.match_state = NetworkValuable.SUCCESSFUL
        }

        @Override
        String toString() {
            return "REQUIRE"
        }
    }


}
