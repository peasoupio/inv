package io.peasoup.inv.run

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic

@CompileStatic
class BroadcastStatement implements Statement {

    final static Manageable BROADCAST = new Broadcast()

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

    static class Broadcast implements Manageable<BroadcastStatement> {

        void manage(NetworkValuablePool pool, BroadcastStatement broadcastValuable) {

            // Reset to make sure NV is fine
            broadcastValuable.state = BroadcastStatement.NOT_PROCESSED

            // Do nothing if halting
            if (pool.runningState == pool.HALTING)
                return

            def channel = pool.availableStatements[broadcastValuable.name]
            def staging = pool.stagingStatements[broadcastValuable.name]

            if (channel.containsKey(broadcastValuable.id) || staging.containsKey(broadcastValuable.id)) {
                Logger.warn "${broadcastValuable.id} already broadcasted. Skipped"

                broadcastValuable.state = BroadcastStatement.ALREADY_BROADCAST
                return
            }

            broadcastValuable.state = BroadcastStatement.SUCCESSFUL

            Logger.info broadcastValuable

            Map response = null
            Closure<Map> defaultClosure = null

            if (broadcastValuable.ready) {
                Object rawReponnse = broadcastValuable.ready.call()

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
        @CompileDynamic
        Expando asDelegate(Inv caller, boolean defaults) {
            assert caller, 'Caller is required'

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
