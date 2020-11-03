package io.peasoup.inv.cli


import org.junit.Test

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertTrue

class RunCommandTest {

    @Test
    void not_ok() {
        assertEquals 1, new RunCommand().call()
    }

    @Test
    void rolling() {
        assertTrue new RunCommand().rolling()
    }
}
