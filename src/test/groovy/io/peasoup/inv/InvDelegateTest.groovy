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
        })
    }

    @Test
    void broadcast() {
        def nvd = new NetworkValuableDescriptor(name: "name")

        assert nvd == myself.broadcast(nvd)
        assert nvd.usingDigestor != null
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
        assert nvd.usingDigestor != null
    }

    @Test
    void require_not_ok() {
        assertThrows(PowerAssertionError.class, {
            myself.require()
        })
    }
}