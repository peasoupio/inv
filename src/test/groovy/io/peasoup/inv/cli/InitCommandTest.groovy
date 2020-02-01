package io.peasoup.inv.cli

import org.junit.Test

import static org.junit.jupiter.api.Assertions.assertThrows

class InitCommandTest {

    @Test
    void ok() {

        def file = new File("./src/main/example/init/init.groovy")
        assert file.exists()

        file.deleteDir()

        def report = new InitCommand().processSCM(file)

        assert report
        assert report.name.toLowerCase() == "main"
        assert report.isOk
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
