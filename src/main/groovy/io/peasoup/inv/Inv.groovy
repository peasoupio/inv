package main.groovy.io.peasoup.inv

import java.util.concurrent.ConcurrentHashMap

class Inv {

    String name
    boolean sync = true

    final List<NetworkValuable> remainingValuables = [].asSynchronized()
    final List<NetworkValuable> totalValuables = [].asSynchronized()

}
