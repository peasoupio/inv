package io.peasoup.inv.cli

import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue

class RepoRunCommandTest {

    @Test
    void not_ok() {
        assertEquals 1, new RepoRunCommand().call()
    }

    @Test
    void rolling() {
        assertTrue new RepoRunCommand().rolling()
    }
}
