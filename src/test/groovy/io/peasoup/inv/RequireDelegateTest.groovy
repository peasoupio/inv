package io.peasoup.inv

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RequireDelegateTest {

    RequireDelegate myself

    @BeforeEach
    void setup() {
        myself = new RequireDelegate()
    }

    @Test
    void id() {
        def id1 = "my-id"
        myself.id(id1)

        assert myself.id == id1

        def id2 = [prop: "my-id"]
        myself.id(id2)

        assert myself.id == id2

        def id3 = null
        myself.id(id3)

        assert myself.id == id3
    }

    @Test
    void resolved() {
        def resolved = { }

        myself.resolved(resolved)

        assert myself.resolved == resolved
    }

    @Test
    void unresolved() {
        def unresolved = { }

        myself.unresolved(unresolved)

        assert myself.unresolved == unresolved
    }
}