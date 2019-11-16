package io.peasoup.inv

class RequireValuable implements NetworkValuable {

    final Manageable match = NetworkValuable.REQUIRE

    // Idenfitication
    Object id
    String name

    // State managmenet
    Boolean unbloatable = false
    Boolean defaults = true
    Inv inv
    String into // When resolving requirement into a variable

    // Events callback
    Closure resolved
    Closure unresolved

    // When processed
    int match_state = NOT_PROCESSED


    @Override
    String toString() {
        return "[$inv.name] => [REQUIRE] [${name}] ${id}"
    }

    static class Require implements NetworkValuable.Manageable<RequireValuable> {

        void manage(NetworkValuablePool pool, RequireValuable requireValuable) {

            // Reset to make sure NV is fine
            requireValuable.match_state = RequireValuable.NOT_PROCESSED

            // Is it in cleaning state ?
            if (pool.runningState == pool.HALTING) {

                if (requireValuable.unresolved)
                    requireValuable.unresolved([
                            name: requireValuable.name,
                            id: requireValuable.id,
                            owner: requireValuable.inv.name
                    ])

                Logger.warn requireValuable

                requireValuable.match_state = RequireValuable.SUCCESSFUL
                return
            }

            def channel = pool.availableValuables[requireValuable.name]
            def broadcast = channel[requireValuable.id]

            if (!broadcast) {

                // Is it bloating ?

                if (pool.runningState == pool.UNBLOATING &&
                    requireValuable.unbloatable) {

                    if (requireValuable.unresolved)
                        requireValuable.unresolved([
                                name: requireValuable.name,
                                id: requireValuable.id,
                                owner: requireValuable.inv.name
                        ])

                    requireValuable.match_state = RequireValuable.UNBLOADTING
                    return
                }

                requireValuable.match_state = RequireValuable.FAILED
                return
            }

            Logger.info requireValuable

            // Implement variable into NV inv (if defined)
            if (requireValuable.into)
                requireValuable.inv.delegate.metaClass.setProperty(
                        requireValuable.into,
                        broadcast.asDelegate(requireValuable.inv, requireValuable.defaults)
                )

            // Sends message to resolved (if defined)
            if (requireValuable.resolved) {
                requireValuable.resolved.delegate = broadcast.asDelegate(requireValuable.inv, requireValuable.defaults)
                requireValuable.resolved()
            }

            // Check if NV would have dumped something
            requireValuable.inv.dumpDelegate()

            requireValuable.match_state = RequireValuable.SUCCESSFUL
        }
    }


}
