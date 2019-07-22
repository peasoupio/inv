package io.peasoup.inv

class NetworkValuable {

    final static Broadcast BROADCAST = new Broadcast()
    final static Require REQUIRE = new Require()

    Object id
    String name
    Manageable match

    Closure ready
    Closure unready
    Closure resolved
    Closure unresolved

    Inv inv

    // When resolving requirement into a variable
    String into

    @Override
    String toString() {
        return "[${match ?: "UNSPECIFIED ACTION"}] [${name}] ${id}"
    }

    interface Manageable {
        boolean manage(NetworkValuablePool pool, NetworkValuable networkValuable)
    }

    static class Broadcast implements Manageable {

        boolean manage(NetworkValuablePool pool, NetworkValuable networkValuable) {
            if (!pool.stillRunning) {
                if (networkValuable.unready)
                    networkValuable.unready()

                return false
            }

            def channel = pool.availableValuables[networkValuable.name]
            def staging = pool.stagingValuables[networkValuable.name]

            if (channel.containsKey(networkValuable.id) || staging.containsKey(networkValuable.id)) {
                Logger.warn "${networkValuable.id} already broadcasted. Skipped"

                return false
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

            return true
        }

        @Override
        String toString() {
            return "BROADCAST"
        }
    }

    static class Require implements Manageable {

        boolean manage(NetworkValuablePool pool, NetworkValuable networkValuable) {

            if (!pool.stillRunning) {
                if (networkValuable.unresolved)
                    networkValuable.unresolved()

                return false
            }

            def channel = pool.availableValuables[networkValuable.name]
            def broadcast = channel[networkValuable.id]

            if (!broadcast)
                return false

            Logger.info networkValuable

            // Implement variable into NV inv (if defined)
            if (networkValuable.into)
                networkValuable.inv.delegate.metaClass.setProperty(networkValuable.into, broadcast.response)

            return true
        }

        @Override
        String toString() {
            return "REQUIRE"
        }
    }


}
