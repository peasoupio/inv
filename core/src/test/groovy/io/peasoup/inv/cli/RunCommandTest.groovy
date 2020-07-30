package io.peasoup.inv.cli

import io.peasoup.inv.run.BroadcastStatement
import org.junit.Test

import static org.junit.jupiter.api.Assertions.assertThrows

class RunCommandTest {

    @Test
    void not_ok() {
        assertThrows(AssertionError.class, {
            assert new RunCommand().call()
        })
    }

    @Test
    void rolling() {
        assert new RunCommand().rolling()
    }
}
