package io.peasoup.inv.cli

import org.junit.Test

import static org.junit.Assert.assertFalse

class ComposerCommandTest {

    @Test
    void rolling() {
        assertFalse new ComposerCommand().rolling()
    }
}
