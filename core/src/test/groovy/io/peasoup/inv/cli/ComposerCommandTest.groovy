package io.peasoup.inv.cli

import org.junit.Test

class ComposerCommandTest {

    @Test
    void rolling() {
        assert !new ComposerCommand().rolling()
    }
}
