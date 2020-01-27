package io.peasoup.inv.cli

import org.junit.Test

import static org.junit.jupiter.api.Assertions.assertThrows

class DeltaCommandTest {

    @Test
    void not_ok() {
        assertThrows(AssertionError.class, {
            assert new DeltaCommand().call()
        })

        assertThrows(AssertionError.class, {
            assert new DeltaCommand(base: "base").call()
        })

        assertThrows(AssertionError.class, {
            assert new DeltaCommand(other: "other").call()
        })
    }

    @Test
    void rolling() {
        assert !new DeltaCommand().rolling()
    }
}
