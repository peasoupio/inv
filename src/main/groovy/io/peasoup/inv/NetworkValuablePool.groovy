package io.peasoup.inv

import java.util.concurrent.ConcurrentHashMap

class NetworkValuablePool {

    boolean stillRunning = true

    Set<String> names = []

    Map<String, Map<Object, Object>> availableValuables = [:]
    Map<String, Map<Object, Object>> stagingValuables = [:]

    List<Inv> remainingsInv = [].asSynchronized()
    List<Inv> totalInv = [].asSynchronized()

    List<Inv> digest() {

        List<Inv> invsDone = []

        Collection<NetworkValuable> toResolve = [].asSynchronized()

        // Use fori-loop for speed
        for (int i = 0; i < remainingsInv.size() ; i++) {
            def inv = remainingsInv[i]

            Collection<NetworkValuable> remainingValuables = inv.remainingValuables.collect()

            // Use fori-loop for speed
            for (int j = 0; j < remainingValuables.size(); j++) {
                def networkValuable = remainingValuables[j]

                def result = networkValuable.match.manage(this, networkValuable)

                if (!result) {
                  if (inv.sync) {
                      break
                  } else {
                      continue
                  }
                }


                if (networkValuable.match == NetworkValuable.REQUIRE)
                    toResolve << networkValuable

                inv.remainingValuables.remove(networkValuable)
            }

            if (inv.remainingValuables.isEmpty())
                invsDone << inv
        }

        def stagingSet = stagingValuables.entrySet()
        for (int i = 0; i < stagingValuables.size(); i++) {
            def networkValuables = stagingSet[i]
            availableValuables[networkValuables.key].putAll(networkValuables.value)
        }


        for (int i = 0; i < toResolve.size(); i++) {
            NetworkValuable networkValuable = toResolve[i]

            if (!networkValuable.resolved)
                continue

            def broadcast = availableValuables[networkValuable.name][networkValuable.id]
            networkValuable.resolved.delegate = broadcast
            networkValuable.resolved()
        }

        remainingsInv.removeAll(invsDone)

        stillRunning = !invsDone.isEmpty()

        return invsDone
    }

    void checkAvailability(String name) {
        if (names.contains(name))
            return

        names << name
        availableValuables.put(name, new ConcurrentHashMap<Object, Object>())
        stagingValuables.put(name, new ConcurrentHashMap<Object, Object>())
    }


    boolean isEmpty() {
        return remainingsInv.isEmpty()
    }
}
