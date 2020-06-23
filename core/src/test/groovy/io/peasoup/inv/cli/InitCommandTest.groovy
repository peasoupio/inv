package io.peasoup.inv.cli

import io.peasoup.inv.TempHome
import org.junit.Test
import org.junit.runner.RunWith

import static org.junit.jupiter.api.Assertions.assertThrows

@RunWith(TempHome.class)
class InitCommandTest {

    @Test
    void ok() {

        def file = new File("../examples/init/init.groovy")
        assert file.exists()

        file.deleteDir()

        def report = new InitCommand(initFileLocation: file.absolutePath).processSCM()

        assert report
        assert report.name.toLowerCase() == "main"
        assert report.isOk()
    }

    @Test
    void ok_url() {
        def report = new InitCommand(initFileLocation: 'https://raw.githubusercontent.com/peasoupio/inv/master/examples/init/init.groovy').processSCM()

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
