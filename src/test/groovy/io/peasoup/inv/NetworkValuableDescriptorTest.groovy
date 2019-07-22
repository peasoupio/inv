package io.peasoup.inv

import org.codehaus.groovy.runtime.metaclass.MethodSelectionException
import org.codehaus.groovy.runtime.powerassert.PowerAssertionError
import org.junit.Before
import org.junit.Test

import static org.junit.jupiter.api.Assertions.assertThrows

class NetworkValuableDescriptorTest {

    NetworkValuableDescriptor myself

    @Before
    void setup() {
        myself = new NetworkValuableDescriptor()
    }

    @Test
    void call_ok() {
        def id1 = "id1"

        assert myself == myself.call(id1)
        assert myself.id == id1

        def id2 = [prop: "id2"]

        assert myself == myself.call(id2)
        assert myself.id == id2
    }

    @Test()
    void call_not_ok() {
        assertThrows(MethodSelectionException.class, {
            myself.call()
        })

        assertThrows(PowerAssertionError.class, {
            myself.call(null)
        })

        assertThrows(PowerAssertionError.class, {
            myself.call((Object)null)
        })

        assertThrows(PowerAssertionError.class, {
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
        assertThrows(PowerAssertionError.class, {
            myself.using {}
        })

        assertThrows(PowerAssertionError.class, {
            myself.usingDigestor = {}
            myself.using()
        })


    }
}