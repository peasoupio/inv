package io.peasoup.inv.randomizer

import io.peasoup.inv.run.InvExecutor
import io.peasoup.inv.run.InvHandler
import org.apache.commons.lang.RandomStringUtils
import org.junit.Before
import org.junit.Test

class RandomizeBatch1 {

    InvExecutor executor
    InvHandler invHandler

    @Before
    void setup() {
        executor = new InvExecutor()
        invHandler = new InvHandler(executor)
    }

    @Test
    void randomize_batch_1() {

        // main parameters
        def totalInv = 10000
        def maxRequire = 12
        // state vars
        Map<String, InvBootstrap> invs = [:] as Map<String, InvBootstrap>

        // Generate invs
        1.upto(totalInv, {
            def invName = RandomStringUtils.random(12, true, true)

            // Don't care about duplicates
            if (invName in invs)
                return

            invs[invName] = new InvBootstrap(name: invName)
        })

        // Randomize requirements and broadcasts
        def remainingsInvs = invs.values().collect()
        def totalInvs = invs.values().collect()

        def index = 0

        while(!remainingsInvs.isEmpty()) {

            def invBoostrap = remainingsInvs.pop()

            // create inv right now
            this.invHandler.call {
                name invBoostrap.name

                if (!invBoostrap.done) {
                    // Get number of requirements
                    def numRequire = Math.abs(new Random().nextInt() % maxRequire) + 1

                    1.upto(numRequire, {

                        def maxAttempt = maxRequire * 2
                        def currentAttempt = 0

                        while (currentAttempt < maxAttempt) {

                            currentAttempt++

                            if (remainingsInvs.isEmpty())
                                break

                            def currentRequireIndex = Math.abs(new Random().nextInt() % totalInv)
                            def currentRequire = totalInvs.get(currentRequireIndex)

                            List stack = currentRequire.cache

                            // Can't refer to myself
                            if (currentRequire.name == invBoostrap.name)
                                continue

                            // Already depends on me
                            if (invBoostrap.name in stack)
                                continue

                            // Otherwise link them
                            invBoostrap.requires << currentRequire.name
                            invBoostrap.cache.addAll(stack)
                            invBoostrap.cache << currentRequire.name

                            require inv.Randomized(currentRequire.name)

                            currentRequire.done = true

                            break
                        }
                    })
                }

                broadcast inv.Randomized(invBoostrap.name)

                println "Completed inv #${index++} with ${invBoostrap.requires.size()} requires"
            }


        }

        def report = executor.execute()

        assert report.isOk()
        assert report.digested.size() == totalInv
    }

    static class InvBootstrap {
        String name
        List<String> requires = []
        List<String> cache = []
        boolean done = false
    }
}
