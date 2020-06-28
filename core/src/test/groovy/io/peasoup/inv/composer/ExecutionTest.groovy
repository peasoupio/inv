package io.peasoup.inv.composer

import groovy.mock.interceptor.MockFor
import io.peasoup.inv.TempHome
import org.eclipse.jetty.websocket.api.Session
import org.junit.Test
import org.junit.runner.RunWith

import static org.junit.jupiter.api.Assertions.assertThrows

@RunWith(TempHome.class)
class ExecutionTest {

    String base = "./target/test-classes"

    @Test
    void not_ok() {

        assertThrows(AssertionError.class, {
            new Execution(null, null)
        })
    }

    @Test
    void messageStreamer() {
        assert Execution.MessageStreamer.sessions != null
        assert Execution.MessageStreamer.sessions.isEmpty()

        Session sessionMock = new MockFor(Session).proxy as Session

        def messageStreamer = new Execution.MessageStreamer()

        messageStreamer.connected(sessionMock)
        assert Execution.MessageStreamer.sessions.contains(sessionMock)

        messageStreamer.closed(sessionMock, 0, null)
        assert !Execution.MessageStreamer.sessions.contains(sessionMock)
    }
}
