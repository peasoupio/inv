package io.peasoup.inv.run

import org.junit.Before
import org.junit.Test

import static org.junit.Assert.*

class RequireUsingDescriptorTest {

    RequireUsingDescriptor myself

    @Before
    void setup() {
        myself = new RequireUsingDescriptor()
    }

    @Test
    void id() {
        def id1 = "my-id"
        myself.id(id1)

        assertEquals id1, myself.id

        def id2 = [prop: "my-id"]
        myself.id(id2)

        assertEquals id2, myself.id

        def id3 = null
        myself.id(id3)

        assertEquals id3, myself.id
    }

    @Test
    void resolved() {
        def resolved = { }

        myself.resolved(resolved)

        assertEquals resolved, myself.resolved
    }

    @Test
    void unresolved() {
        def unresolved = { }

        myself.unresolved(unresolved)

        assertEquals unresolved, myself.unresolved
    }

    @Test
    void optional() {
        def optional = true

        assertNull myself.optional

        myself.optional(optional)

        assertTrue myself.optional
    }

    @Test
    void not_ok() {
        assertThrows(IllegalArgumentException.class) {
            myself.markdown("")
        }
    }
}