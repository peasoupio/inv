package io.peasoup.inv

import org.codehaus.groovy.runtime.powerassert.PowerAssertionError
import org.junit.Before
import org.junit.Test

import static org.junit.jupiter.api.Assertions.assertThrows

class InvDelegateTest {

    InvDelegate myself

    @Before
    void setup() {
        myself = new InvDelegate()
    }

    @Test
    void name() {
        def name = "name"
        myself.name(name)

        assert myself.name == name
    }

    @Test
    void name_not_ok() {
        assertThrows(PowerAssertionError.class, {
            String name = null
            myself.name(name)

            assert myself.name == name
        })
    }

    @Test
    void broadcast() {
        def nvd = new NetworkValuableDescriptor(name: "name")

        assert nvd == myself.broadcast(nvd)
        assert nvd.digestor != null

        def nvd2 = new NetworkValuableDescriptor(name: "name", id: "id")

        assert nvd2 == myself.broadcast(nvd2)
        assert nvd2.digestor == null
    }

    @Test
    void broadcast_not_ok() {
        assertThrows(PowerAssertionError.class, {
            myself.broadcast()
        })
    }

    @Test
    void require() {
        def nvd = new NetworkValuableDescriptor(name: "name")

        assert nvd == myself.require(nvd)
        assert nvd.digestor != null

        def nvd2 = new NetworkValuableDescriptor(name: "name", id: "id")

        assert nvd2 == myself.require(nvd2)
        assert nvd2.digestor == null
    }

    @Test
    void require_not_ok() {
        assertThrows(PowerAssertionError.class, {
            myself.require()
        })
    }
}