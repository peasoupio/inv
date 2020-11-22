package io.peasoup.inv.cli

import org.junit.Test

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertFalse

class DeltaCommandTest {

    @Test
    void not_ok() {
        assertEquals 1, new DeltaCommand().call()
        assertEquals 2, new DeltaCommand(base: "base").call()
    }

    @Test
    void rolling() {
        assertFalse new DeltaCommand().rolling()
    }
}
