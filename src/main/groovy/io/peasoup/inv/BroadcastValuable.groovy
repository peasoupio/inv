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
            Closure defaultClosure = null

            if (networkValuable.ready && pool.runningState != pool.HALTING) {
                response = networkValuable.ready()

                // Resolve default closure
                if (response instanceof Map && response["\$"] instanceof Closure)
                    defaultClosure = response["\$"]
                else if(response.respondsTo("\$")) {
                    defaultClosure = response.&$
                }

            }

            // Staging response
            staging.put(networkValuable.id, new Response(
                resolvedBy: networkValuable.inv.name,
                response: response,
                defaultClosure: defaultClosure
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
        Closure defaultClosure


        void callDefault(Inv caller) {
            assert caller

            // If response has a default closure, call it right now
            if (!defaultClosure) {
                return
            }

            def copy = defaultClosure
                    .dehydrate()
                    .rehydrate(
                            caller.delegate,
                            defaultClosure.owner,
                            defaultClosure.thisObject)
            copy.resolveStrategy = Closure.DELEGATE_FIRST
            copy.call()
        }

        /**
         * Cast Response into a thread-safe delegate.
         * It also expands response closures at top level already bound to the inv's delegate (for inner broadcasts and requires)
         * @param caller the actual inv requiring this response
         * @return an expando object gathering all the delegate information
         */
        Expando asDelegate(Inv caller) {
            assert caller

            def delegate = toExpando()

            // Defer properties into response
            delegate.metaClass.propertyMissing = { String propertyName ->
                def property = response[propertyName]

                if (property)
                    return property
            }

            // Defer into response (if existing)
            delegate.metaClass.methodMissing = { String methodName, args ->
                // Can't call default closure directly
                if (methodName == "\$")
                    return

                def method = response[methodName]

                if (method && method instanceof Closure) {
                    def copy = method
                            .dehydrate()
                            .rehydrate(
                                    caller.delegate,
                                    method.owner,
                                    method.thisObject)
                    copy.resolveStrategy = Closure.DELEGATE_FIRST
                    copy.call(args)
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
