package io.peasoup.inv

// TODO @CompileStatic is a bit more complicated here
class BroadcastValuable implements NetworkValuable {

    final static Manageable BROADCAST = new BroadcastValuable.Broadcast()

    Object id
    String name

    // Callback when ready
    Closure<Map> ready

    // When assigned to a Inv
    Inv inv

    // When processed
    int state = NOT_PROCESSED

    Manageable getMatch() { BROADCAST }

    @Override
    String toString() {
        return "[$inv.name] => [BROADCAST] [${name}] ${id}"
    }

    static class Broadcast implements NetworkValuable.Manageable<BroadcastValuable> {

        void manage(NetworkValuablePool pool, BroadcastValuable broadcastValuable) {

            // Reset to make sure NV is fine
            broadcastValuable.state = BroadcastValuable.NOT_PROCESSED

            def channel = pool.availableValuables[broadcastValuable.name]
            def staging = pool.stagingValuables[broadcastValuable.name]

            if (channel.containsKey(broadcastValuable.id) || staging.containsKey(broadcastValuable.id)) {
                Logger.warn "${broadcastValuable.id} already broadcasted. Skipped"

                broadcastValuable.state = BroadcastValuable.ALREADY_BROADCAST
                return
            }

            if (pool.runningState == pool.HALTING) {

                broadcastValuable.state = RequireValuable.HALTING

                Logger.warn broadcastValuable
                return
            }


            broadcastValuable.state = BroadcastValuable.SUCCESSFUL

            Logger.info broadcastValuable

            Map response = null
            Closure<Map> defaultClosure = null

            if (broadcastValuable.ready) {
                def rawReponnse = broadcastValuable.ready()

                if (rawReponnse instanceof Map) {
                    response = rawReponnse as Map

                    // Resolve default closure
                    if (response && response["\$"] instanceof Closure) {
                        defaultClosure = response["\$"] as Closure<Map>
                        response.remove("\$")
                    }
                }

            }

            // Staging response
            staging.put(broadcastValuable.id, new Response(
                resolvedBy: broadcastValuable.inv.name,
                response: response,
                defaultClosure: defaultClosure
            ))
        }
    }

    static class Response {

        String resolvedBy
        Map response
        Closure defaultClosure

        /**
         * Cast Response into a thread-safe delegate.
         * It also expands response closures at top level already bound to the inv's delegate (for inner broadcasts and requires)
         * @param caller the actual inv requiring this response
         * @return an expando object gathering all the delegate information
         */
        Expando asDelegate(Inv caller, boolean defaults) {
            assert caller

            def delegate = toExpando()

            // Defer properties into response
            delegate.metaClass.propertyMissing = { String propertyName ->

                if (!response)
                    return null

                def property = response[propertyName]

                if (!property)
                    return null

                return property
            }

            // Defer into response (if existing)
            delegate.metaClass.methodMissing = { String methodName, args ->

                if (!response)
                    return null

                def closure = response[methodName]

                if (!closure)
                    return null

                if (!(closure instanceof Closure))
                    return null

                def copy = closure
                        .dehydrate()
                        .rehydrate(
                                caller.delegate,
                                closure.owner,
                                closure.thisObject)
                copy.resolveStrategy = Closure.DELEGATE_FIRST
                //noinspection GroovyAssignabilityCheck
                return copy.call(*args)
            }

            // If response has a default closure, call it right now
            if (defaultClosure && defaults) {
                def copy = defaultClosure
                        .dehydrate()
                        .rehydrate(
                                caller.delegate,
                                defaultClosure.owner,
                                defaultClosure.thisObject)
                copy.resolveStrategy = Closure.DELEGATE_FIRST
                def defaultResponse = copy.call()

                if (defaultResponse) {
                    delegate.properties << defaultResponse
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
