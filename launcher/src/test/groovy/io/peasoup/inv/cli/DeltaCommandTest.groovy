package io.peasoup.inv.cli

import org.junit.Test

import static junit.framework.TestCase.assertFalse
import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertThrows

class DeltaCommandTest {

    @Test
    void not_ok() {
        assertThrows(IllegalArgumentException.class, {
            new DeltaCommand().call(null)
        })

        assertEquals 1, new DeltaCommand().call([:])
        assertEquals 2, new DeltaCommand().call(["<base>": "base"])
    }

    @Test
    void rolling() {
        assertFalse new DeltaCommand().rolling()
    }
}
