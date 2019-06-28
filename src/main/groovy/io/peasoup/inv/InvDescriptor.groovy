package main.groovy.io.peasoup.inv

class InvDescriptor {

    private static Map<String, Closure> names = [:]
    private static NetworkValuablePool pool = new NetworkValuablePool()

    InvDescriptor() {

        // When trying to invoke Name as property (w/o parameters)
        InvDescriptor.metaClass.propertyMissing = { String propertyName ->
            pool.checkAvailability(propertyName)
            return new NetworkValuableDescriptor(name: propertyName)
        }

        InvDescriptor.metaClass.methodMissing = { String methodName, args ->
            pool.checkAvailability(methodName)
            return new NetworkValuableDescriptor(name: methodName)(*args)
        }
    }

    def call(Closure body) {

        assert body


        InvDelegate delegate = new InvDelegate()

        body.delegate = delegate
        body.call()

        Inv inv = new Inv(name: delegate.name)

        // use for-loop to keep order
        for(NetworkValuable networkValuable : delegate.networkValuables) {
            networkValuable.inv = inv

            inv.totalValuables << networkValuable
            inv.remainingValuables << networkValuable
        }

        pool.totalInv << inv
        pool.remainingsInv << inv

    }




    List<Inv> call() {

        int count = 0
        List<Inv> digested = []

        while(true) {

            // has no more work to do
            if (pool.isEmpty())
                break

            // flagged as done, but has more work to do.
            // --- It allows the remaining NV to raise uncompleted events
            if (!pool.stillRunning) {
                pool.digest()
                pool.stillRunning = true
                break
            }

            Logger.info "---- [DIGEST] #${++count} ----"
            digested += pool.digest()
        }

        Logger.info "--- completed ----"

        return digested
    }
}
