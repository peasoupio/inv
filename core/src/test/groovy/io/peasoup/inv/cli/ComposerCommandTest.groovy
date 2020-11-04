package io.peasoup.inv.cli

import org.junit.Test

import static org.junit.Assert.assertTrue

class ComposerCommandTest {

    @Test
    void rolling() {
        assertTrue new ComposerCommand().rolling()
    }
}
