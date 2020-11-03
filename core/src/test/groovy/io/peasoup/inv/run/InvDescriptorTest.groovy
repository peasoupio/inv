package io.peasoup.inv.run

import org.junit.Before
import org.junit.Test

import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertNull
import static org.junit.jupiter.api.Assertions.*

class InvDescriptorTest {

    InvDescriptor.Properties properties
    InvDescriptor myself

    @Before
    void setup() {
        properties = new InvDescriptor.Properties()
        myself = new InvDescriptor(properties, null)
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
            myself.broadcast()
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
        assertTrue delegate.defaults
        assertFalse delegate.unbloatable
        assertNull delegate.into
    }

    @Test
    void require_without_id() {
        def nvd = new StatementDescriptor("name")

        myself.require(nvd)

        def delegate = properties.statements.first() as RequireStatement

        assertEquals nvd.name, delegate.name
        assertEquals InvDescriptor.DEFAULT_ID, delegate.id
    }

    @Test
    void require_set_defaults() {
        def statementDescriptor = new StatementDescriptor("name")
        def requireDescriptor = myself.require(statementDescriptor)

        def delegate = properties.statements.first() as RequireStatement

        assertEquals statementDescriptor.name, delegate.name
        assertTrue delegate.defaults

        requireDescriptor.using {
            defaults false
        }

        assertEquals statementDescriptor.name, delegate.name
        assertFalse delegate.defaults

        requireDescriptor.using {
            defaults true
        }

        assertEquals statementDescriptor.name, delegate.name
        assertTrue delegate.defaults
    }

    @Test
    void require_set_unbloatable() {
        def statementDescriptor = new StatementDescriptor("name")

        def requireDescriptor = myself.require(statementDescriptor)
        assertNotNull requireDescriptor

        def delegate = properties.statements.first() as RequireStatement
        assertEquals statementDescriptor.name, delegate.name
        assertFalse delegate.unbloatable

        requireDescriptor.using {
            unbloatable true
        }

        assertEquals statementDescriptor.name, delegate.name
        assertTrue delegate.unbloatable

        requireDescriptor.using {
            unbloatable false
        }

        assertEquals statementDescriptor.name, delegate.name
        assertFalse delegate.unbloatable
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