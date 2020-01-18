package io.peasoup.inv


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
        assertThrows(AssertionError.class, {
            String name = null
            myself.name(name)
        })
    }

    @Test
    void broadcast() {
        def nvd = new StatementDescriptor("name")

        assert nvd == myself.broadcast(nvd)
        assert nvd.usingDigestor != null
    }

    @Test
    void broadcast_not_ok() {
        assertThrows(AssertionError.class, {
            myself.broadcast()
        })
    }

    @Test
    void require() {
        def nvd = new StatementDescriptor("name")
        nvd("my-id")

        myself.require(nvd)

        def delegate = myself.statements.get(0) as RequireStatement

        assert delegate.name == nvd.name
        assert delegate.id == nvd.id
        assert delegate.defaults
        assert !delegate.unbloatable
        assert delegate.into == null
    }

    @Test
    void require_without_id() {
        def nvd = new StatementDescriptor("name")

        myself.require(nvd)

        def delegate = myself.statements.get(0) as RequireStatement

        assert delegate.name == nvd.name
        assert delegate.id == Statement.DEFAULT_ID
    }

    @Test
    void require_set_defaults() {
        def nvd = new StatementDescriptor("name")

        myself.require(nvd)
        def delegate = myself.statements.get(0) as RequireStatement

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
        def nvd = new StatementDescriptor("name")

        myself.require(nvd)
        def delegate = myself.statements.get(0) as RequireStatement

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
        assertThrows(AssertionError.class, {
            myself.require()
        })
    }
}