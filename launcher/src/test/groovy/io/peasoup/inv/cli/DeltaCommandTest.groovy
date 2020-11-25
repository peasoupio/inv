package io.peasoup.inv.cli

import org.junit.Test

import static junit.framework.TestCase.assertFalse

class DeltaCommandTest {

    @Test
    void rolling() {
        assertFalse new DeltaCommand().rolling()
    }
}
