package io.peasoup.inv.cli

import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertFalse

class GraphCommandTest {

    @Test
    void not_ok() {
        assertEquals 1, new GraphCommand().call()
    }

    @Test
    void rolling() {
        assertFalse new GraphCommand().rolling()
    }
}
