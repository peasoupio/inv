package io.peasoup.inv.cli

import org.junit.Test

import static org.junit.jupiter.api.Assertions.assertFalse

class DeltaCommandTest {

    @Test
    void rolling() {
        assertFalse new DeltaCommand().rolling()
    }
}
