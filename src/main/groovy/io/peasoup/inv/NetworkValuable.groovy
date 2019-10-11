package io.peasoup.inv

class NetworkValuable {

    final static int FAILED = 0, // Negative match
                     UNBLOADTING = 1, // No match verified
                     SUCCESSFUL = 2 // Positive match

    final static Broadcast BROADCAST = new Broadcast()
    final static Require REQUIRE = new Require()

    Object id
    String name
    Manageable match
    boolean unbloatable

    Closure ready
    Closure resolved
    Closure unresolved

    Inv inv

    // When resolving requirement into a variable
    String into


    @Override
    String toString() {
        return "[$inv.name] => [${match ?: "UNSPECIFIED ACTION"}] [${name}] ${id}"
    }

    interface Manageable {
        int manage(NetworkValuablePool pool, NetworkValuable networkValuable)
    }

    static class Broadcast implements Manageable {

        int manage(NetworkValuablePool pool, NetworkValuable networkValuable) {

            /*
            if (pool.runningState == pool.UNBLOATING) {
                if (networkValuable.unready)
                    networkValuable.unready()

                Logger.warn networkValuable

                return NetworkValuable.UNBLOADTING
            }
            */

            def channel = pool.availableValuables[networkValuable.name]
            def staging = pool.stagingValuables[networkValuable.name]

            if (channel.containsKey(networkValuable.id) || staging.containsKey(networkValuable.id)) {
                Logger.warn "${networkValuable.id} already broadcasted. Skipped"

                return NetworkValuable.SUCCESSFUL
            }

            Logger.info networkValuable

            def response = "undefined"

            if (networkValuable.ready) {
                response = networkValuable.ready()
            }

            staging.put(networkValuable.id, [
                resolvedBy: networkValuable.inv.name,
                response: response
            ])

            return NetworkValuable.SUCCESSFUL
        }

        @Override
        String toString() {
            return "BROADCAST"
        }
    }

    static class Require implements Manageable {

        int manage(NetworkValuablePool pool, NetworkValuable networkValuable) {

            // Is it in cleaning state ?
            if (pool.runningState == pool.HALTED) {

                if (networkValuable.unresolved)
                    networkValuable.unresolved([
                            name: networkValuable.name,
                            id: networkValuable.id,
                            owner: networkValuable.inv.name
                    ])

                Logger.warn networkValuable

                return NetworkValuable.SUCCESSFUL
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

                    return NetworkValuable.UNBLOADTING
                }

                return NetworkValuable.FAILED
            }

            Logger.info networkValuable

            // Implement variable into NV inv (if defined)
            if (networkValuable.into)
                networkValuable.inv.delegate.metaClass.setProperty(networkValuable.into, broadcast.response)

            return NetworkValuable.SUCCESSFUL
        }

        @Override
        String toString() {
            return "REQUIRE"
        }
    }


}
