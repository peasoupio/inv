package io.peasoup.inv.randomizer

import groovy.transform.CompileStatic
import io.peasoup.inv.run.*
import org.apache.commons.lang.RandomStringUtils
import org.junit.Before
import org.junit.Test

@CompileStatic
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
        Logger.enableSystem()

        // Rando
        Random random = new Random()

        // main parameters
        //def totalInv = 2500000
        def totalInv = 1250
        def maxRequire = 12

        // state vars
        Map<String, InvBootstrap> invs = new HashMap<String, InvBootstrap>(totalInv)

        // Generate invs
        for(def i=0;i<totalInv;i++) {
            def invName = RandomStringUtils.random(12, true, true)

            // Don't care about duplicates
            if (invs.containsKey(invName))
                return

            invs.put(invName, new InvBootstrap(invName, maxRequire))
        }

        Queue<InvBootstrap> remainingsInvs = new LinkedList<>(invs.values().collect())
        List<InvBootstrap> totalInvs = new ArrayList<>(invs.values() as List<InvBootstrap>)

        def index = 0
        while(!remainingsInvs.isEmpty()) {

            InvBootstrap invBoostrap = remainingsInvs.poll()

            // create inv right now
            this.invHandler.call {

                InvDescriptor myself = delegate as InvDescriptor
                myself.name(invBoostrap.name)

                if (!invBoostrap.done) {
                    // Get number of requirements
                    Integer numRequire = Math.abs(random.nextInt() % maxRequire) + 1

                    for(def i=0;i<numRequire;i++) {

                        Integer maxAttempt = maxRequire / 2 as Integer
                        Integer currentAttempt = 0

                        while (currentAttempt < maxAttempt) {

                            currentAttempt++

                            if (remainingsInvs.isEmpty())
                                break

                            Integer currentRequireIndex = Math.abs(random.nextInt() % totalInv)
                            InvBootstrap currentRequire = totalInvs.get(currentRequireIndex)


                            // Can't refer to myself
                            if (currentRequire.name == invBoostrap.name)
                                continue

                            boolean circular = false
                            Queue<Set<String>> stack = new LinkedList<>()
                            stack.add(currentRequire.requires)

                            while(!stack.isEmpty()) {
                                def currentStack = stack.poll()

                                if (currentStack.contains(invBoostrap.name)) {
                                    circular = true
                                    break
                                }

                                for(String fromStack: currentStack)
                                    stack.add(invs.get(fromStack).requires)
                            }

                            // Already depends on me
                            if (circular)
                                continue

                            // Otherwise link them
                            invBoostrap.requires.add(currentRequire.name)

                            myself.require((myself.inv.propertyMissing("Randomized") as StatementDescriptor).call(currentRequire.name))

                            currentRequire.done = true

                            break
                        }
                    }
                }

                myself.broadcast((myself.inv.propertyMissing("Randomized") as StatementDescriptor).call(invBoostrap.name))

                ++index

                if (index % 100 == 0)
                    println "Completed inv #${index} with ${invBoostrap.requires.size()} requires"
            }
        }

        def report = executor.execute()

        assert report.isOk()
        assert report.digested.size() == totalInv
    }

    static class InvBootstrap {
        final String name

        Set<String> requires
        boolean done = false

        InvBootstrap(String name, Integer capacity) {
            this.name = name
            this.requires = new HashSet<>(capacity)
        }
    }
}
