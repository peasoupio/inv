package io.peasoup.inv

import org.junit.Before
import org.junit.Test

class BroadcastDelegateTest {

    BroadcastDelegate myself

    @Before
    void setup() {
        myself = new BroadcastDelegate()
    }

    @Test
    void id() {
        def id1 = "my-id"
        myself.id(id1)

        assert myself.id == id1

        def id2 = [prop: "my-id"]
        myself.id(id2)

        assert myself.id == id2

        def id3 = null
        myself.id(id3)

        assert myself.id == id3
    }

    @Test
    void ready() {
        def ready = { }

        myself.ready(ready)

        assert myself.ready == ready
    }
}