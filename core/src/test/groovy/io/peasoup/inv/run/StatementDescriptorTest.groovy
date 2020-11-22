package io.peasoup.inv.run

import org.junit.Before
import org.junit.Test

import static org.junit.jupiter.api.Assertions.*

class StatementDescriptorTest {

    StatementDescriptor myself

    @Before
    void setup() {
        myself = new StatementDescriptor("name")
    }

    @Test
    void ctor_not_ok() {
        assertThrows(IllegalArgumentException.class, {
            new StatementDescriptor(null)
        })

        assertThrows(IllegalArgumentException.class, {
            new StatementDescriptor("%\$;")
        })
    }

    @Test
    void call_ok() {

        assertEquals myself, myself.call()
        assertNotNull myself.id
        assertEquals myself.id, StatementDescriptor.DEFAULT_ID


        def id1 = "id1"

        assertEquals myself, myself.call(id1)
        assertEquals id1, myself.id

        def id2 = [prop: "id2"]

        assertEquals myself, myself.call(id2)
        assertEquals id2, myself.id
    }

    @Test()
    void call_not_ok() {

        assertThrows(IllegalArgumentException.class, {
            myself.call(null)
        })

        assertThrows(IllegalArgumentException.class, {
            myself.call((Object)null)
        })

        assertThrows(IllegalArgumentException.class, {
            myself.call((Map)null)
        })
    }
}