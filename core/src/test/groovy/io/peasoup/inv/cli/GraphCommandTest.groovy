package io.peasoup.inv.cli

import org.junit.Test

import static org.junit.jupiter.api.Assertions.assertThrows

class GraphCommandTest {

    @Test
    void not_ok() {
        assertThrows(AssertionError.class, {
            assert new GraphCommand().call()
        })
    }

    @Test
    void rolling() {
        assert !new GraphCommand().rolling()
    }
}
