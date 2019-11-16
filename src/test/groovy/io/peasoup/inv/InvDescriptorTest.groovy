package io.peasoup.inv

import org.codehaus.groovy.runtime.powerassert.PowerAssertionError
import org.junit.Before
import org.junit.Test

import static org.junit.jupiter.api.Assertions.assertThrows

class InvDescriptorTest {

    InvDescriptor myself

    @Before
    void setup() {
        myself = new InvDescriptor()
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
        def nvd = new NetworkValuableDescriptor("name")

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
        def nvd = new NetworkValuableDescriptor("name")
        nvd("my-id")

        myself.require(nvd)

        def delegate = myself.networkValuables.get(0) as RequireValuable

        assert delegate.name == nvd.name
        assert delegate.id == nvd.id
        assert delegate.defaults
        assert !delegate.unbloatable
        assert delegate.into == null
    }

    @Test
    void require_without_id() {
        def nvd = new NetworkValuableDescriptor("name")

        myself.require(nvd)

        def delegate = myself.networkValuables.get(0) as RequireValuable

        assert delegate.name == nvd.name
        assert delegate.id == NetworkValuable.DEFAULT_ID
    }

    @Test
    void require_set_defaults() {
        def nvd = new NetworkValuableDescriptor("name")

        myself.require(nvd)
        def delegate = myself.networkValuables.get(0) as RequireValuable

        assert delegate.name == nvd.name
        assert delegate.defaults

        nvd.using {
            defaults false
        }

        assert delegate.name == nvd.name
        assert !delegate.defaults

        nvd.using {
            defaults true
        }

        assert delegate.name == nvd.name
        assert delegate.defaults
    }

    @Test
    void require_set_unbloatable() {
        def nvd = new NetworkValuableDescriptor("name")

        myself.require(nvd)
        def delegate = myself.networkValuables.get(0) as RequireValuable

        assert delegate.name == nvd.name
        assert !delegate.unbloatable

        nvd.using {
            unbloatable false
        }

        assert delegate.name == nvd.name
        assert !delegate.unbloatable

        nvd.using {
            unbloatable true
        }

        assert delegate.name == nvd.name
        assert delegate.unbloatable
    }

    @Test
    void require_not_ok() {
        assertThrows(PowerAssertionError.class, {
            myself.require()
        })
    }
}