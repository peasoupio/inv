package io.peasoup.inv.cli

import io.peasoup.inv.TempHome
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith

import static org.junit.jupiter.api.Assertions.*

@RunWith(TempHome.class)
class InitRunCommandTest {

    @Test
    @Ignore
    void ok() {

        def file = new File("../examples/init/init.groovy")
        assertTrue file.exists()

        file.deleteDir()

        def report = new InitRunCommand(initRepoFileLocation: file.absolutePath).processREPO()

        assertNotNull report
        assertEquals "main", report.name.toLowerCase()
        assertTrue report.isOk()
    }

    @Test
    @Ignore
    void ok_url() {
        def report = new InitRunCommand(initRepoFileLocation: 'https://raw.githubusercontent.com/peasoupio/inv/master/examples/init/init.groovy').processREPO()

        assertNotNull report
        assertEquals "main", report.name.toLowerCase()
        assertTrue report.isOk()
    }

    @Test
    void not_ok() {
        assertEquals 1, new InitRunCommand().call()
    }

    @Test
    void rolling() {
        assertFalse new InitRunCommand().rolling()
    }
}
