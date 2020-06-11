package io.peasoup.inv.run

import org.junit.Before
import org.junit.Test

import static org.junit.jupiter.api.Assertions.assertThrows

class InvDescriptorTest {

    InvDescriptor.Properties properties
    InvDescriptor myself

    @Before
    void setup() {
        properties = new InvDescriptor.Properties()
        myself = new InvDescriptor(properties)
    }

    @Test
    void name() {
        def name = "name"
        myself.name(name)

        assert myself.name == name
    }

    @Test
    void name_not_ok() {
        assertThrows(IllegalArgumentException.class, {
            String name = null
            myself.name(name)
        })
    }

    @Test
    void broadcast() {
        def statementDescriptor = new StatementDescriptor("name")

        def broadcastDescriptor = myself.broadcast(statementDescriptor)
        assert broadcastDescriptor
    }

    @Test
    void broadcast_not_ok() {
        assertThrows(IllegalArgumentException.class, {
            myself.broadcast()
        })
    }

    @Test
    void require() {
        def nvd = new StatementDescriptor("name")
        nvd("my-id")

        myself.require(nvd)

        def delegate = properties.statements.first() as RequireStatement

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

        def delegate = properties.statements.first() as RequireStatement

        assert delegate.name == nvd.name
        assert delegate.id == InvDescriptor.DEFAULT_ID
    }

    @Test
    void require_set_defaults() {
        def statementDescriptor = new StatementDescriptor("name")
        def requireDescriptor = myself.require(statementDescriptor)

        def delegate = properties.statements.first() as RequireStatement

        assert delegate.name == statementDescriptor.name
        assert delegate.defaults

        requireDescriptor.using {
            defaults false
        }

        assert delegate.name == statementDescriptor.name
        assert !delegate.defaults

        requireDescriptor.using {
            defaults true
        }

        assert delegate.name == statementDescriptor.name
        assert delegate.defaults
    }

    @Test
    void require_set_unbloatable() {
        def statementDescriptor = new StatementDescriptor("name")

        def requireDescriptor = myself.require(statementDescriptor)
        assert requireDescriptor

        def delegate = properties.statements.first() as RequireStatement
        assert delegate.name == statementDescriptor.name
        assert !delegate.unbloatable

        requireDescriptor.using {
            unbloatable false
        }

        assert delegate.name == statementDescriptor.name
        assert !delegate.unbloatable

        requireDescriptor.using {
            unbloatable true
        }

        assert delegate.name == statementDescriptor.name
        assert delegate.unbloatable
    }

    @Test
    void require_not_ok() {
        assertThrows(IllegalArgumentException.class, {
            myself.require()
        })
    }

    @Test
    void pop_ok() {
        boolean value = true

        myself.pop(value)

        assert properties.pop == value
    }

    @Test
    void tail_ok() {
        boolean value = true

        myself.tail(value)

        assert properties.tail == value
    }

    @Test
    void pop_tail_true() {
        assertThrows(IllegalArgumentException.class, {
            myself.pop(true)
            myself.tail(true)
        })
    }

    @Test
    void tail_pop_true() {
        assertThrows(IllegalArgumentException.class, {
            myself.tail(true)
            myself.pop(true)
        })
    }

    @Test
    void tags_ok() {
        Map<String, String> tags = [my: 'tag']

        myself.tags(tags)

        assert myself.getTags() == tags
    }

    @Test
    void tags_fail() {
        assertThrows(IllegalArgumentException.class, {
            myself.tags(null)
        })

        assertThrows(IllegalArgumentException.class, {
            myself.tags([:])
        })
    }

    @Test
    void myself_ok() {
        assert myself.myself == myself // funny isn't it ?
    }
}