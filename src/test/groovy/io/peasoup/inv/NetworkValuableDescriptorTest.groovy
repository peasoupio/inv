package io.peasoup.inv

import org.codehaus.groovy.runtime.metaclass.MethodSelectionException
import org.codehaus.groovy.runtime.powerassert.PowerAssertionError
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import static org.junit.jupiter.api.Assertions.assertThrows

class NetworkValuableDescriptorTest {

    NetworkValuableDescriptor myself

    @BeforeEach
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
    }

    @Test
    void using_ok() {
        myself.digestor = { }

        myself.using { }
    }

    @Test
    void using__not_ok() {

        // requires digestor
        assertThrows(PowerAssertionError.class, {
            myself.using {}
        })

        assertThrows(PowerAssertionError.class, {
            myself.digestor = {}
            myself.using()
        })


    }
}