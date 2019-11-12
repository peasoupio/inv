package io.peasoup.inv

class RequireValuable implements NetworkValuable {

    final Manageable match = NetworkValuable.REQUIRE

    // Idenfitication
    Object id
    String name

    // State managmenet
    boolean unbloatable
    boolean defaults = true
    Inv inv
    String into // When resolving requirement into a variable

    // Events callback
    Closure ready
    Closure resolved
    Closure unresolved

    // When processed
    int match_state = NOT_PROCESSED


    @Override
    String toString() {
        return "[$inv.name] => [REQUIRE] [${name}] ${id}"
    }

    static class Require implements NetworkValuable.Manageable<RequireValuable> {

        void manage(NetworkValuablePool pool, RequireValuable networkValuable) {

            // Reset to make sure NV is fine
            networkValuable.match_state = RequireValuable.NOT_PROCESSED

            // Is it in cleaning state ?
            if (pool.runningState == pool.HALTING) {

                if (networkValuable.unresolved)
                    networkValuable.unresolved([
                            name: networkValuable.name,
                            id: networkValuable.id,
                            owner: networkValuable.inv.name
                    ])

                Logger.warn networkValuable

                networkValuable.match_state = RequireValuable.SUCCESSFUL
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

                    networkValuable.match_state = RequireValuable.UNBLOADTING
                    return
                }

                networkValuable.match_state = RequireValuable.FAILED
                return
            }

            Logger.info networkValuable

            // Implement variable into NV inv (if defined)
            if (networkValuable.into)
                networkValuable.inv.delegate.metaClass.setProperty(
                        networkValuable.into,
                        broadcast.asDelegate(networkValuable.inv, networkValuable.defaults)
                )

            networkValuable.match_state = RequireValuable.SUCCESSFUL
        }

        @Override
        String toString() {
            return "REQUIRE"
        }
    }


}
