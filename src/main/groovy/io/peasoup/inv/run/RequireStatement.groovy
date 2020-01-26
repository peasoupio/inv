package io.peasoup.inv.run

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic

@CompileStatic
class RequireStatement implements Statement {

    final static Manageable REQUIRE = new Require()

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
    int state = NOT_PROCESSED

    Manageable getMatch() { REQUIRE }

    @Override
    String toString() {
        return "[$inv.name] => [REQUIRE] [${name}] ${id}"
    }

    static class Require implements Manageable<RequireStatement> {

        void manage(NetworkValuablePool pool, RequireStatement requireValuable) {

            // Reset to make sure NV is fine
            requireValuable.state = RequireStatement.NOT_PROCESSED

            // Do nothing if halting
            if (pool.runningState == pool.HALTING)
                return

            // Get broadcast
            Map<Object, BroadcastStatement.Response> channel = pool.availableStatements.get(requireValuable.name)
            BroadcastStatement.Response broadcast = channel.get(requireValuable.id)

            if (!broadcast) {

                // By default
                requireValuable.state = RequireStatement.FAILED

                boolean toUnbloat = false

                // Did it already unbloated
                if (pool.unbloatedStatements.get(requireValuable.name).contains(requireValuable.id)) {
                    toUnbloat = true
                }

                // Does this one unbloats
                if (pool.runningState == pool.UNBLOATING && requireValuable.unbloatable) {
                    toUnbloat = true

                    // Cache for later
                    pool.unbloatedStatements[requireValuable.name].add(requireValuable.id)
                }

                if (toUnbloat) {

                    requireValuable.state = RequireStatement.UNBLOADTING
                    Logger.info requireValuable

                    if (requireValuable.unresolved)
                        requireValuable.unresolved.call([
                                name: requireValuable.name,
                                id: requireValuable.id,
                                owner: requireValuable.inv.name
                        ])
                }

                return
            }

            requireValuable.state = RequireStatement.SUCCESSFUL

            Logger.info requireValuable

            // Implement variable into NV inv (if defined)
            if (requireValuable.into)
                setPropertyToDelegate(
                        requireValuable.inv.delegate,
                        requireValuable.into,
                        broadcast.asDelegate(requireValuable.inv, requireValuable.defaults))

            // Sends message to resolved (if defined)
            if (requireValuable.resolved) {
                requireValuable.resolved.delegate = broadcast.asDelegate(requireValuable.inv, requireValuable.defaults)
                requireValuable.resolved.call()
            }

            // Check if NV would have dumped something
            requireValuable.inv.dumpDelegate()
        }

        @CompileDynamic
        static def setPropertyToDelegate(Object delegate, String propertyName, Object value) {
            //noinspection GroovyAssignabilityCheck
            delegate.metaClass.setProperty(propertyName, value)
        }
    }
}
