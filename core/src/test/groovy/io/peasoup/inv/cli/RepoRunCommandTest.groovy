package io.peasoup.inv.cli

import org.junit.Test

class RepoRunCommandTest {

    @Test
    void not_ok() {
        assert new RepoRunCommand().call() == -1
    }

    @Test
    void rolling() {
        assert new RepoRunCommand().rolling()
    }
}
