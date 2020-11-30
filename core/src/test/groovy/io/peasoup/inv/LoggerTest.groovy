package io.peasoup.inv

import org.junit.Test

import static org.junit.Assert.assertTrue

class LoggerTest {

    @Test
    void send_not_ok() {
        List<String> logs = []

        Logger.capture = logs

        Logger.system("")
        assertTrue logs.isEmpty()

        Logger.system("\n")
        assertTrue logs.isEmpty()

        Logger.capture = null
    }
}
