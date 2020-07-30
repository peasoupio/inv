package io.peasoup.inv.cli

import org.junit.Test

import static org.junit.jupiter.api.Assertions.assertThrows

class ScmCommandTest {

    @Test
    void not_ok() {
        assertThrows(AssertionError.class, {
            assert new ScmCommand().call()
        })
    }

    @Test
    void rolling() {
        assert new ScmCommand().rolling()
    }
}
