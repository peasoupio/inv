package io.peasoup.inv.cli

import org.junit.Test

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
}
