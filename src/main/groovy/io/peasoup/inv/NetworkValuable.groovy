package main.groovy.io.peasoup.inv

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
    Object response


    @Override
    String toString() {
        return "[${match ?: "UNSPECIFIED ACTION"}] ${this.id}"
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

            Logger.info networkValuable

            if (networkValuable.ready) {
                networkValuable.response = networkValuable.ready()
            }


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

            if (!channel.containsKey(networkValuable.id))
                return false

            def broadcast = channel.containsKey(networkValuable.id)

            Logger.info networkValuable

            if (networkValuable.resolved)
                networkValuable.resolved(broadcast.response)

            return true
        }

        @Override
        String toString() {
            return "REQUIRE"
        }
    }


}
