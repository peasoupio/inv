package io.peasoup.inv.randomizer

import io.peasoup.inv.InvExecutor
import io.peasoup.inv.InvHandler
import org.apache.commons.lang.RandomStringUtils
import org.junit.Before
import org.junit.Test

class RandomizeBatch1 {

    InvExecutor executor
    InvHandler inv

    @Before
    void setup() {
        executor = new InvExecutor()
        inv = new InvHandler(executor)
    }

    @Test
    void randomize_batch_1() {

        // main parameters
        def totalInv = 100
        def maxRequire = 10
        // state vars
        Map invs = [:]

        // Generate invs
        1.upto(totalInv, {
            def invName = RandomStringUtils.random(12, true, true)

            // Don't care about duplicates
            if (invName in invs)
                return

            invs[invName] = [
                name: invName,
                requires: [],
                cache: []
            ]
        })

        // Randomize requirements and broadcasts
        def remainings = invs.values().collect()
        def index = 0
        while(!remainings.isEmpty()) {

            def inv = remainings.pop()

            // create inv right now
            this.inv.call {
                def myInvInstance = delegate

                name inv.name


                if (!inv.done) {
                    // Get number of requirements
                    def numRequire = Math.abs(new Random().nextInt() % maxRequire) + 1

                    1.upto(numRequire, {

                        def limit = 10
                        def current = 0

                        while (current < limit) {

                            current++

                            if (remainings.isEmpty())
                                break

                            def currentRequireIndex = Math.abs(new Random().nextInt() % totalInv)
                            def currentRequire = invs.values()[currentRequireIndex]

                            List stack = currentRequire.cache

                            // Can't use that dependency branch
                            if (inv.name in stack)
                                continue

                            // Otherwise link them
                            inv.requires << currentRequire.name
                            inv.cache += stack
                            inv.cache << currentRequire.name

                            //inv.cache = inv.cache.unique()

                            myInvInstance.require(this.inv.Randomized(currentRequire.name))

                            currentRequire.done = true

                            break
                        }
                    })
                }

                myInvInstance.broadcast(this.inv.Randomized(inv.name))

                println "Completed inv #${index++} with ${inv.requires.size()} requires"
            }


        }

        executor.execute()
    }
}
