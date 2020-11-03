package io.peasoup.inv.run

import org.junit.Before
import org.junit.Test

import static org.junit.Assert.assertEquals

class BroadcastUsingDescriptorTest {

    BroadcastUsingDescriptor myself

    @Before
    void setup() {
        myself = new BroadcastUsingDescriptor()
    }

    @Test
    void id() {
        def id1 = "my-id"
        myself.id(id1)

        assertEquals id1, myself.id

        def id2 = [prop: "my-id"]
        myself.id(id2)

        assertEquals id2, myself.id

        def id3 = null
        myself.id(id3)

        assertEquals id3, myself.id
    }

    @Test
    void ready() {
        def ready = { }

        myself.ready(ready)

        assertEquals ready, myself.ready
    }
}