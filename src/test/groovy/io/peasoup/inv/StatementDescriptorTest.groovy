package io.peasoup.inv


import org.junit.Before
import org.junit.Test

import static org.junit.jupiter.api.Assertions.assertThrows

class StatementDescriptorTest {

    StatementDescriptor myself

    @Before
    void setup() {
        myself = new StatementDescriptor("name")
    }

    @Test
    void ctor_not_ok() {
        assertThrows(AssertionError.class, {
            new StatementDescriptor(null)
        })

        assertThrows(AssertionError.class, {
            new StatementDescriptor("%\$;")
        })
    }

    @Test
    void call_ok() {

        assert myself == myself.call()
        assert myself.id == null


        def id1 = "id1"

        assert myself == myself.call(id1)
        assert myself.id == id1

        def id2 = [prop: "id2"]

        assert myself == myself.call(id2)
        assert myself.id == id2
    }

    @Test()
    void call_not_ok() {

        assertThrows(AssertionError.class, {
            myself.call(null)
        })

        assertThrows(AssertionError.class, {
            myself.call((Object)null)
        })

        assertThrows(AssertionError.class, {
            myself.call((Map)null)
        })
    }

    @Test
    void using_ok() {
        myself.usingDigestor = { }

        myself.using { }
    }

    @Test
    void using__not_ok() {

        // requires usingDigestor
        assertThrows(AssertionError.class, {
            myself.using {}
        })

        assertThrows(AssertionError.class, {
            myself.usingDigestor = {}
            myself.using()
        })


    }
}