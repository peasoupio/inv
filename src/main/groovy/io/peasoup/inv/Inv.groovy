package io.peasoup.inv

class Inv {

    String name
    boolean sync = true

    final List<NetworkValuable> remainingValuables = [].asSynchronized()
    final List<NetworkValuable> totalValuables = [].asSynchronized()

}
