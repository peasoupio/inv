package io.peasoup.inv.cli

import io.peasoup.inv.TempHome
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith

import static org.junit.jupiter.api.Assertions.assertThrows

@RunWith(TempHome.class)
class InitCommandTest {

    @Test
    @Ignore
    void ok() {

        def file = new File("../examples/init/init.groovy")
        assert file.exists()

        file.deleteDir()

        def report = new InitCommand(initRepoFileLocation: file.absolutePath).processREPO()

        assert report
        assert report.name.toLowerCase() == "main"
        assert report.isOk()
    }

    @Test
    @Ignore
    void ok_url() {
        def report = new InitCommand(initRepoFileLocation: 'https://raw.githubusercontent.com/peasoupio/inv/master/examples/init/init.groovy').processREPO()

        assert report
        assert report.name.toLowerCase() == "main"
        assert report.isOk()
    }

    @Test
    void not_ok() {
        assertThrows(AssertionError.class, {
            assert new InitCommand().call()
        })
    }

    @Test
    void rolling() {
        assert !new InitCommand().rolling()
    }
}
