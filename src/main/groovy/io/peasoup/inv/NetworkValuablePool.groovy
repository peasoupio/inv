package main.groovy.io.peasoup.inv

import java.util.concurrent.ConcurrentHashMap

class NetworkValuablePool {

    boolean stillRunning = true

    Map<String, Map<Object, Object>> availableValuables = [:]

    List<Inv> remainingsInv = [].asSynchronized()
    List<Inv> totalInv = [].asSynchronized()

    List<Inv> digest() {

        List<Inv> invsDone = []
        Collection<NetworkValuable> toBroadcast = []

        // Use fori-loop for speed
        for (int i = 0; i < remainingsInv.size() ; i++) {
            def inv = remainingsInv.getAt(i)

            Collection<NetworkValuable> remainingValuables = inv.remainingValuables.collect()


            // Use fori-loop for speed
            for (int j = 0; j < remainingValuables.size(); j++) {
                def networkValuable = remainingValuables[j]

                def result = networkValuable.match.manage(this, networkValuable)

                if (!result) {
                  if (inv.sync)
                      break
                    else
                      continue
                }

                if (networkValuable.match == NetworkValuable.BROADCAST)
                    toBroadcast << networkValuable

                inv.remainingValuables.remove(networkValuable)
            }

            if (inv.remainingValuables.isEmpty())
                invsDone << inv
        }

        for (int i = 0; i < toBroadcast.size(); i++) {
            NetworkValuable networkValuable = toBroadcast[i]
            def channel = availableValuables[networkValuable.name]

            if (channel.containsKey(networkValuable.id)) {
                Logger.warn "${networkValuable.id} already broadcasted. Skipped"
                continue
            }

            channel << [(networkValuable.id): [
                owner: networkValuable.inv.name,
                response: networkValuable.response]
            ]
        }


        remainingsInv.removeAll(invsDone)

        stillRunning = !remainingsInv.isEmpty()

        return invsDone
    }

    boolean checkAvailability(String name) {
        if (availableValuables.containsKey(name))
            return true

        availableValuables.put(name, new ConcurrentHashMap<Object, Object>())
    }


    def isEmpty() {
        return remainingsInv.isEmpty()
    }
}
