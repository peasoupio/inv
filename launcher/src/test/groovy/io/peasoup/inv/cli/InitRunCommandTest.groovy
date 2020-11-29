package io.peasoup.inv.cli

import io.peasoup.inv.TempHome
import org.junit.After
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import spark.Spark

import static org.junit.Assert.*

@RunWith(TempHome.class)
class InitRunCommandTest {

    @After
    void finish() {
        // Spark stop
        Spark.stop()
        Spark.awaitStop()
    }

    @Test
    void ok() {
        def file = new File("../examples/init/init.groovy")

        assertTrue file.exists()
        assertEquals 0, new InitRunCommand().call(
                "<repoFile>": file.absolutePath,
                "<port>": "8082"
        )
    }

    @Test
    @Ignore("Wait till init.groovy is merged into master")
    void ok_url() {
        assertEquals 0,  new InitRunCommand().call(
                "<repoFile>": 'https://raw.githubusercontent.com/peasoupio/inv/master/examples/init/init.groovy',
                "<port>": "8083"
        )
    }

    @Test
    void not_ok() {
        assertThrows(IllegalArgumentException.class, {
            new InitRunCommand().call(null)
        })

        assertEquals 1, new InitRunCommand().call()
        assertEquals 2, new InitRunCommand().call("<repoFile>": "./does-not-exists")
    }

    @Test
    void rolling() {
        assertFalse new InitRunCommand().rolling()
    }
}