package io.peasoup.inv.cli

import io.peasoup.inv.TempHome
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import spark.Spark

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue

@RunWith(TempHome.class)
class ComposerCommandTest {

    @After
    void finish() {
        // Spark stop
        Spark.stop()
        Spark.awaitStop()
    }

    @Test
    void ok() {
        assertEquals 0, new ComposerCommand().call("<port>": "8081")
    }

    @Test
    void rolling() {
        assertTrue new ComposerCommand().rolling()
    }
}
