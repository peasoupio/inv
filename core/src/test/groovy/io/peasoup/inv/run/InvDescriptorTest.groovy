package io.peasoup.inv.run

import org.junit.Before
import org.junit.Test

import static org.junit.Assert.*

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

        assertEquals name, myself.name
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
        assertNotNull broadcastDescriptor
    }

    @Test
    void broadcast_not_ok() {
        assertThrows(IllegalArgumentException.class, {
            myself.broadcast((Closure)null)
        })

        assertThrows(IllegalArgumentException.class, {
            myself.broadcast((StatementDescriptor)null)
        })
    }

    @Test
    void require() {
        def nvd = new StatementDescriptor("name")
        nvd("my-id")

        myself.require(nvd)

        def delegate = properties.statements.first() as RequireStatement

        assertEquals nvd.name, delegate.name
        assertEquals nvd.id, delegate.id
        assertFalse delegate.dynamic
        assertFalse delegate.optional
        assertNull delegate.into
    }

    @Test
    void require_without_id() {
        def nvd = new StatementDescriptor("name")

        myself.require(nvd)

        def delegate = properties.statements.first() as RequireStatement

        assertEquals nvd.name, delegate.name
        assertEquals StatementDescriptor.DEFAULT_ID, delegate.id
    }

    @Test
    void require_set_defaults() {
        def statementDescriptor = new StatementDescriptor("name")
        def requireDescriptor = myself.require(statementDescriptor)

        def delegate = properties.statements.first() as RequireStatement

        assertEquals statementDescriptor.name, delegate.name
        assertFalse delegate.dynamic

        requireDescriptor.using {
            dynamic true
        }

        assertEquals statementDescriptor.name, delegate.name
        assertTrue delegate.dynamic

        requireDescriptor.using {
            dynamic false
        }

        assertEquals statementDescriptor.name, delegate.name
        assertFalse delegate.dynamic
    }

    @Test
    void require_set_optional() {
        def statementDescriptor = new StatementDescriptor("name")

        def requireDescriptor = myself.require(statementDescriptor)
        assertNotNull requireDescriptor

        def delegate = properties.statements.first() as RequireStatement
        assertEquals statementDescriptor.name, delegate.name
        assertFalse delegate.optional

        requireDescriptor.using {
            optional true
        }

        assertEquals statementDescriptor.name, delegate.name
        assertTrue delegate.optional

        requireDescriptor.using {
            optional false
        }

        assertEquals statementDescriptor.name, delegate.name
        assertFalse delegate.optional
    }

    @Test
    void require_not_ok() {
        assertThrows(IllegalArgumentException.class, {
            myself.require((Closure)null)
        })

        assertThrows(IllegalArgumentException.class, {
            myself.require((StatementDescriptor)null)
        })
    }

    @Test
    void pop_ok() {
        boolean value = true

        myself.pop(value)

        assertEquals value, properties.pop
    }

    @Test
    void tail_ok() {
        boolean value = true

        myself.tail(value)

        assertEquals value, properties.tail
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

        assertEquals tags, myself.getTags()
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
        assertEquals myself, myself.myself  // funny isn't it ?
    }
}