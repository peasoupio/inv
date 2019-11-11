package io.peasoup.inv

class BroadcastValuable implements NetworkValuable {

    final Manageable match = NetworkValuable.BROADCAST

    Object id
    String name



    // Callback when ready
    Closure ready

    // When assigned to a Inv
    Inv inv

    // When processed
    int match_state = NOT_PROCESSED

    @Override
    String toString() {
        return "[$inv.name] => [BROADCAST] [${name}] ${id}"
    }

    static class Broadcast implements NetworkValuable.Manageable {

        void manage(NetworkValuablePool pool, NetworkValuable networkValuable) {

            // Reset to make sure NV is fine
            networkValuable.match_state = BroadcastValuable.NOT_PROCESSED

            def channel = pool.availableValuables[networkValuable.name]
            def staging = pool.stagingValuables[networkValuable.name]

            if (channel.containsKey(networkValuable.id) || staging.containsKey(networkValuable.id)) {
                Logger.warn "${networkValuable.id} already broadcasted. Skipped"

                networkValuable.match_state = BroadcastValuable.ALREADY_BROADCAST
                return
            }

            Logger.info networkValuable

            Object response = "undefined"

            if (networkValuable.ready && pool.runningState != pool.HALTING) {
                response = networkValuable.ready()
            }

            staging.put(networkValuable.id, new Response(
                resolvedBy: networkValuable.inv.name,
                response: response
            ))

            networkValuable.match_state = BroadcastValuable.SUCCESSFUL
        }

        @Override
        String toString() {
            return "BROADCAST"
        }

    }

    static class Response {
        String resolvedBy
        Object response



        private Expando asDelegate(Inv caller) {
            def delegate = toExpando()

            delegate.metaClass.propertyMissing = { String propertyName ->
                def property = response[propertyName]

                if (property)
                    return property
            }

            delegate.metaClass.methodMissing = { String methodName, args ->
                def method = response[methodName]

                if (method && method instanceof Closure) {
                    method
                            .dehydrate()
                            .rehydrate(
                                    caller.delegate,
                                    method.owner,
                                    method.thisObject)
                            .call(args)
                }
            }

            return delegate
        }

        private Expando toExpando() {
            return new Expando(
                    response: this.response,
                    resolvedBy: this.resolvedBy
            )
        }
    }

}
