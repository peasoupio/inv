package io.peasoup.inv.cli

import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertThrows

class GraphCommandTest {

    @Test
    void not_ok() {
        assertThrows(IllegalArgumentException.class, {
            new GraphCommand().call(null)
        })

        assertEquals 1, new GraphCommand().call([:])
        assertEquals 2, new GraphCommand().call(["<base>": "base"])
    }

    @Test
    void rolling() {
        assertFalse new GraphCommand().rolling()
    }
}
