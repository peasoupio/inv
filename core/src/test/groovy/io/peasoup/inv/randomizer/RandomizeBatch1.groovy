package io.peasoup.inv.randomizer

import groovy.transform.CompileStatic
import io.peasoup.inv.Logger
import io.peasoup.inv.run.*
import org.apache.commons.lang.RandomStringUtils
import org.junit.Before
import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue

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
    void randomize_2_1_8() {
        launchRandomizer(2,1,8)
    }

    @Test
    void randomize_4_1_8() {
        launchRandomizer(4,1,8)
    }

    @Test
    void randomize_8_1_8() {
        launchRandomizer(8,1,8)
    }

    @Test
    void randomize_16_1_8() {
        launchRandomizer(16,1,8)
    }

    @Test
    void randomize_32_1_8() {
        launchRandomizer(32,1,8)
    }

    @Test
    void randomize_64_1_8() {
        launchRandomizer(64,1,8)
    }

    @Test
    void randomize_128_1_8() {
        launchRandomizer(128,1,8)
    }

    @Test
    void randomize_extreme() {
        launchRandomizer(4028,8,64)
    }

    private void launchRandomizer(int layers_count, int min_per_layer, int max_per_layer) {
        Random random = new Random()

        // Setup the layers
        def layers = []
        1.upto(layers_count, {
            def inv_for_layer = Math.round(random.nextFloat() * (max_per_layer - min_per_layer)) + min_per_layer

            println "Layer #${it} has ${inv_for_layer} INV's"
            def newLayer = []
            1.upto(inv_for_layer, {
                newLayer << RandomStringUtils.random(12, true, true)
            })
            layers << newLayer
        })

        0.upto(layers_count - 1, { Number layerIndex ->

            def previousLayer = layerIndex > 0 ? layers[layerIndex - 1] as List : []
            def currentLayer = layers[layerIndex]

            for(String name : currentLayer) {

                this.invHandler.call {
                    InvDescriptor myself = delegate as InvDescriptor

                    myself.name(name)

                    // Add requirements only if not the first layer
                    if (!previousLayer.isEmpty()) {
                        def required_names = [] as List
                        def amount = Math.round(random.nextFloat() * previousLayer.size())
                        while(amount > 0) {
                            def nextNameIndex = Math.round(random.nextFloat() * previousLayer.size()) - 1
                            def nextName = previousLayer[nextNameIndex]

                            if (required_names.contains(nextName))
                                continue

                            required_names << nextName
                            amount--
                        }

                        for(String require_name : required_names)
                            myself.require((InvNames.Instance.getProperty("Randomized") as StatementDescriptor).call(require_name))
                    }

                    // Add broadcast
                    myself.broadcast((InvNames.Instance.getProperty("Randomized") as StatementDescriptor).call(name))
                }
            }
        })

        def results = executor.execute()
        assertTrue results.report.isOk()
    }

    /*
    @Test
    void randomize_batch_1() {

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

                            myself.require((InvNames.Instance.getProperty("Randomized") as StatementDescriptor).call(currentRequire.name))

                            currentRequire.done = true

                            break
                        }
                    }
                }

                myself.broadcast((InvNames.Instance.getProperty("Randomized") as StatementDescriptor).call(invBoostrap.name))

                ++index

                if (index % 100 == 0)
                    println "Completed inv #${index} with ${invBoostrap.requires.size()} requires"
            }
        }

        def results = executor.execute()

        assertTrue results.report.isOk()
        assertEquals totalInv, results.ingested.size()
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

     */
}
